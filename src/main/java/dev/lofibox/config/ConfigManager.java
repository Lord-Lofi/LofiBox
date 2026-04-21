package dev.lofibox.config;

import dev.lofibox.LofiBox;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ConfigManager {

    private final LofiBox plugin;

    private String fullInventoryAction;
    private boolean previewShowWeights;
    private String defaultOpenSound;
    private String defaultWinSound;
    private boolean winFirework;
    private boolean headFoundBroadcast;
    private boolean discordHeadAnnounce;
    private String discordChannel;

    public ConfigManager(LofiBox plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) plugin.saveDefaultConfig();
        plugin.reloadConfig();
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(Objects.requireNonNull(
                plugin.getResource("config.yml")), StandardCharsets.UTF_8));
        plugin.getConfig().setDefaults(defaults);
        plugin.getConfig().options().copyDefaults(true);
        try { plugin.getConfig().save(file); } catch (IOException e) {
            plugin.getLogger().warning("Could not save config.yml after migration: " + e.getMessage());
        }
        load();
    }

    private void load() {
        var cfg = plugin.getConfig();
        fullInventoryAction  = cfg.getString("full-inventory-action", "DROP").toUpperCase();
        previewShowWeights   = cfg.getBoolean("preview-show-weights", true);
        defaultOpenSound     = cfg.getString("default-open-sound", "block.chest.open");
        defaultWinSound      = cfg.getString("default-win-sound", "entity.player.levelup");
        winFirework          = cfg.getBoolean("win-firework", true);
        headFoundBroadcast   = cfg.getBoolean("head-found-broadcast", true);
        discordHeadAnnounce  = cfg.getBoolean("discord-head-announce", false);
        discordChannel       = cfg.getString("discord-channel", "chat");
    }

    public String getFullInventoryAction()  { return fullInventoryAction; }
    public boolean isPreviewShowWeights()   { return previewShowWeights; }
    public String getDefaultOpenSound()     { return defaultOpenSound; }
    public String getDefaultWinSound()      { return defaultWinSound; }
    public boolean isWinFirework()          { return winFirework; }
    public boolean isHeadFoundBroadcast()   { return headFoundBroadcast; }
    public boolean isDiscordHeadAnnounce()  { return discordHeadAnnounce; }
    public String getDiscordChannel()       { return discordChannel; }
}
