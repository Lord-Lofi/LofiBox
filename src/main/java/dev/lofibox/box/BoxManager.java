package dev.lofibox.box;

import dev.lofibox.LofiBox;
import dev.lofibox.gui.SpinGui;
import dev.lofibox.key.KeyTier;
import dev.lofibox.util.ItemUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public final class BoxManager {

    private final LofiBox plugin;
    private final NamespacedKey boxKey;

    private final Map<String, MysteryBox> boxes      = new LinkedHashMap<>();
    private final Map<UUID, SpinGui>      activeSpins = new HashMap<>();

    public BoxManager(LofiBox plugin) {
        this.plugin = plugin;
        this.boxKey = new NamespacedKey(plugin, "box_id");
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private static final List<String> DEFAULT_BOX_FILES = List.of(
        "example.yml",
        "alphanumeric.yml", "animals.yml",    "anime.yml",         "blocks.yml",
        "christmas.yml",    "decoration.yml",  "dinosaur.yml",      "easter.yml",
        "fantasy.yml",      "flags.yml",       "food_and_drink.yml","furniture.yml",
        "halloween.yml",    "humanoid.yml",    "humans.yml",        "insects.yml",
        "miscellaneous.yml","monsters.yml",    "mythology.yml",     "ocean.yml",
        "plants.yml",       "plushie.yml",     "pokemon.yml",       "random.yml",
        "space.yml",        "st_patricks.yml", "star_wars.yml",     "thanksgiving.yml",
        "valentine.yml",    "vehicles.yml",    "villager.yml",      "winter.yml"
    );

    public void loadAll() {
        boxes.clear();
        File dir = new File(plugin.getDataFolder(), "boxes");
        if (!dir.exists()) {
            dir.mkdirs();
            for (String name : DEFAULT_BOX_FILES) {
                try { plugin.saveResource("boxes/" + name, false); } catch (Exception ignored) {}
            }
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                MysteryBox box = loadBox(f);
                if (box != null) boxes.put(box.getId(), box);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load box: " + f.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + boxes.size() + " box(es).");
    }

    private MysteryBox loadBox(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id          = file.getName().replace(".yml", "");
        String displayName = cfg.getString("name", id);

        // Build box item
        ItemStack boxItem = ItemUtil.buildItem(plugin, cfg.getConfigurationSection("item"), displayName);
        ItemMeta meta = boxItem.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(boxKey, PersistentDataType.STRING, id);
            boxItem.setItemMeta(meta);
        }

        String openSound = cfg.getString("sounds.open",
                plugin.getConfigManager().getDefaultOpenSound());
        String winSound  = cfg.getString("sounds.win",
                plugin.getConfigManager().getDefaultWinSound());

        // Load rewards
        List<BoxReward> rewards = new ArrayList<>();
        var rewardSec = cfg.getConfigurationSection("rewards");
        if (rewardSec != null) {
            for (String key : rewardSec.getKeys(false)) {
                var sec = rewardSec.getConfigurationSection(key);
                if (sec == null) continue;
                int weight      = sec.getInt("weight", 10);
                String name     = sec.getString("display-name", key);
                String permReq    = sec.getString("permission-required", "");
                List<String> actions = sec.getStringList("actions");
                var itemSec       = sec.getConfigurationSection("item");
                String hdbCategory = itemSec != null ? itemSec.getString("head-database-category", "") : "";
                ItemStack item    = ItemUtil.buildItem(plugin, itemSec, name);
                rewards.add(new BoxReward(key, weight, name, item, actions, permReq, hdbCategory));
            }
        }

        if (rewards.isEmpty()) {
            plugin.getLogger().warning("Box '" + id + "' has no rewards — skipping.");
            return null;
        }

        // Optional key requirement
        KeyTier requiredKey = null;
        String keyName = cfg.getString("required-key", "");
        if (!keyName.isBlank()) {
            try {
                requiredKey = KeyTier.valueOf(keyName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Box '" + id + "' has unknown required-key '" + keyName + "' — ignoring.");
            }
        }

        // Optional Vault economy cost
        double openCost = cfg.getDouble("open-cost", 0.0);

        return new MysteryBox(id, displayName, boxItem, rewards, openSound, winSound, requiredKey, openCost);
    }

    // ── Opening ───────────────────────────────────────────────────────────────

    public void openBox(Player player, MysteryBox box) {
        SpinGui gui = new SpinGui(plugin, player, box);
        activeSpins.put(player.getUniqueId(), gui);
        gui.start();
    }

    // ── Active spin tracking ──────────────────────────────────────────────────

    public boolean isSpinning(UUID uuid)           { return activeSpins.containsKey(uuid); }
    public SpinGui getActiveSpin(UUID uuid)        { return activeSpins.get(uuid); }
    public void removeActiveSpin(UUID uuid)        { activeSpins.remove(uuid); }

    // ── Item helpers ──────────────────────────────────────────────────────────

    /** Returns the box item tagged with its ID, or null if the box doesn't exist. */
    public ItemStack createBoxItem(String boxId, int amount) {
        MysteryBox box = boxes.get(boxId);
        if (box == null) return null;
        ItemStack item = box.getBoxItem();
        item.setAmount(Math.max(1, amount));
        return item;
    }

    /** Reads the lofibox:box_id PDC tag from an item, returning null if absent. */
    public String getBoxId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(boxKey, PersistentDataType.STRING);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public MysteryBox getBox(String id)             { return boxes.get(id); }
    public Collection<MysteryBox> getAllBoxes()      { return Collections.unmodifiableCollection(boxes.values()); }
    public Set<String> getBoxIds()                  { return Collections.unmodifiableSet(boxes.keySet()); }
}
