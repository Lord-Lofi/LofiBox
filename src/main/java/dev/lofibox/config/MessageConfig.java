package dev.lofibox.config;

import dev.lofibox.LofiBox;
import dev.lofibox.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
        // Merge any missing keys from the default messages.yml
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(Objects.requireNonNull(
                plugin.getResource("messages.yml")), StandardCharsets.UTF_8));
        cfg.setDefaults(defaults);
        cfg.options().copyDefaults(true);
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Could not save messages.yml after migration: " + e.getMessage());
        }
    }

    public Component get(String key, String... replacements) {
        String raw = cfg.getString(key, "<red>Missing message: " + key);
        String prefix = cfg.getString("prefix", "");
        raw = raw.replace("<prefix>", prefix);
        raw = MessageUtil.replacePlaceholders(raw, replacements);
        return MessageUtil.parse(raw);
    }

    /** Returns the message as plain text (MiniMessage tags stripped), suitable for Discord. */
    public String getRaw(String key, String... replacements) {
        return PlainTextComponentSerializer.plainText().serialize(get(key, replacements));
    }

    public void send(Player player, String key, String... replacements) {
        player.sendMessage(get(key, replacements));
    }

    public void sendRewardWon(Player player, String boxName, String rewardName) {
        send(player, "reward-won", "box", boxName, "reward", rewardName);
    }
}
