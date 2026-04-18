package dev.lofibox.integration;

import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class HeadDatabaseHook implements Listener {

    private static final Random RNG = new Random();

    private HeadDatabaseAPI api;
    private boolean ready = false;

    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent e) {
        api   = new HeadDatabaseAPI();
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    /** Returns a specific head by its HeadDatabase numeric ID. */
    @Nullable
    public ItemStack getHead(String id) {
        if (!ready || id == null || id.isEmpty()) return null;
        try {
            return api.getItemHead(id);
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns all heads in the given HeadDatabase category. Empty list if unavailable. */
    public List<ItemStack> getHeadsByCategory(String category) {
        if (!ready || category == null || category.isEmpty()) return Collections.emptyList();
        try {
            List<ItemStack> heads = api.getHeads(category);
            return heads != null ? heads : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Picks a random head from the given HeadDatabase category.
     * Returns null if HeadDatabase is not ready or the category has no results.
     */
    @Nullable
    public ItemStack getRandomHeadByCategory(String category) {
        List<ItemStack> heads = getHeadsByCategory(category);
        if (heads.isEmpty()) return null;
        return heads.get(RNG.nextInt(heads.size())).clone();
    }
}
