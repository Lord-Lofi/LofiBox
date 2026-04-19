package dev.lofibox.gui.editor;

import dev.lofibox.LofiBox;
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

import java.util.List;

/**
 * 4-row reward editor.
 *
 * Row 0 (0-8):   display item, name, weight, permission (property buttons)
 * Row 1 (9-17):  separator, then actions header
 * Row 2 (18-25): action items (click to remove); slot 26 = "Add Action"
 * Row 3 (27-35): Back (27), Save (31), rest borders
 */
public final class RewardEditorGui implements InventoryHolder {

    public static final int SLOT_DISPLAY_ITEM  = 0;
    public static final int SLOT_NAME          = 2;
    public static final int SLOT_WEIGHT        = 4;
    public static final int SLOT_PERMISSION    = 6;

    // Actions area: slots 9-25 (17 action slots) + slot 26 (Add Action button)
    public static final int SLOT_ACTIONS_START = 9;
    public static final int MAX_ACTIONS        = 17;
    public static final int SLOT_ADD_ACTION    = 26;

    public static final int SLOT_BACK          = 27;
    public static final int SLOT_SAVE          = 31;

    private final LofiBox plugin;
    private final Player player;
    private final RewardDraft draft;
    private final int rewardIndex;
    private final Inventory inv;

    public RewardEditorGui(LofiBox plugin, Player player, RewardDraft draft, int rewardIndex) {
        this.plugin       = plugin;
        this.player       = player;
        this.draft        = draft;
        this.rewardIndex  = rewardIndex;
        this.inv          = Bukkit.createInventory(this, 36,
                MessageUtil.parse("<dark_purple>Reward: <white>" + draft.getId()));
        populate();
    }

    @Override
    public Inventory getInventory() { return inv; }
    public void open()              { player.openInventory(inv); }
    public RewardDraft getDraft()   { return draft; }
    public int getRewardIndex()     { return rewardIndex; }

    /** Returns the action index for the given slot, or -1. */
    public int getActionIndexAt(int slot) {
        int rel = slot - SLOT_ACTIONS_START;
        if (rel < 0 || rel >= MAX_ACTIONS) return -1;
        return rel < draft.getActions().size() ? rel : -1;
    }

    private void populate() {
        inv.clear();
        ItemStack border = ItemUtil.makeBorderItem();

        // ── Row 0: properties ────────────────────────────────────────────────
        for (int i = 0; i < 9; i++) inv.setItem(i, border);

        // Display item
        if (draft.getDisplayItem() != null) {
            ItemStack di = draft.getDisplayItem().clone();
            di.setAmount(1);
            ItemMeta m = di.getItemMeta();
            if (m != null) {
                m.displayName(MessageUtil.parse("<aqua>Reward Item"));
                m.lore(List.of(Component.empty(),
                    MessageUtil.parse("<gray>Hold an item and left-click"),
                    MessageUtil.parse("<gray>to replace the reward item.")));
                di.setItemMeta(m);
            }
            inv.setItem(SLOT_DISPLAY_ITEM, di);
        } else {
            inv.setItem(SLOT_DISPLAY_ITEM, BoxEditorGui.btn(Material.BARRIER, "<aqua>Reward Item",
                List.of("<gray>Hold an item and left-click", "<gray>to set the reward item.")));
        }

        inv.setItem(SLOT_NAME, BoxEditorGui.btn(Material.WRITABLE_BOOK, "<aqua>Display Name",
            List.of("<gray>Current: " + draft.getDisplayName(), "", "<yellow>Click to change.")));

        inv.setItem(SLOT_WEIGHT, BoxEditorGui.btn(Material.ORANGE_STAINED_GLASS_PANE,
            "<aqua>Weight: <white>" + draft.getWeight(),
            List.of("<gray>Left-click: +1 | Right-click: -1",
                    "<gray>Shift+left: +10 | Shift+right: -10")));

        String permDisplay = draft.getPermissionRequired().isEmpty() ? "<gray><i>None" : draft.getPermissionRequired();
        inv.setItem(SLOT_PERMISSION, BoxEditorGui.btn(Material.PAPER, "<aqua>Permission Required",
            List.of("<gray>Current: " + permDisplay, "", "<yellow>Click to change.", "<gray>Leave blank to remove.")));

        // ── Rows 1-2: actions ─────────────────────────────────────────────────
        List<String> actions = draft.getActions();
        for (int i = 0; i < MAX_ACTIONS; i++) {
            int slot = SLOT_ACTIONS_START + i;
            if (i < actions.size()) {
                inv.setItem(slot, buildActionItem(i, actions.get(i)));
            } else {
                inv.setItem(slot, border);
            }
        }
        inv.setItem(SLOT_ADD_ACTION, BoxEditorGui.btn(Material.LIME_DYE, "<green>Add Action",
            List.of("<gray>Supported: [message] [actionbar] [title]",
                    "<gray>[sound] [command] [console]")));

        // ── Row 3: control buttons ────────────────────────────────────────────
        for (int i = 27; i < 36; i++) inv.setItem(i, border);
        inv.setItem(SLOT_BACK, BoxEditorGui.btn(Material.ARROW, "<gray>← Back to Box", (String) null));
        inv.setItem(SLOT_SAVE, BoxEditorGui.btn(Material.EMERALD, "<green>Apply & Back",
            List.of("<gray>Apply changes and return to the box editor.")));
    }

    private ItemStack buildActionItem(int index, String action) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MessageUtil.parse("<white>" + action));
        meta.lore(List.of(
            Component.empty(),
            MessageUtil.parse("<dark_gray>#" + index),
            MessageUtil.parse("<red>Click to remove")
        ));
        item.setItemMeta(meta);
        return item;
    }
}
