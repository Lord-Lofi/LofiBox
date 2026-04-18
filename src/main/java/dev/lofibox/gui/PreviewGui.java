package dev.lofibox.gui;

import dev.lofibox.LofiBox;
import dev.lofibox.box.BoxReward;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.util.ItemUtil;
import dev.lofibox.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 6-row preview inventory showing all rewards with their chances.
 * Rows 1-5 (slots 0-44): reward items, 45 per page.
 * Row 6 (slots 45-53):   navigation — prev (45), close (49), next (53).
 */
public final class PreviewGui implements InventoryHolder {

    private static final int REWARDS_PER_PAGE = 45;
    private static final int SLOT_PREV        = 45;
    private static final int SLOT_CLOSE       = 49;
    private static final int SLOT_NEXT        = 53;

    private final LofiBox plugin;
    private final MysteryBox box;
    private final List<BoxReward> rewards;
    private final int totalPages;
    private int page;
    private final Inventory inv;

    public PreviewGui(LofiBox plugin, MysteryBox box, int page) {
        this.plugin     = plugin;
        this.box        = box;
        this.rewards    = box.getRewards();
        this.totalPages = Math.max(1, (int) Math.ceil((double) rewards.size() / REWARDS_PER_PAGE));
        this.page       = Math.max(0, Math.min(page, totalPages - 1));
        this.inv        = Bukkit.createInventory(this, 54,
                plugin.getMessageConfig().get("preview-title", "box", box.getDisplayName()));
        populate();
    }

    public PreviewGui(LofiBox plugin, MysteryBox box) {
        this(plugin, box, 0);
    }

    @Override
    public Inventory getInventory() { return inv; }

    public MysteryBox getBox()  { return box; }
    public int getPage()        { return page; }
    public int getTotalPages()  { return totalPages; }

    public void open(Player player) {
        player.openInventory(inv);
    }

    private void populate() {
        inv.clear();
        ItemStack border = ItemUtil.makeBorderItem();

        // Fill nav row
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Prev
        if (page > 0) {
            inv.setItem(SLOT_PREV, navItem("ARROW", "<gray>← Previous Page"));
        }
        // Close
        inv.setItem(SLOT_CLOSE, navItem("BARRIER", "<red>Close"));
        // Next
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, navItem("ARROW", "<gray>Next Page →"));
        }

        // Rewards
        int start = page * REWARDS_PER_PAGE;
        int end   = Math.min(start + REWARDS_PER_PAGE, rewards.size());
        boolean showWeights = plugin.getConfigManager().isPreviewShowWeights();

        for (int i = start; i < end; i++) {
            BoxReward reward = rewards.get(i);
            inv.setItem(i - start, buildRewardSlot(reward, showWeights));
        }
    }

    private ItemStack buildRewardSlot(BoxReward reward, boolean showWeights) {
        ItemStack item = reward.getDisplayItem();
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MessageUtil.parse(reward.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        double chance = (double) reward.getWeight() / box.getTotalWeight() * 100.0;
        if (showWeights) {
            lore.add(MessageUtil.parse("<gray>Weight: <white>" + reward.getWeight()));
            lore.add(MessageUtil.parse(String.format("<gray>Chance: <yellow>%.2f%%", chance)));
        } else {
            lore.add(MessageUtil.parse("<gray>Rarity: <white>" + rarityLabel(chance)));
        }

        if (!reward.getPermissionRequired().isEmpty()) {
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<red>Requires: <white>" + reward.getPermissionRequired()));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String rarityLabel(double chance) {
        if (chance >= 30) return "<gray>Common";
        if (chance >= 10) return "<green>Uncommon";
        if (chance >= 3)  return "<aqua>Rare";
        if (chance >= 1)  return "<light_purple>Epic";
        return "<gold>Legendary";
    }

    private ItemStack navItem(String material, String name) {
        var mat  = org.bukkit.Material.matchMaterial(material);
        if (mat == null) mat = org.bukkit.Material.STONE;
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public PreviewGui nextPage() { return new PreviewGui(plugin, box, page + 1); }
    public PreviewGui prevPage() { return new PreviewGui(plugin, box, page - 1); }
}
