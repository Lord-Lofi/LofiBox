package dev.lofibox.box;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BoxReward {

    private final String id;
    private final int weight;
    private final String displayName;
    private final ItemStack displayItem;
    private final List<String> actions;
    private final String permissionRequired;

    public BoxReward(String id, int weight, String displayName, ItemStack displayItem,
                     List<String> actions, String permissionRequired) {
        this.id                  = id;
        this.weight              = weight;
        this.displayName         = displayName;
        this.displayItem         = displayItem;
        this.actions             = List.copyOf(actions);
        this.permissionRequired  = permissionRequired == null ? "" : permissionRequired;
    }

    public boolean isAvailableTo(Player player) {
        if (permissionRequired.isEmpty()) return true;
        return player.hasPermission(permissionRequired) || player.hasPermission("lofibox.bypass");
    }

    public String getId()                  { return id; }
    public int getWeight()                 { return weight; }
    public String getDisplayName()         { return displayName; }
    public ItemStack getDisplayItem()      { return displayItem.clone(); }
    public List<String> getActions()       { return actions; }
    public String getPermissionRequired()  { return permissionRequired; }
}
