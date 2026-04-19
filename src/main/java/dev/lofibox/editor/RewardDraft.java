package dev.lofibox.editor;

import dev.lofibox.box.BoxReward;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RewardDraft {

    private final String id;
    private String displayName;
    private int weight;
    private ItemStack displayItem;
    private String permissionRequired;
    private final List<String> actions = new ArrayList<>();

    private RewardDraft(String id) {
        this.id = id;
    }

    public static RewardDraft blank(String id) {
        RewardDraft d = new RewardDraft(id);
        d.displayName = id;
        d.weight = 10;
        d.permissionRequired = "";
        return d;
    }

    public static RewardDraft from(BoxReward r) {
        RewardDraft d = new RewardDraft(r.getId());
        d.displayName = r.getDisplayName();
        d.weight = r.getWeight();
        d.displayItem = r.getDisplayItem();
        d.permissionRequired = r.getPermissionRequired();
        d.actions.addAll(r.getActions());
        return d;
    }

    public String getId()                            { return id; }
    public String getDisplayName()                   { return displayName; }
    public void   setDisplayName(String v)           { displayName = v; }
    public int    getWeight()                        { return weight; }
    public void   setWeight(int v)                   { weight = Math.max(1, v); }
    public ItemStack getDisplayItem()                { return displayItem; }
    public void   setDisplayItem(ItemStack v)        { displayItem = v; }
    public String getPermissionRequired()            { return permissionRequired; }
    public void   setPermissionRequired(String v)    { permissionRequired = v == null ? "" : v; }
    public List<String> getActions()                 { return actions; }
    public void   addAction(String a)                { actions.add(a); }

    public boolean removeAction(int index) {
        if (index < 0 || index >= actions.size()) return false;
        actions.remove(index);
        return true;
    }
}
