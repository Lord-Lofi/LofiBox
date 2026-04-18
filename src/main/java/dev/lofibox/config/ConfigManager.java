package dev.lofibox.config;

import dev.lofibox.LofiBox;

public final class ConfigManager {

    private final LofiBox plugin;

    private String fullInventoryAction;
    private boolean previewShowWeights;
    private String defaultOpenSound;
    private String defaultWinSound;
    private boolean winFirework;

    public ConfigManager(LofiBox plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        var cfg = plugin.getConfig();
        fullInventoryAction  = cfg.getString("full-inventory-action", "DROP").toUpperCase();
        previewShowWeights   = cfg.getBoolean("preview-show-weights", true);
        defaultOpenSound     = cfg.getString("default-open-sound", "block.chest.open");
        defaultWinSound      = cfg.getString("default-win-sound", "entity.player.levelup");
        winFirework          = cfg.getBoolean("win-firework", true);
    }

    public String getFullInventoryAction() { return fullInventoryAction; }
    public boolean isPreviewShowWeights()  { return previewShowWeights; }
    public String getDefaultOpenSound()    { return defaultOpenSound; }
    public String getDefaultWinSound()     { return defaultWinSound; }
    public boolean isWinFirework()         { return winFirework; }
}
