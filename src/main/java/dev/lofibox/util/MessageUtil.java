package dev.lofibox.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    public static Component parse(String input) {
        if (input == null) return Component.empty();
        return MM.deserialize(input);
    }

    public static Component parse(String input, Player player) {
        if (input == null) return Component.empty();
        String processed = applyPapi(input, player);
        return MM.deserialize(processed);
    }

    public static String applyPapi(String input, Player player) {
        if (player == null) return input;
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, input);
        } catch (ClassNotFoundException ignored) {
            return input;
        }
    }

    public static String replacePlaceholders(String input, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            input = input.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return input;
    }
}
