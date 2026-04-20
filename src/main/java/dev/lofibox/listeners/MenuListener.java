package dev.lofibox.listeners;

import dev.lofibox.LofiBox;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.gui.PreviewGui;
import dev.lofibox.gui.SpinGui;
import dev.lofibox.gui.StatsGui;
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

        if (e.getInventory().getHolder() instanceof StatsGui stats) {
            e.setCancelled(true);
            if (e.getClickedInventory() == null || !e.getClickedInventory().equals(stats.getInventory())) return;

            int slot = e.getSlot();

            if (slot == StatsGui.SLOT_CLOSE) {
                player.closeInventory();
            } else if (slot == StatsGui.SLOT_PREV && stats.getPage() > 0) {
                new StatsGui(plugin, player, stats.getSubject(), stats.getPage() - 1).open();
            } else if (slot == StatsGui.SLOT_NEXT && stats.getPage() < stats.getTotalPages() - 1) {
                new StatsGui(plugin, player, stats.getSubject(), stats.getPage() + 1).open();
            } else {
                // Entry click — open preview for that box
                String boxId = stats.getBoxIdAt(slot);
                if (boxId == null) return;
                MysteryBox box = plugin.getBoxManager().getBox(boxId);
                if (box == null) return;
                new PreviewGui(plugin, box).open(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SpinGui
                || e.getInventory().getHolder() instanceof PreviewGui
                || e.getInventory().getHolder() instanceof StatsGui) {
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
