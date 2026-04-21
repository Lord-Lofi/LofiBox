package dev.lofibox.redeem;

import dev.lofibox.LofiBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persists pending (unclaimed) rewards per player to disk.
 * Each player gets a YAML file under plugins/LofiBox/pending/<uuid>.yml
 * containing a list of base64-serialized ItemStacks.
 */
public final class PendingRewardsManager {

    private final LofiBox plugin;
    private final File dir;
    private final Map<UUID, List<ItemStack>> cache = new HashMap<>();

    public PendingRewardsManager(LofiBox plugin) {
        this.plugin = plugin;
        this.dir    = new File(plugin.getDataFolder(), "pending");
        if (!dir.exists()) dir.mkdirs();
    }

    public void addPending(UUID uuid, ItemStack item) {
        List<ItemStack> list = load(uuid);
        list.add(item.clone());
        save(uuid, list);
    }

    public List<ItemStack> getPending(UUID uuid) {
        return Collections.unmodifiableList(load(uuid));
    }

    public boolean hasPending(UUID uuid) {
        return !load(uuid).isEmpty();
    }

    /** Removes the item at the given index. Returns false if index out of range. */
    public boolean remove(UUID uuid, int index) {
        List<ItemStack> list = load(uuid);
        if (index < 0 || index >= list.size()) return false;
        list.remove(index);
        save(uuid, list);
        return true;
    }

    public int count(UUID uuid) {
        return load(uuid).size();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private List<ItemStack> load(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        File file = new File(dir, uuid + ".yml");
        List<ItemStack> list = new ArrayList<>();
        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            List<?> raw = cfg.getList("items");
            if (raw != null) {
                for (Object obj : raw) {
                    if (obj instanceof ItemStack is) list.add(is);
                }
            }
        }
        cache.put(uuid, list);
        return list;
    }

    private void save(UUID uuid, List<ItemStack> items) {
        File file = new File(dir, uuid + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("items", new ArrayList<>(items));
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save pending rewards for " + uuid + ": " + e.getMessage());
        }
        if (items.isEmpty()) file.delete();
    }
}
