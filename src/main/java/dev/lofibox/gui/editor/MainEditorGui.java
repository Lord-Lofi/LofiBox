package dev.lofibox.gui.editor;

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

public final class MainEditorGui implements InventoryHolder {

    public static final int SLOTS_PER_PAGE = 45;
    public static final int SLOT_PREV      = 45;
    public static final int SLOT_NEW       = 49;
    public static final int SLOT_NEXT      = 53;

    private final LofiBox plugin;
    private final Player player;
    private final List<MysteryBox> boxes;
    private final int page;
    private final int totalPages;
    private final Inventory inv;

    public MainEditorGui(LofiBox plugin, Player player) {
        this(plugin, player, 0);
    }

    public MainEditorGui(LofiBox plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.boxes  = new ArrayList<>(plugin.getBoxManager().getAllBoxes());
        this.totalPages = Math.max(1, (int) Math.ceil((double) boxes.size() / SLOTS_PER_PAGE));
        this.page   = Math.max(0, Math.min(page, totalPages - 1));
        this.inv    = Bukkit.createInventory(this, 54, MessageUtil.parse("<dark_purple>LofiBox Editor"));
        populate();
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void open() { player.openInventory(inv); }

    public int getPage()       { return page; }
    public int getTotalPages() { return totalPages; }

    /** Returns the MysteryBox for the given inventory slot on the current page, or null. */
    public MysteryBox getBoxAt(int slot) {
        if (slot < 0 || slot >= SLOTS_PER_PAGE) return null;
        int index = page * SLOTS_PER_PAGE + slot;
        return index < boxes.size() ? boxes.get(index) : null;
    }

    private void populate() {
        inv.clear();
        ItemStack border = ItemUtil.makeBorderItem();

        // Nav row defaults
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        inv.setItem(SLOT_NEW, makeButton(Material.EMERALD, "<green>Create New Box",
            List.of("<gray>Click to define a brand-new loot box.")));

        if (page > 0)            inv.setItem(SLOT_PREV, makeButton(Material.ARROW, "<gray>← Previous Page", null));
        if (page < totalPages-1) inv.setItem(SLOT_NEXT, makeButton(Material.ARROW, "<gray>Next Page →",     null));

        int start = page * SLOTS_PER_PAGE;
        int end   = Math.min(start + SLOTS_PER_PAGE, boxes.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildBoxSlot(boxes.get(i)));
        }
    }

    private ItemStack buildBoxSlot(MysteryBox box) {
        ItemStack item = box.getBoxItem();
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MessageUtil.parse(box.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<gray>ID: <white>" + box.getId()));
        lore.add(MessageUtil.parse("<gray>Rewards: <white>" + box.getRewards().size()));
        if (box.requiresKey()) lore.add(MessageUtil.parse("<gray>Key: <white>" + box.getRequiredKey().getDisplayName()));
        if (box.hasCost())     lore.add(MessageUtil.parse("<gray>Cost: <gold>" + box.getOpenCost()));
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<yellow>Left-click to edit"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack makeButton(Material mat, String name, List<String> loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MessageUtil.parse(name));
        if (loreLine != null && !loreLine.isEmpty()) {
            meta.lore(loreLine.stream().map(MessageUtil::parse).toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}
