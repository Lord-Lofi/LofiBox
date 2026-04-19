package dev.lofibox.listeners;

import dev.lofibox.LofiBox;
import dev.lofibox.box.MysteryBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BoxItemListener implements Listener {

    private final LofiBox plugin;

    public BoxItemListener(LofiBox plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        // Only fire once (main hand)
        if (e.getHand() != EquipmentSlot.HAND) return;

        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String boxId = plugin.getBoxManager().getBoxId(item);
        if (boxId == null) return;

        e.setCancelled(true);

        if (!player.hasPermission("lofibox.use")) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }

        if (plugin.getBoxManager().isSpinning(player.getUniqueId())) {
            plugin.getMessageConfig().send(player, "already-spinning");
            return;
        }

        MysteryBox box = plugin.getBoxManager().getBox(boxId);
        if (box == null) {
            plugin.getMessageConfig().send(player, "unknown-box", "box", boxId);
            return;
        }

        // Vault cost check — admins bypass
        if (box.hasCost() && !player.hasPermission("lofibox.admin")) {
            var vault = plugin.getVaultHook();
            if (!vault.isAvailable()) {
                plugin.getMessageConfig().send(player, "vault-unavailable");
                return;
            }
            if (!vault.has(player, box.getOpenCost())) {
                plugin.getMessageConfig().send(player, "vault-insufficient-funds",
                    "cost", vault.format(box.getOpenCost()),
                    "balance", vault.format(vault.getBalance(player)));
                return;
            }
        }

        // Key check — admins bypass
        if (box.requiresKey() && !player.hasPermission("lofibox.admin")) {
            if (!plugin.getKeyManager().hasKey(player, box.getRequiredKey())) {
                plugin.getMessageConfig().send(player, "key-required",
                    "key", box.getRequiredKey().getDisplayName(),
                    "box", box.getDisplayName());
                return;
            }
        }

        // Consume one box item BEFORE opening to prevent duplication exploits
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Consume key and deduct cost after box is confirmed consumed
        if (!player.hasPermission("lofibox.admin")) {
            if (box.requiresKey()) {
                plugin.getKeyManager().consumeKey(player, box.getRequiredKey());
                plugin.getMessageConfig().send(player, "key-consumed",
                    "key", box.getRequiredKey().getDisplayName());
            }
            if (box.hasCost()) {
                plugin.getVaultHook().withdraw(player, box.getOpenCost());
                plugin.getMessageConfig().send(player, "vault-charged",
                    "cost", plugin.getVaultHook().format(box.getOpenCost()));
            }
        }

        plugin.getMessageConfig().send(player, "box-opened", "box", box.getDisplayName());
        plugin.getBoxManager().openBox(player, box);
    }
}
