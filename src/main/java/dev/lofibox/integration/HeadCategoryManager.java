package dev.lofibox.integration;

import dev.lofibox.LofiBox;
import me.arcaniax.hdb.object.head.Head;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages custom head category pools defined in head-categories.yml.
 * Each pool is built by searching HDB head names/tags for configured terms.
 * Supports seasonal availability windows and double-reward chances.
 */
public final class HeadCategoryManager {

    private static final Random RNG = new Random();
    private static final DateTimeFormatter MD_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private record CategoryMeta(
        List<String> ids,
        MonthDay availableFrom,
        MonthDay availableTo,
        int doubleChance,
        MonthDay doubleFrom,
        MonthDay doubleTo
    ) {}

    private final LofiBox plugin;
    private FileConfiguration config;
    private final Map<String, CategoryMeta> categories = new HashMap<>();

    public HeadCategoryManager(LofiBox plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ── Config ────────────────────────────────────────────────────────────────

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "head-categories.yml");
        if (!file.exists()) plugin.saveResource("head-categories.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
        // Merge any missing categories/keys from the default head-categories.yml
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(java.util.Objects.requireNonNull(
                plugin.getResource("head-categories.yml")), StandardCharsets.UTF_8));
        config.setDefaults(defaults);
        config.options().copyDefaults(true);
        try { config.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Could not save head-categories.yml after migration: " + e.getMessage());
        }
    }

    public void reload() {
        loadConfig();
        if (plugin.getHeadDatabaseHook().isReady()) buildPools();
    }

    // ── Pool building ─────────────────────────────────────────────────────────

    public void buildPools() {
        categories.clear();
        ConfigurationSection sec = config.getConfigurationSection("categories");
        if (sec == null) return;

        List<Head> allHeads = plugin.getHeadDatabaseHook().getAllHeads();
        plugin.getLogger().info("Building head category pools from " + allHeads.size() + " heads...");

        for (String name : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(name);
            if (cs == null) continue;

            List<String> ids;
            if (cs.getBoolean("random", false)) {
                ids = new ArrayList<>(allHeads.size());
                for (Head h : allHeads) ids.add(h.id);
            } else {
                Set<String> found = new LinkedHashSet<>();

                // Pull from native HDB category if specified
                String hdbCat = cs.getString("hdb-category");
                if (hdbCat != null && !hdbCat.isBlank()) {
                    found.addAll(plugin.getHeadDatabaseHook().getHeadIdsByEnum(hdbCat.trim()));
                }

                // Also search by keyword terms (union with above)
                List<String> terms = resolveTerms(cs);
                for (Head h : allHeads) {
                    for (String t : terms) {
                        if (matches(h, t)) { found.add(h.id); break; }
                    }
                }

                if (found.isEmpty() && hdbCat == null) continue;
                ids = new ArrayList<>(found);
            }

            MonthDay availFrom  = parseMonthDay(cs.getString("available-from"));
            MonthDay availTo    = parseMonthDay(cs.getString("available-to"));
            int doubleChance    = cs.getInt("double-chance", 0);
            MonthDay doubleFrom = parseMonthDay(cs.getString("double-chance-from"));
            MonthDay doubleTo   = parseMonthDay(cs.getString("double-chance-to"));

            categories.put(name.toLowerCase(),
                new CategoryMeta(ids, availFrom, availTo, doubleChance, doubleFrom, doubleTo));

            plugin.getLogger().info(
                "  [HeadCategories] '" + name + "' — " + ids.size() + " heads"
                + (availFrom != null ? " | seasonal: " + availFrom + "–" + availTo : "")
                + (doubleChance > 0 ? " | double: " + doubleChance + "%" : "")
            );
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns a random head from the named category, or null if out of season
     * or unavailable.
     */
    public ItemStack getRandomHead(String categoryName) {
        CategoryMeta meta = categories.get(categoryName.toLowerCase());
        if (meta == null || meta.ids().isEmpty()) return null;
        if (!isInRange(meta.availableFrom(), meta.availableTo())) return null;
        String id = meta.ids().get(RNG.nextInt(meta.ids().size()));
        return plugin.getHeadDatabaseHook().getHead(id);
    }

    /**
     * Returns the double-reward percentage (0–100) applicable right now,
     * or 0 if double-reward is not active for this category today.
     */
    public int getEffectiveDoubleChance(String categoryName) {
        CategoryMeta meta = categories.get(categoryName.toLowerCase());
        if (meta == null || meta.doubleChance() <= 0) return 0;
        if (!isInRange(meta.doubleFrom(), meta.doubleTo())) return 0;
        return meta.doubleChance();
    }

    public boolean hasCategory(String name)  { return categories.containsKey(name.toLowerCase()); }
    public int poolSize(String name) {
        CategoryMeta m = categories.get(name.toLowerCase());
        return m == null ? 0 : m.ids().size();
    }
    public Set<String> getCategoryNames() { return Collections.unmodifiableSet(categories.keySet()); }

    /** Returns the start of the availability window for the category, or null if year-round. */
    public MonthDay getCategoryAvailableFrom(String name) {
        CategoryMeta m = categories.get(name.toLowerCase());
        return m == null ? null : m.availableFrom();
    }

    /** Returns the end of the availability window for the category, or null if year-round. */
    public MonthDay getCategoryAvailableTo(String name) {
        CategoryMeta m = categories.get(name.toLowerCase());
        return m == null ? null : m.availableTo();
    }

    /** Returns the base double-reward chance (0–100), regardless of current date. */
    public int getCategoryBaseDoubleChance(String name) {
        CategoryMeta m = categories.get(name.toLowerCase());
        return m == null ? 0 : m.doubleChance();
    }

    /** Returns the start of the double-chance window, or null if always active. */
    public MonthDay getCategoryDoubleFrom(String name) {
        CategoryMeta m = categories.get(name.toLowerCase());
        return m == null ? null : m.doubleFrom();
    }

    /** Returns the end of the double-chance window, or null if always active. */
    public MonthDay getCategoryDoubleTo(String name) {
        CategoryMeta m = categories.get(name.toLowerCase());
        return m == null ? null : m.doubleTo();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private List<String> resolveTerms(ConfigurationSection cs) {
        if (cs.isList("search")) {
            return cs.getStringList("search").stream()
                .map(String::toLowerCase).filter(s -> !s.isBlank()).toList();
        }
        String s = cs.getString("search", "").toLowerCase().trim();
        return s.isBlank() ? List.of() : List.of(s);
    }

    private boolean matches(Head head, String term) {
        if (head.name != null && head.name.toLowerCase().contains(term)) return true;
        if (head.tags != null) {
            for (String tag : head.tags) {
                if (tag != null && tag.toLowerCase().contains(term)) return true;
            }
        }
        return false;
    }

    private MonthDay parseMonthDay(String s) {
        if (s == null || s.isBlank()) return null;
        try { return MonthDay.parse(s, MD_FMT); } catch (Exception e) { return null; }
    }

    /**
     * True if today is within [from, to]. Handles year-wrap (e.g. Dec–Jan).
     * Null bounds = unbounded (always true).
     */
    private boolean isInRange(MonthDay from, MonthDay to) {
        if (from == null || to == null) return true;
        MonthDay today = MonthDay.now();
        if (!from.isAfter(to)) {
            return !today.isBefore(from) && !today.isAfter(to);
        } else {
            // Wraps year-end (e.g. Dec 01 – Jan 31)
            return !today.isBefore(from) || !today.isAfter(to);
        }
    }
}
