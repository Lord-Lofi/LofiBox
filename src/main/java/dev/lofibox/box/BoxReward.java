package dev.lofibox.box;

import dev.lofibox.LofiBox;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BoxReward {

    private final String id;
    private final int weight;
    private final String displayName;
    private final ItemStack displayItem;   // fallback / static item
    private final List<String> actions;
    private final String permissionRequired;
    private final String hdbCategory;     // null = not a random HDB reward

    public BoxReward(String id, int weight, String displayName, ItemStack displayItem,
                     List<String> actions, String permissionRequired, String hdbCategory) {
        this.id                 = id;
        this.weight             = weight;
        this.displayName        = displayName;
        this.displayItem        = displayItem;
        this.actions            = List.copyOf(actions);
        this.permissionRequired = permissionRequired == null ? "" : permissionRequired;
        this.hdbCategory        = (hdbCategory == null || hdbCategory.isBlank()) ? null : hdbCategory;
    }

    public boolean isAvailableTo(Player player) {
        if (permissionRequired.isEmpty()) return true;
        return player.hasPermission(permissionRequired) || player.hasPermission("lofibox.bypass");
    }

    /**
     * Returns the item for this reward. If head-database-category is set, a random head
     * from that category is picked each time this is called — giving every spin/give a
     * different head from the pool. Falls back to the static display item if HDB is not
     * ready or the category returns no results.
     */
    public ItemStack getDisplayItem() {
        if (hdbCategory != null) {
            ItemStack head = LofiBox.getInstance().getHeadDatabaseHook().getRandomHeadByCategory(hdbCategory);
            if (head != null) return head;
        }
        return displayItem.clone();
    }

    public boolean isHdbCategoryReward()   { return hdbCategory != null; }
    public String getHdbCategory()         { return hdbCategory; }
    public String getId()                  { return id; }
    public int getWeight()                 { return weight; }
    public String getDisplayName()         { return displayName; }
    public List<String> getActions()       { return actions; }
    public String getPermissionRequired()  { return permissionRequired; }
}
