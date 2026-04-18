package dev.lofibox.stats;

import dev.lofibox.LofiBox;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StatsManager {

    private final LofiBox plugin;
    private final File dir;
    // uuid -> (boxId -> count)
    private final Map<UUID, Map<String, Integer>> cache = new HashMap<>();

    public StatsManager(LofiBox plugin) {
        this.plugin = plugin;
        this.dir    = new File(plugin.getDataFolder(), "stats");
        if (!dir.exists()) dir.mkdirs();
    }

    public void increment(UUID uuid, String boxId) {
        Map<String, Integer> map = load(uuid);
        map.merge(boxId, 1, Integer::sum);
    }

    public int getOpened(UUID uuid, String boxId) {
        return load(uuid).getOrDefault(boxId, 0);
    }

    public int getTotalOpened(UUID uuid) {
        return load(uuid).values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<String, Integer> getAll(UUID uuid) {
        return new HashMap<>(load(uuid));
    }

    private Map<String, Integer> load(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        File file = new File(dir, uuid + ".yml");
        Map<String, Integer> map = new HashMap<>();
        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                map.put(key, cfg.getInt(key, 0));
            }
        }
        cache.put(uuid, map);
        return map;
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Integer>> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    private void save(UUID uuid, Map<String, Integer> data) {
        File file = new File(dir, uuid + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        data.forEach(cfg::set);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats for " + uuid + ": " + e.getMessage());
        }
    }
}
