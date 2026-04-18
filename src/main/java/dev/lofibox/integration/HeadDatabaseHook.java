package dev.lofibox.integration;

import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class HeadDatabaseHook implements Listener {

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

    @Nullable
    public ItemStack getHead(String id) {
        if (!ready || id == null || id.isEmpty()) return null;
        try {
            return api.getItemHead(id);
        } catch (Exception e) {
            return null;
        }
    }
}
