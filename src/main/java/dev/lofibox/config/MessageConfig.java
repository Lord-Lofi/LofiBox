package dev.lofibox.config;

import dev.lofibox.LofiBox;
import dev.lofibox.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public final class MessageConfig {

    private final LofiBox plugin;
    private FileConfiguration cfg;

    public MessageConfig(LofiBox plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public Component get(String key, String... replacements) {
        String raw = cfg.getString(key, "<red>Missing message: " + key);
        String prefix = cfg.getString("prefix", "");
        raw = raw.replace("<prefix>", prefix);
        raw = MessageUtil.replacePlaceholders(raw, replacements);
        return MessageUtil.parse(raw);
    }

    public void send(Player player, String key, String... replacements) {
        player.sendMessage(get(key, replacements));
    }

    public void sendRewardWon(Player player, String boxName, String rewardName) {
        send(player, "reward-won", "box", boxName, "reward", rewardName);
    }
}
