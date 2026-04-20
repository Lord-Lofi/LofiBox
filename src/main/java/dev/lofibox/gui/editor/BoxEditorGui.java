package dev.lofibox.gui.editor;

import dev.lofibox.LofiBox;
import dev.lofibox.editor.BoxDraft;
import dev.lofibox.editor.RewardDraft;
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

/**
 * 6-row box editor GUI.
 *
 * Row 0 (slots 0-8):    box item, name, required-key, open-cost, sounds
 * Row 1 (slots 9-17):   separator (all borders)
 * Rows 2-4 (18-44):     reward list (27 per page)
 * Row 5 (slots 45-53):  action buttons
 */
public final class BoxEditorGui implements InventoryHolder {

    // Row 0 — property slots
    public static final int SLOT_BOX_ITEM   = 0;
    public static final int SLOT_NAME       = 2;
    public static final int SLOT_REQ_KEY    = 3;
    public static final int SLOT_COST       = 4;
    public static final int SLOT_OPEN_SOUND = 5;
    public static final int SLOT_WIN_SOUND  = 6;

    // Reward area
    public static final int REWARDS_OFFSET  = 18;
    public static final int REWARDS_PER_PAGE = 27;

    // Row 5 — action buttons
    public static final int SLOT_BACK       = 45;
    public static final int SLOT_ADD_REWARD = 46;
    public static final int SLOT_PREV       = 48;
    public static final int SLOT_NEXT       = 50;
    public static final int SLOT_SAVE       = 52;
    public static final int SLOT_DELETE     = 53;

    private final LofiBox plugin;
    private final Player player;
    private final BoxDraft draft;
    private final int rewardPage;
    private final Inventory inv;

    public BoxEditorGui(LofiBox plugin, Player player, BoxDraft draft) {
        this(plugin, player, draft, 0);
    }

    public BoxEditorGui(LofiBox plugin, Player player, BoxDraft draft, int rewardPage) {
        this.plugin      = plugin;
        this.player      = player;
        this.draft       = draft;
        this.rewardPage  = rewardPage;
        this.inv         = Bukkit.createInventory(this, 54,
                MessageUtil.parse("<dark_purple>Box: <white>" + draft.getId()));
        populate();
    }

    @Override
    public Inventory getInventory() { return inv; }
    public void open()              { player.openInventory(inv); }
    public BoxDraft getDraft()      { return draft; }
    public int getRewardPage()      { return rewardPage; }

    public int getTotalRewardPages() {
        return Math.max(1, (int) Math.ceil((double) draft.getRewards().size() / REWARDS_PER_PAGE));
    }

    /** Returns the draft reward index for an inventory slot, or -1 if none. */
    public int getRewardIndexAt(int slot) {
        int rel = slot - REWARDS_OFFSET;
        if (rel < 0 || rel >= REWARDS_PER_PAGE) return -1;
        int idx = rewardPage * REWARDS_PER_PAGE + rel;
        return idx < draft.getRewards().size() ? idx : -1;
    }

    private void populate() {
        inv.clear();
        ItemStack border = ItemUtil.makeBorderItem();

        // ── Row 0: properties ────────────────────────────────────────────────
        inv.setItem(1, border);
        inv.setItem(7, border);
        inv.setItem(8, border);

        // Box item slot
        if (draft.getBoxItem() != null) {
            ItemStack bi = draft.getBoxItem().clone();
            bi.setAmount(1);
            ItemMeta m = bi.getItemMeta();
            if (m != null) {
                m.displayName(MessageUtil.parse("<aqua>Box Item"));
                m.lore(List.of(Component.empty(),
                    MessageUtil.parse("<gray>Pick up an item onto your cursor,"),
                    MessageUtil.parse("<gray>then click here to replace the box item.")));
                bi.setItemMeta(m);
            }
            inv.setItem(SLOT_BOX_ITEM, bi);
        } else {
            inv.setItem(SLOT_BOX_ITEM, btn(Material.CHEST, "<aqua>Box Item",
                List.of("<gray>Pick up an item onto your cursor,", "<gray>then click here to set the box item.")));
        }

        inv.setItem(SLOT_NAME, btn(Material.WRITABLE_BOOK, "<aqua>Display Name",
            List.of("<gray>Current: " + draft.getDisplayName(), "", "<yellow>Click to change.")));

        String keyLabel = draft.getRequiredKey() != null ? draft.getRequiredKey().getDisplayName() : "<gray><i>None";
        inv.setItem(SLOT_REQ_KEY, btn(Material.TRIPWIRE_HOOK, "<aqua>Required Key",
            List.of("<gray>Current: " + keyLabel, "", "<yellow>Left-click to cycle tiers.", "<yellow>Right-click to clear.")));

        String costLabel = draft.getOpenCost() > 0 ? String.valueOf(draft.getOpenCost()) : "<gray><i>Free";
        inv.setItem(SLOT_COST, btn(Material.GOLD_INGOT, "<aqua>Open Cost",
            List.of("<gray>Current: <gold>" + costLabel, "", "<yellow>Click to change.", "<gray>Enter 0 to disable.")));

        inv.setItem(SLOT_OPEN_SOUND, btn(Material.NOTE_BLOCK, "<aqua>Open Sound",
            List.of("<gray>Current: <white>" + draft.getOpenSound(), "", "<yellow>Click to change.")));

        inv.setItem(SLOT_WIN_SOUND, btn(Material.JUKEBOX, "<aqua>Win Sound",
            List.of("<gray>Current: <white>" + draft.getWinSound(), "", "<yellow>Click to change.")));

        // ── Row 1: separator ─────────────────────────────────────────────────
        for (int i = 9; i < 18; i++) inv.setItem(i, border);

        // ── Rows 2-4: rewards ─────────────────────────────────────────────────
        List<RewardDraft> rewards = draft.getRewards();
        int start = rewardPage * REWARDS_PER_PAGE;
        int end   = Math.min(start + REWARDS_PER_PAGE, rewards.size());
        for (int i = start; i < end; i++) {
            inv.setItem(REWARDS_OFFSET + (i - start), buildRewardSlot(rewards.get(i)));
        }
        for (int i = end - start; i < REWARDS_PER_PAGE; i++) {
            inv.setItem(REWARDS_OFFSET + i, border);
        }

        // ── Row 5: action buttons ─────────────────────────────────────────────
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        inv.setItem(SLOT_BACK,       btn(Material.ARROW,         "<gray>← Back",      (String) null));
        inv.setItem(SLOT_ADD_REWARD, btn(Material.LIME_DYE,      "<green>Add Reward",  "<gray>Add a new reward to this box."));
        inv.setItem(SLOT_SAVE,       btn(Material.EMERALD_BLOCK,  "<green>Save Box",
            List.of("<gray>Save & reload. Requires box item + ≥1 reward.")));
        inv.setItem(SLOT_DELETE,     btn(Material.TNT,            "<red>Delete Box",
            List.of("<gray>Shift-click to permanently delete.", "<red>This cannot be undone!")));

        if (rewardPage > 0)                  inv.setItem(SLOT_PREV, btn(Material.ARROW, "<gray>← Previous Rewards", (String) null));
        if (rewardPage < getTotalRewardPages()-1) inv.setItem(SLOT_NEXT, btn(Material.ARROW, "<gray>Next Rewards →",    (String) null));
    }

    private ItemStack buildRewardSlot(RewardDraft r) {
        ItemStack item = r.getDisplayItem() != null ? r.getDisplayItem().clone() : new ItemStack(Material.BARRIER);
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MessageUtil.parse(r.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<gray>Weight: <white>" + r.getWeight()));
        lore.add(MessageUtil.parse("<gray>Actions: <white>" + r.getActions().size()));
        if (!r.getPermissionRequired().isEmpty())
            lore.add(MessageUtil.parse("<red>Perm: <white>" + r.getPermissionRequired()));
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<yellow>Left-click to edit"));
        lore.add(MessageUtil.parse("<red>Shift-right-click to remove"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack btn(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MessageUtil.parse(name));
        if (lore != null && !lore.isEmpty()) meta.lore(lore.stream().map(MessageUtil::parse).toList());
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack btn(Material mat, String name, String loreLine) {
        return btn(mat, name, loreLine != null ? List.of(loreLine) : null);
    }
}
