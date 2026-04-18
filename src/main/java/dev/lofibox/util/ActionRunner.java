package dev.lofibox.util;

import dev.lofibox.LofiBox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class ActionRunner {

    private final LofiBox plugin;

    public ActionRunner(LofiBox plugin) {
        this.plugin = plugin;
    }

    public void run(Player player, List<String> actions, Map<String, String> placeholders) {
        for (String raw : actions) {
            String action = replace(raw.strip(), player, placeholders);
            if (action.startsWith("[message]")) {
                player.sendMessage(MessageUtil.parse(action.substring(9).strip(), player));
            } else if (action.startsWith("[actionbar]")) {
                player.sendActionBar(MessageUtil.parse(action.substring(11).strip(), player));
            } else if (action.startsWith("[title]")) {
                String content = action.substring(7).strip();
                String[] parts = content.split(";", 2);
                Component title = MessageUtil.parse(parts[0].strip(), player);
                Component sub   = parts.length > 1 ? MessageUtil.parse(parts[1].strip(), player) : Component.empty();
                player.showTitle(Title.title(title, sub));
            } else if (action.startsWith("[sound]")) {
                String soundName = action.substring(7).strip().toUpperCase().replace(".", "_");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName), 1f, 1f);
                } catch (IllegalArgumentException ignored) {}
            } else if (action.startsWith("[command]")) {
                Bukkit.dispatchCommand(player, action.substring(9).strip());
            } else if (action.startsWith("[console]")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.substring(9).strip());
            }
        }
    }

    private String replace(String s, Player player, Map<String, String> extra) {
        s = s.replace("{player}", player.getName());
        s = s.replace("{uuid}", player.getUniqueId().toString());
        if (extra != null) {
            for (Map.Entry<String, String> e : extra.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return MessageUtil.applyPapi(s, player);
    }
}
