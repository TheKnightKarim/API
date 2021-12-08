package com.envyful.api.forge.player;

import com.envyful.api.concurrency.UtilConcurrency;
import com.envyful.api.player.PlayerManager;
import com.envyful.api.player.attribute.PlayerAttribute;
import com.envyful.api.player.attribute.data.PlayerAttributeData;
import com.envyful.api.player.save.SaveManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * Forge implementation of the {@link PlayerManager} interface.
 * Registers the {@link PlayerListener} class as a listener with forge on instantiation so that it can
 * automatically update the cache when player log in and out of the server.
 *
 * Simple instantiation as not enough arguments to warrant a builder class and
 */
public class ForgePlayerManager implements PlayerManager<ForgeEnvyPlayer, EntityPlayerMP> {

    private final Map<UUID, ForgeEnvyPlayer> cachedPlayers = Maps.newHashMap();
    private final List<PlayerAttributeData> attributeData = Lists.newArrayList();

    private SaveManager<EntityPlayerMP> saveManager = null;

    public ForgePlayerManager() {
        MinecraftForge.EVENT_BUS.register(new PlayerListener(this));
    }

    @Override
    public ForgeEnvyPlayer getPlayer(EntityPlayerMP player) {
        return this.getPlayer(player.getUniqueID());
    }

    @Override
    public ForgeEnvyPlayer getPlayer(UUID uuid) {
        return this.cachedPlayers.get(uuid);
    }

    @Override
    public ForgeEnvyPlayer getOnlinePlayer(String username) {
        for (ForgeEnvyPlayer online : this.cachedPlayers.values()) {
            if (online.getParent().getName().equals(username)) {
                return online;
            }
        }

        return null;
    }

    @Override
    public ForgeEnvyPlayer getOnlinePlayerCaseInsensitive(String username) {
        for (ForgeEnvyPlayer online : this.cachedPlayers.values()) {
            if (online.getParent().getName().equalsIgnoreCase(username)) {
                return online;
            }
        }

        return null;
    }

    @Override
    public List<ForgeEnvyPlayer> getOnlinePlayers() {
        return Collections.unmodifiableList(Lists.newArrayList(this.cachedPlayers.values()));
    }

    @Override
    public void registerAttribute(Object manager, Class<? extends PlayerAttribute<?>> attribute) {
        this.attributeData.add(new PlayerAttributeData(manager, attribute));

        if (this.saveManager != null) {
            this.saveManager.registerAttribute(manager, attribute);
        }
    }

    @Override
    public void setSaveManager(SaveManager<EntityPlayerMP> saveManager) {
        this.saveManager = saveManager;
    }

    private final class PlayerListener {

        private final ForgePlayerManager manager;

        private PlayerListener(ForgePlayerManager manager) {
            this.manager = manager;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            ForgeEnvyPlayer player = new ForgeEnvyPlayer((EntityPlayerMP) event.player);
            this.manager.cachedPlayers.put(event.player.getUniqueID(), player);

            UtilConcurrency.runAsync(() -> {
                for (PlayerAttributeData attributeDatum : this.manager.attributeData) {
                    PlayerAttribute<?> instance = attributeDatum.getInstance(player);

                    if (instance == null) {
                        continue;
                    }

                    instance.load();
                    attributeDatum.addToMap(player.attributes, instance);
                }
            });
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
            ForgeEnvyPlayer player = this.manager.cachedPlayers.remove(event.player.getUniqueID());

            if (player == null) {
                return;
            }

            UtilConcurrency.runAsync(() -> {
                for (PlayerAttribute<?> value : player.attributes.values()) {
                    if (value != null) {
                        value.save();
                    }
                }
            });
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            UtilConcurrency.runLater(() -> {
                ForgeEnvyPlayer player = this.manager.cachedPlayers.get(event.player.getUniqueID());

                player.setPlayer((EntityPlayerMP) event.player);
            }, 5L);
        }
    }
}
