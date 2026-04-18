package dev.lofibox.integration;

import dev.lofibox.LofiBox;
import me.arcaniax.hdb.object.head.Head;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Manages custom head category pools defined in head-categories.yml.
 * Each category is a named pool of HDB head IDs built by searching all heads
 * for matching name/tag terms. Pools are cached in memory and rebuilt on reload.
 */
public final class HeadCategoryManager {

    private static final Random RNG = new Random();

    private final LofiBox plugin;
    private FileConfiguration config;
    // categoryName (lowercase) -> list of matching HDB head ID strings
    private final Map<String, List<String>> pools = new HashMap<>();

    public HeadCategoryManager(LofiBox plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ── Config ────────────────────────────────────────────────────────────────

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "head-categories.yml");
        if (!file.exists()) plugin.saveResource("head-categories.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        loadConfig();
        if (plugin.getHeadDatabaseHook().isReady()) buildPools();
    }

    // ── Pool building ─────────────────────────────────────────────────────────

    /**
     * Iterates every HDB head once and builds pools for all configured categories.
     * Called by HeadDatabaseHook after the HDB database finishes loading, and on reload.
     */
    public void buildPools() {
        pools.clear();
        var categoriesSec = config.getConfigurationSection("categories");
        if (categoriesSec == null) return;

        List<Head> allHeads = plugin.getHeadDatabaseHook().getAllHeads();
        plugin.getLogger().info("Building head category pools from " + allHeads.size() + " heads...");

        for (String categoryName : categoriesSec.getKeys(false)) {
            var sec = categoriesSec.getConfigurationSection(categoryName);
            if (sec == null) continue;

            List<String> searchTerms = resolveSearchTerms(sec);
            if (searchTerms.isEmpty()) continue;

            // Use a LinkedHashSet to deduplicate heads that match multiple terms
            Set<String> matchingIds = new LinkedHashSet<>();
            for (Head head : allHeads) {
                for (String term : searchTerms) {
                    if (matches(head, term)) {
                        matchingIds.add(head.id);
                        break; // no need to check remaining terms for this head
                    }
                }
            }

            pools.put(categoryName.toLowerCase(), new ArrayList<>(matchingIds));
            plugin.getLogger().info(
                "  [HeadCategories] '" + categoryName + "' — " + matchingIds.size()
                + " heads (terms: " + searchTerms + ")"
            );
        }
    }

    /** Reads search as either a plain string or a list of strings. */
    private List<String> resolveSearchTerms(org.bukkit.configuration.ConfigurationSection sec) {
        if (sec.isList("search")) {
            return sec.getStringList("search").stream()
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .toList();
        }
        String single = sec.getString("search", "").toLowerCase().trim();
        return single.isBlank() ? List.of() : List.of(single);
    }

    /** Case-insensitive match against the head's name and every tag. */
    private boolean matches(Head head, String term) {
        if (head.name != null && head.name.toLowerCase().contains(term)) return true;
        if (head.tags != null) {
            for (String tag : head.tags) {
                if (tag != null && tag.toLowerCase().contains(term)) return true;
            }
        }
        return false;
    }

    // ── Random head retrieval ─────────────────────────────────────────────────

    /**
     * Returns a random head from the named custom category pool.
     * Returns null if the category doesn't exist or HDB is unavailable.
     */
    public ItemStack getRandomHead(String categoryName) {
        List<String> ids = pools.get(categoryName.toLowerCase());
        if (ids == null || ids.isEmpty()) return null;
        String id = ids.get(RNG.nextInt(ids.size()));
        return plugin.getHeadDatabaseHook().getHead(id);
    }

    public boolean hasCategory(String name) {
        return pools.containsKey(name.toLowerCase());
    }

    public int poolSize(String name) {
        List<String> ids = pools.get(name.toLowerCase());
        return ids == null ? 0 : ids.size();
    }

    public Set<String> getCategoryNames() {
        return Collections.unmodifiableSet(pools.keySet());
    }
}
