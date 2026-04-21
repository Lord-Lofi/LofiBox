package dev.lofibox.gui;

import dev.lofibox.LofiBox;
import dev.lofibox.util.ItemUtil;
import dev.lofibox.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Paginated GUI showing a player's pending (unclaimed) rewards.
 *
 * Layout (6 rows):
 *   Row 0 (0-8):     border
 *   Rows 1-4 (9-44): up to 36 pending reward slots — click to claim
 *   Row 5 (45-53):   controls
 */
public final class RedeemGui implements InventoryHolder {

    public static final int ENTRIES_PER_PAGE = 36;
    public static final int ENTRIES_OFFSET   = 9;

    public static final int SLOT_CLOSE = 45;
    public static final int SLOT_PREV  = 48;
    public static final int SLOT_INFO  = 49;
    public static final int SLOT_NEXT  = 50;
    public static final int SLOT_COUNT = 53;

    private final LofiBox plugin;
    private final Player player;
    private final List<ItemStack> pending;
    private final int page;
    private final int totalPages;
    private final Inventory inv;

    public RedeemGui(LofiBox plugin, Player player, int page) {
        this.plugin   = plugin;
        this.player   = player;
        this.pending  = plugin.getPendingRewardsManager().getPending(player.getUniqueId());

        this.totalPages = Math.max(1, (int) Math.ceil((double) pending.size() / ENTRIES_PER_PAGE));
        this.page       = Math.max(0, Math.min(page, totalPages - 1));

        this.inv = Bukkit.createInventory(this, 54, MessageUtil.parse("<dark_purple>✦ Pending Rewards"));
        populate();
    }

    @Override
    public Inventory getInventory() { return inv; }
    public void open()              { player.openInventory(inv); }
    public int getPage()            { return page; }
    public int getTotalPages()      { return totalPages; }
    public Player getPlayer()       { return player; }

    /** Returns the index into the full pending list for the given inventory slot, or -1. */
    public int getPendingIndexAt(int slot) {
        int rel = slot - ENTRIES_OFFSET;
        if (rel < 0 || rel >= ENTRIES_PER_PAGE) return -1;
        int idx = page * ENTRIES_PER_PAGE + rel;
        return idx < pending.size() ? idx : -1;
    }

    // ── Population ────────────────────────────────────────────────────────────

    private void populate() {
        inv.clear();
        ItemStack border = ItemUtil.makeBorderItem();

        for (int i = 0; i < 9; i++) inv.setItem(i, border);

        int start = page * ENTRIES_PER_PAGE;
        int end   = Math.min(start + ENTRIES_PER_PAGE, pending.size());

        for (int i = start; i < end; i++) {
            ItemStack item = pending.get(i).clone();
            tagClaimable(item);
            inv.setItem(ENTRIES_OFFSET + (i - start), item);
        }

        for (int i = end - start; i < ENTRIES_PER_PAGE; i++) {
            inv.setItem(ENTRIES_OFFSET + i, border);
        }

        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        inv.setItem(SLOT_CLOSE, makeBtn(Material.BARRIER,
                "<red>Close", List.of("<gray>Close this menu.")));

        if (page > 0) {
            inv.setItem(SLOT_PREV, makeBtn(Material.ARROW,
                    "<gray>← Previous", List.of("<dark_gray>Page " + page + " of " + totalPages)));
        }

        inv.setItem(SLOT_INFO, makeBtn(Material.PAPER,
                "<white>Page " + (page + 1) + " / " + totalPages,
                List.of("<gray>" + pending.size() + " reward" + (pending.size() == 1 ? "" : "s") + " pending")));

        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, makeBtn(Material.ARROW,
                    "<gray>Next →", List.of("<dark_gray>Page " + (page + 2) + " of " + totalPages)));
        }

        inv.setItem(SLOT_COUNT, makeBtn(Material.CHEST,
                "<yellow>Pending Rewards",
                List.of("<white>" + pending.size() + " <gray>item" + (pending.size() == 1 ? "" : "s") + " waiting")));

        if (pending.isEmpty()) {
            inv.setItem(22, makeBtn(Material.LIME_STAINED_GLASS_PANE,
                    "<green>All caught up!", List.of("<gray>No pending rewards.")));
        }
    }

    private void tagClaimable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Component> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.lore()) : new java.util.ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<yellow>Click to claim"));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static ItemStack makeBtn(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MessageUtil.parse(name));
        if (lore != null) meta.lore(lore.stream().map(MessageUtil::parse).toList());
        item.setItemMeta(meta);
        return item;
    }
}
