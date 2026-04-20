package dev.lofibox.integration;

import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.arcaniax.hdb.enums.CategoryEnum;
import me.arcaniax.hdb.object.head.Head;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class HeadDatabaseHook implements Listener {

    private static final Random RNG = new Random();

    private HeadDatabaseAPI api;
    private boolean ready = false;
    private HeadCategoryManager categoryManager;

    public void setCategoryManager(HeadCategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent e) {
        api   = new HeadDatabaseAPI();
        ready = true;
        if (categoryManager != null) categoryManager.buildPools();
    }

    public boolean isReady() { return ready; }

    // ── Specific head by ID ───────────────────────────────────────────────────

    @Nullable
    public ItemStack getHead(String id) {
        if (!ready || id == null || id.isEmpty()) return null;
        try {
            return api.getItemHead(id);
        } catch (Exception e) {
            return null;
        }
    }

    // ── All heads across every category ──────────────────────────────────────

    /** Returns every Head object across all HDB categories. Used by HeadCategoryManager to build search pools. */
    public List<Head> getAllHeads() {
        if (!ready) return Collections.emptyList();
        List<Head> all = new ArrayList<>();
        for (CategoryEnum cat : CategoryEnum.values()) {
            try {
                List<Head> heads = api.getHeads(cat);
                if (heads != null) all.addAll(heads);
            } catch (Exception ignored) {}
        }
        return all;
    }

    // ── Direct CategoryEnum lookup (fallback for non-custom categories) ───────

    /**
     * Picks a random head directly from an HDB CategoryEnum by name.
     * Used as a fallback when head-database-category doesn't match a custom category.
     * Returns null if the name doesn't map to a valid CategoryEnum or HDB isn't ready.
     */
    @Nullable
    public ItemStack getRandomHeadByEnum(String categoryName) {
        if (!ready) return null;
        try {
            CategoryEnum cat = CategoryEnum.valueOf(categoryName.toUpperCase());
            List<Head> heads = api.getHeads(cat);
            if (heads == null || heads.isEmpty()) return null;
            Head head = heads.get(RNG.nextInt(heads.size()));
            return api.getItemHead(head.id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Returns all head IDs belonging to a native HDB CategoryEnum.
     * Returns an empty list if the name is not a valid enum value or HDB isn't ready.
     */
    public List<String> getHeadIdsByEnum(String categoryName) {
        if (!ready) return Collections.emptyList();
        try {
            CategoryEnum cat = CategoryEnum.valueOf(categoryName.toUpperCase());
            List<Head> heads = api.getHeads(cat);
            if (heads == null || heads.isEmpty()) return Collections.emptyList();
            List<String> ids = new ArrayList<>(heads.size());
            for (Head h : heads) ids.add(h.id);
            return ids;
        } catch (IllegalArgumentException ignored) {
            return Collections.emptyList();
        }
    }
}
