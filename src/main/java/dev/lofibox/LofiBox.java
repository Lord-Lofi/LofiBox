package dev.lofibox;

import dev.lofibox.box.BoxManager;
import dev.lofibox.commands.LofiBoxCommand;
import dev.lofibox.config.ConfigManager;
import dev.lofibox.config.MessageConfig;
import dev.lofibox.editor.ChatInputManager;
import dev.lofibox.editor.EditorManager;
import dev.lofibox.integration.HeadCategoryManager;
import dev.lofibox.integration.HeadDatabaseHook;
import dev.lofibox.integration.VaultHook;
import dev.lofibox.key.KeyManager;
import dev.lofibox.listeners.BoxItemListener;
import dev.lofibox.listeners.EditorListener;
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
    private KeyManager           keyManager;
    private VaultHook            vaultHook;
    private EditorManager        editorManager;
    private ChatInputManager     chatInputManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager    = new ConfigManager(this);
        messageConfig    = new MessageConfig(this);
        statsManager     = new StatsManager(this);
        actionRunner     = new ActionRunner(this);
        keyManager       = new KeyManager(this);

        PluginManager pm = getServer().getPluginManager();

        vaultHook = new VaultHook();
        if (pm.isPluginEnabled("Vault")) {
            if (vaultHook.setup(this)) {
                getLogger().info("Vault economy hook registered.");
            } else {
                getLogger().warning("Vault found but no economy provider — cost features disabled.");
            }
        }

        headDatabaseHook    = new HeadDatabaseHook();
        headCategoryManager = new HeadCategoryManager(this);
        headDatabaseHook.setCategoryManager(headCategoryManager);

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

        editorManager    = new EditorManager(this);
        chatInputManager = new ChatInputManager(this);

        pm.registerEvents(new BoxItemListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(chatInputManager, this);
        pm.registerEvents(new EditorListener(this), this);

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
    public KeyManager getKeyManager()                   { return keyManager; }
    public VaultHook getVaultHook()                     { return vaultHook; }
    public EditorManager getEditorManager()             { return editorManager; }
    public ChatInputManager getChatInputManager()       { return chatInputManager; }
}
