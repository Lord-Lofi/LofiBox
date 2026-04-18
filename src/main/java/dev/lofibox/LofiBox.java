package dev.lofibox;

import dev.lofibox.box.BoxManager;
import dev.lofibox.commands.LofiBoxCommand;
import dev.lofibox.config.ConfigManager;
import dev.lofibox.config.MessageConfig;
import dev.lofibox.integration.HeadCategoryManager;
import dev.lofibox.integration.HeadDatabaseHook;
import dev.lofibox.listeners.BoxItemListener;
import dev.lofibox.listeners.MenuListener;
import dev.lofibox.stats.StatsManager;
import dev.lofibox.util.ActionRunner;
import dev.lofibox.util.PlaceholderHook;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LofiBox extends JavaPlugin {

    private static LofiBox instance;

    private ConfigManager        configManager;
    private MessageConfig        messageConfig;
    private BoxManager           boxManager;
    private StatsManager         statsManager;
    private ActionRunner         actionRunner;
    private HeadDatabaseHook     headDatabaseHook;
    private HeadCategoryManager  headCategoryManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager    = new ConfigManager(this);
        messageConfig    = new MessageConfig(this);
        statsManager     = new StatsManager(this);
        actionRunner     = new ActionRunner(this);

        headDatabaseHook    = new HeadDatabaseHook();
        headCategoryManager = new HeadCategoryManager(this);
        headDatabaseHook.setCategoryManager(headCategoryManager);

        PluginManager pm = getServer().getPluginManager();
        if (pm.isPluginEnabled("HeadDatabase")) {
            pm.registerEvents(headDatabaseHook, this);
            getLogger().info("HeadDatabase hook registered.");
        }

        boxManager = new BoxManager(this);
        boxManager.loadAll();

        PluginCommand cmd = getCommand("lofibox");
        if (cmd != null) {
            LofiBoxCommand handler = new LofiBoxCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        pm.registerEvents(new BoxItemListener(this), this);
        pm.registerEvents(new MenuListener(this), this);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered.");
        }

        getLogger().info("LofiBox v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) statsManager.saveAll();
        getLogger().info("LofiBox disabled.");
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        messageConfig.reload();
        headCategoryManager.reload();
        boxManager.loadAll();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static LofiBox getInstance()          { return instance; }
    public ConfigManager getConfigManager()       { return configManager; }
    public MessageConfig getMessageConfig()       { return messageConfig; }
    public BoxManager getBoxManager()             { return boxManager; }
    public StatsManager getStatsManager()         { return statsManager; }
    public ActionRunner getActionRunner()         { return actionRunner; }
    public HeadDatabaseHook getHeadDatabaseHook()       { return headDatabaseHook; }
    public HeadCategoryManager getHeadCategoryManager() { return headCategoryManager; }
}
