package dev.lofibox.listeners;

import dev.lofibox.LofiBox;
import dev.lofibox.gui.PreviewGui;
import dev.lofibox.gui.SpinGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    private final LofiBox plugin;

    public MenuListener(LofiBox plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (e.getInventory().getHolder() instanceof SpinGui) {
            // Block ALL interaction during the spin animation
            e.setCancelled(true);
            return;
        }

        if (e.getInventory().getHolder() instanceof PreviewGui preview) {
            e.setCancelled(true);
            if (e.getClickedInventory() == null || !e.getClickedInventory().equals(preview.getInventory())) return;

            int slot = e.getSlot();
            if (slot == 49) {
                player.closeInventory();
            } else if (slot == 45 && preview.getPage() > 0) {
                player.openInventory(preview.prevPage().getInventory());
            } else if (slot == 53 && preview.getPage() < preview.getTotalPages() - 1) {
                player.openInventory(preview.nextPage().getInventory());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SpinGui
                || e.getInventory().getHolder() instanceof PreviewGui) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof SpinGui)) return;

        // If the spin hasn't finished, reopen on the next tick to lock the player in
        if (plugin.getBoxManager().isSpinning(player.getUniqueId())) {
            SpinGui spin = plugin.getBoxManager().getActiveSpin(player.getUniqueId());
            if (spin != null) {
                Bukkit.getScheduler().runTaskLater(plugin,
                    () -> player.openInventory(spin.getInventory()), 1L);
            }
        }
    }
}
