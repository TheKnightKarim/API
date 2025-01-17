package com.envyful.api.forge.config;

import com.envyful.api.config.type.ConfigItem;
import com.envyful.api.config.type.PermissibleConfigItem;
import com.envyful.api.config.type.PositionableConfigItem;
import com.envyful.api.forge.chat.UtilChatColour;
import com.envyful.api.forge.items.ItemBuilder;
import com.envyful.api.forge.items.ItemFlag;
import com.envyful.api.forge.player.util.UtilPlayer;
import com.envyful.api.gui.Transformer;
import com.envyful.api.gui.factory.GuiFactory;
import com.envyful.api.gui.item.Displayable;
import com.envyful.api.gui.pane.Pane;
import com.envyful.api.player.EnvyPlayer;
import com.envyful.api.type.Pair;
import com.envyful.api.type.UtilParse;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.registry.Registry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class UtilConfigItem {

    public static void addPermissibleConfigItem(Pane pane, ServerPlayerEntity player, List<Transformer> transformers, PermissibleConfigItem configItem) {
        addPermissibleConfigItem(pane, player, configItem, transformers,null);
    }

    public static void addPermissibleConfigItem(Pane pane, ServerPlayerEntity player, PermissibleConfigItem configItem) {
        addPermissibleConfigItem(pane, player, configItem, null);
    }

    public static void addPermissibleConfigItem(Pane pane, ServerPlayerEntity player, PermissibleConfigItem configItem,
                                                BiConsumer<EnvyPlayer<?>, Displayable.ClickType> clickHandler) {
        addPermissibleConfigItem(pane, player, configItem, Collections.emptyList(), clickHandler);
    }

    public static void addPermissibleConfigItem(Pane pane, ServerPlayerEntity player, PermissibleConfigItem configItem,
                                                List<Transformer> transformers,
                                                BiConsumer<EnvyPlayer<?>, Displayable.ClickType> clickHandler) {
        ItemStack itemStack = fromPermissibleItem(player, configItem, transformers);

        if (itemStack == null) {
            return;
        }

        for (Pair<Integer, Integer> position : configItem.getPositions()) {
            if (clickHandler == null) {
                pane.set(position.getX(), position.getY(), GuiFactory.displayable(itemStack));
            } else {
                pane.set(position.getX(), position.getY(), GuiFactory.displayableBuilder(itemStack)
                        .clickHandler(clickHandler).build());
            }
        }
    }

    public static void addConfigItem(Pane pane, PositionableConfigItem configItem) {
        addConfigItem(pane, configItem, null);
    }

    public static void addConfigItem(Pane pane, List<Transformer> transformers, PositionableConfigItem configItem) {
        addConfigItem(pane, configItem, transformers,null);
    }

    public static void addConfigItem(Pane pane, PositionableConfigItem configItem,
                                     BiConsumer<EnvyPlayer<?>, Displayable.ClickType> clickHandler) {
        addConfigItem(pane, configItem, Collections.emptyList(), clickHandler);
    }

    public static void addConfigItem(Pane pane, PositionableConfigItem configItem, List<Transformer> transformers,
                                     BiConsumer<EnvyPlayer<?>, Displayable.ClickType> clickHandler) {
        if (!configItem.isEnabled()) {
            return;
        }

        for (Pair<Integer, Integer> position : configItem.getPositions()) {
            if (clickHandler == null) {
                pane.set(position.getX(), position.getY(), GuiFactory.displayable(fromConfigItem(
                        configItem,
                        transformers
                )));
            } else {
                pane.set(position.getX(), position.getY(), GuiFactory.displayableBuilder(fromConfigItem(
                        configItem,
                        transformers
                )).clickHandler(clickHandler).build());
            }
        }
    }

    public static ItemStack fromPermissibleItem(ServerPlayerEntity player, PermissibleConfigItem permissibleConfigItem) {
        return fromPermissibleItem(player, permissibleConfigItem, Collections.emptyList());
    }

    public static ItemStack fromPermissibleItem(ServerPlayerEntity player, PermissibleConfigItem permissibleConfigItem, List<Transformer> transformers) {
        if (!permissibleConfigItem.isEnabled()) {
            return null;
        }

        if (permissibleConfigItem.getPermission().isEmpty() || UtilPlayer.hasPermission(player,
                                                                                        permissibleConfigItem.getPermission())) {
            return fromConfigItem(permissibleConfigItem);
        }

        if (permissibleConfigItem.getElseItem() == null || !permissibleConfigItem.getElseItem().isEnabled()) {
            return null;
        }

        return fromConfigItem(permissibleConfigItem.getElseItem());
    }

    public static ItemStack fromConfigItem(ConfigItem configItem) {
        return fromConfigItem(configItem, Collections.emptyList());
    }

    public static ItemStack fromConfigItem(ConfigItem configItem, List<Transformer> transformers) {
        if (!configItem.isEnabled()) {
            return null;
        }

        String name = configItem.getName();

        ItemBuilder itemBuilder = new ItemBuilder()
                .type(fromNameOrId(configItem.getType()))
                .amount(configItem.getAmount(transformers));

        List<String> lore = configItem.getLore();
        List<String> flags = configItem.getFlags();

        if (!transformers.isEmpty()) {
            for (Transformer transformer : transformers) {
                lore = transformer.transformLore(lore);
                name = transformer.transformName(name);
                flags = transformer.transformLore(flags);
            }
        }

        for (String flag : flags) {
            ItemFlag foundFlag = ItemFlag.valueOf(flag.toUpperCase());
            itemBuilder.itemFlag(foundFlag);
        }

        for (String s : lore) {
            itemBuilder.addLore(UtilChatColour.colour(s));
        }

        itemBuilder.name(UtilChatColour.colour(name));

        for (ConfigItem.EnchantData value : configItem.getEnchants().values()) {
            String enchantName = value.getEnchant();
            String level = value.getLevel();

            if (!transformers.isEmpty()) {
                for (Transformer transformer : transformers) {
                    enchantName = transformer.transformName(enchantName);
                    level = transformer.transformName(level);
                }
            }

            Enchantment enchantment = Registry.ENCHANTMENT.getOptional(new ResourceLocation(enchantName.toLowerCase())).orElse(null);
            int parsedLevel = UtilParse.parseInteger(level).orElse(1);

            if (enchantment == null) {
                continue;
            }

            itemBuilder.enchant(enchantment, parsedLevel);
        }

        for (Map.Entry<String, ConfigItem.NBTValue> nbtData : configItem.getNbt().entrySet()) {
            Pair<String, INBT> parsed = parseNBT(nbtData, transformers);

            if (parsed != null) {
                itemBuilder.nbt(parsed.getX(), parsed.getY());
            }
        }

        return itemBuilder.build();
    }

    public static Pair<String, INBT> parseNBT(Map.Entry<String, ConfigItem.NBTValue> nbtEntry, List<Transformer> transformers) {
        if (nbtEntry.getValue().getType().equalsIgnoreCase("nbt")) {
            CompoundNBT compound = new CompoundNBT();

            for (Map.Entry<String, ConfigItem.NBTValue> entry : nbtEntry.getValue().getSubData().entrySet()) {
                Pair<String, INBT> parsed = parseNBT(entry, transformers);

                if (parsed != null) {
                    compound.put(parsed.getX(), parsed.getY());
                }
            }

            return Pair.of(nbtEntry.getKey(), compound);
        }

        if (nbtEntry.getValue().getType().equalsIgnoreCase("list")) {
            ListNBT list = new ListNBT();

            for (Map.Entry<String, ConfigItem.NBTValue> nbtValue : nbtEntry.getValue().getSubData().entrySet()) {
                Pair<String, INBT> parsed = parseNBT(nbtValue, transformers);

                if (parsed != null) {
                    CompoundNBT compound = new CompoundNBT();
                    compound.put(parsed.getX(), parsed.getY());
                    list.add(compound);
                }
            }

            return Pair.of(nbtEntry.getKey(), list);
        }

        return Pair.of(nbtEntry.getKey(), parseBasic(nbtEntry.getValue(), transformers));
    }

    public static INBT parseBasic(ConfigItem.NBTValue value, List<Transformer> transformers) {
        String data = value.getData();

        if (!transformers.isEmpty()) {
            for (Transformer transformer : transformers) {
                data = transformer.transformName(data);
            }
        }

        INBT base;

        switch (value.getType().toLowerCase()) {
            case "int":
            case "integer":
                base = IntNBT.valueOf(Integer.parseInt(data));
                break;
            case "long":
                base = LongNBT.valueOf(Long.parseLong(data));
                break;
            case "byte":
                base = ByteNBT.valueOf(Byte.parseByte(data));
                break;
            case "double":
                base = DoubleNBT.valueOf(Double.parseDouble(data));
                break;
            case "float":
                base = FloatNBT.valueOf(Float.parseFloat(data));
                break;
            case "short":
                base = ShortNBT.valueOf(Short.parseShort(data));
                break;
            default:
            case "string":
                base = StringNBT.valueOf(data);
                break;
        }

        return base;
    }

    public static Item fromNameOrId(String data) {
        try {
            Item item = Registry.ITEM.getOptional(new ResourceLocation(data)).orElse(null);

            if (item != null) {
                return item;
            }

            int integer = UtilParse.parseInteger(data).orElse(-1);

            if (integer == -1) {
                return null;
            }

            return Item.byId(integer);
        } catch (ResourceLocationException e) {
            return null;
        }
    }

}
