package com.envyful.api.forge.chat;

import com.google.common.collect.Lists;
import net.minecraft.util.text.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Static utility methods relating to colour codes
 *
 */
public class UtilChatColour {

    private static final char COLOUR_CHAR = '§';
    private static final String CHARACTERS = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    private static final Pattern COLOUR_PATTERN = Pattern.compile("&(#\\w{6}|[\\da-zA-Z])");

    /**
     *
     * Parses the string to a {@link ITextComponent} with the correctly formatted colour codes and hex codes
     *
     * @param text The unformatted text
     * @return The newly formatted text
     */
    public static ITextComponent colour(String text) {
        Matcher matcher = COLOUR_PATTERN.matcher(text);
        IFormattableTextComponent textComponent = new StringTextComponent("").withStyle(TextFormatting.RESET);
        int lastEnd = 0;
        Color lastColor = null;

        while (matcher.find()) {
            int start = matcher.start();
            String segment = text.substring(lastEnd, start);
            attemptAppend(textComponent, segment, lastColor);

            lastEnd = matcher.end();
            String colourCode = matcher.group(1);
            Optional<Color> colour = parseColour(colourCode);

            if (colour.isPresent()) {
                lastColor = colour.get();
            } else {
                TextFormatting byCode = getByCode(colourCode.toCharArray()[0]);

                if (byCode != null && byCode.isFormat()) {
                    textComponent = textComponent.withStyle(byCode);
                } else if (byCode != null && byCode == TextFormatting.RESET) {
                    textComponent = textComponent.withStyle(TextFormatting.RESET);
                } else {
                    textComponent.append(new StringTextComponent("&" + colourCode));
                }
            }
        }

        String segment = text.substring(lastEnd);
        attemptAppend(textComponent, segment, lastColor);

        return textComponent;
    }

    /**
     *
     * Attempts to append the segment to the {@link TextComponent} with the given (nullable) colour
     *
     * @param textComponent The text component
     * @param segment The segment
     * @param lastColour The colour
     */
    public static void attemptAppend(IFormattableTextComponent textComponent, String segment, Color lastColour) {
        if (segment.isEmpty()) {
            return;
        }

        IFormattableTextComponent appended = new StringTextComponent(segment);

        if (lastColour != null) {
            appended.setStyle(Style.EMPTY.withColor(lastColour));
        }

        textComponent.append(appended);
    }

    /**
     *
     * Attempts to parse the colour code firstly as a hex, then as a legacy
     *
     * @param colourCode The colour code
     * @return The potential equivalent colour
     */
    public static Optional<Color> parseColour(String colourCode) {
        Color colour = Color.parseColor(colourCode);

        if (colour != null) {
            return Optional.of(colour);
        }

        if (colourCode.length() > 1) {
            return Optional.empty();
        }

        TextFormatting byCode = getByCode(colourCode.toCharArray()[0]);

        if (byCode == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(Color.fromLegacyFormat(byCode));
    }

    public static TextFormatting getByCode(char p_211165_0_) {
        char c0 = Character.toString(p_211165_0_).toLowerCase(Locale.ROOT).charAt(0);

        switch (c0) {
            case '0': return TextFormatting.BLACK;
            case '1': return TextFormatting.DARK_BLUE;
            case '2': return TextFormatting.DARK_GREEN;
            case '3': return TextFormatting.DARK_AQUA;
            case '4': return TextFormatting.DARK_RED;
            case '5': return TextFormatting.DARK_PURPLE;
            case '6': return TextFormatting.GOLD;
            case '7': return TextFormatting.GRAY;
            case '8': return TextFormatting.DARK_GRAY;
            case '9': return TextFormatting.BLUE;
            case 'a': return TextFormatting.GREEN;
            case 'b': return TextFormatting.AQUA;
            case 'c': return TextFormatting.RED;
            case 'd': return TextFormatting.LIGHT_PURPLE;
            case 'e': return TextFormatting.YELLOW;
            case 'f': return TextFormatting.WHITE;
            case 'k': return TextFormatting.OBFUSCATED;
            case 'l': return TextFormatting.BOLD;
            case 'm': return TextFormatting.STRIKETHROUGH;
            case 'n': return TextFormatting.UNDERLINE;
            case 'o': return TextFormatting.ITALIC;
            case 'r': return TextFormatting.RESET;
        }

        return null;
    }

    /**
     *
     * Translates the text to coloured text.
     * Reference: https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/ChatColor.java#216
     *
     * @param altColorChar The character
     * @param textToTranslate The text
     * @return The coloured text
     */
    public static String translateColourCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && CHARACTERS.indexOf(b[i + 1]) > -1) {
                b[i] = COLOUR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    /**
     *
     * Translates each line in the list to coloured text.
     * Using {@link UtilChatColour#translateColourCodes(char, String)}
     *
     * @param altColorChar The character
     * @param lines The lines of text
     * @return The coloured text
     */
    public static List<String> translateColourCodes(char altColorChar, List<String> lines) {
        List<String> newLines = Lists.newArrayList();

        for (String line : lines) {
            newLines.add(translateColourCodes('&', line));
        }

        return newLines;
    }
}
