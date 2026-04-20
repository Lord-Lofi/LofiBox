package dev.lofibox.gui;

import dev.lofibox.LofiBox;
import dev.lofibox.box.MysteryBox;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Paginated stats GUI showing how many times a player has opened each box.
 * Each entry is clickable and opens the box's preview GUI.
 *
 * Layout (6 rows):
 *   Row 0 (0-8):    border
 *   Rows 1-4 (9-44): up to 36 entry slots
 *   Row 5 (45-53):  controls
 */
public final class StatsGui implements InventoryHolder {

    public static final int ENTRIES_PER_PAGE = 36;
    public static final int ENTRIES_OFFSET   = 9;

    public static final int SLOT_CLOSE = 45;
    public static final int SLOT_PREV  = 48;
    public static final int SLOT_INFO  = 49;
    public static final int SLOT_NEXT  = 50;
    public static final int SLOT_TOTAL = 53;

    private final LofiBox plugin;
    private final Player viewer;
    private final Player subject;
    private final List<Map.Entry<String, Integer>> entries;
    private final int page;
    private final int totalPages;
    private final Inventory inv;

    public StatsGui(LofiBox plugin, Player viewer, Player subject, int page) {
        this.plugin   = plugin;
        this.viewer   = viewer;
        this.subject  = subject;

        // Sort most-opened first
        this.entries = plugin.getStatsManager().getAll(subject.getUniqueId())
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        this.totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE));
        this.page       = Math.max(0, Math.min(page, totalPages - 1));

        String titleStr = viewer.getUniqueId().equals(subject.getUniqueId())
                ? "<dark_purple>✦ Your Stats"
                : "<dark_purple>✦ " + subject.getName() + "'s Stats";
        this.inv = Bukkit.createInventory(this, 54, MessageUtil.parse(titleStr));
        populate();
    }

    @Override
    public Inventory getInventory() { return inv; }
    public void open()              { viewer.openInventory(inv); }
    public int getPage()            { return page; }
    public int getTotalPages()      { return totalPages; }
    public Player getSubject()      { return subject; }

    /** Returns the box ID for the entry at the given inventory slot, or null. */
    public String getBoxIdAt(int slot) {
        int rel = slot - ENTRIES_OFFSET;
        if (rel < 0 || rel >= ENTRIES_PER_PAGE) return null;
        int idx = page * ENTRIES_PER_PAGE + rel;
        return idx < entries.size() ? entries.get(idx).getKey() : null;
    }

    // ── Population ────────────────────────────────────────────────────────────

    private void populate() {
        inv.clear();
        ItemStack border = ItemUtil.makeBorderItem();

        // Row 0: header border
        for (int i = 0; i < 9; i++) inv.setItem(i, border);

        // Rows 1-4: entries
        int start = page * ENTRIES_PER_PAGE;
        int end   = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            inv.setItem(ENTRIES_OFFSET + (i - start), buildEntrySlot(entry.getKey(), entry.getValue()));
        }

        // Fill empty entry slots with border
        for (int i = end - start; i < ENTRIES_PER_PAGE; i++) {
            inv.setItem(ENTRIES_OFFSET + i, border);
        }

        // Row 5: controls
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        inv.setItem(SLOT_CLOSE, makeBtn(Material.BARRIER,
                "<red>Close", List.of("<gray>Close this menu.")));

        if (page > 0) {
            inv.setItem(SLOT_PREV, makeBtn(Material.ARROW,
                    "<gray>← Previous", List.of("<dark_gray>Page " + page + " of " + totalPages)));
        }

        inv.setItem(SLOT_INFO, makeBtn(Material.PAPER,
                "<white>Page " + (page + 1) + " / " + totalPages,
                List.of("<gray>" + entries.size() + " box" + (entries.size() == 1 ? "" : "es") + " opened total")));

        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, makeBtn(Material.ARROW,
                    "<gray>Next →", List.of("<dark_gray>Page " + (page + 2) + " of " + totalPages)));
        }

        int total = plugin.getStatsManager().getTotalOpened(subject.getUniqueId());
        inv.setItem(SLOT_TOTAL, makeBtn(Material.NETHER_STAR,
                "<yellow>Total Opened",
                List.of("<white>" + total + " <gray>box" + (total == 1 ? "" : "es") + " opened overall")));

        // Empty state
        if (entries.isEmpty()) {
            inv.setItem(22, makeBtn(Material.GRAY_STAINED_GLASS_PANE,
                    "<gray>No boxes opened yet.", List.of("<dark_gray>Get opening!")));
        }
    }

    private ItemStack buildEntrySlot(String boxId, int count) {
        MysteryBox box = plugin.getBoxManager().getBox(boxId);

        ItemStack icon;
        String displayName;

        if (box != null) {
            icon        = box.getBoxItem();
            displayName = box.getDisplayName();
        } else {
            icon        = new ItemStack(Material.BARRIER);
            displayName = "<red>" + boxId + " <dark_gray>(removed)";
        }

        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        meta.displayName(MessageUtil.parse(displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<gray>Opened: <white>" + count + " <gray>time" + (count == 1 ? "" : "s")));
        if (box != null) {
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<yellow>Click to preview rewards"));
        }
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
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
