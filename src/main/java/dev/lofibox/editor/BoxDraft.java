package dev.lofibox.editor;

import dev.lofibox.box.BoxReward;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.key.KeyTier;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BoxDraft {

    private final String id;
    private String displayName;
    private ItemStack boxItem;
    private String openSound;
    private String winSound;
    private KeyTier requiredKey;
    private double openCost;
    private final List<RewardDraft> rewards = new ArrayList<>();
    private final boolean isNew;

    private BoxDraft(String id, boolean isNew) {
        this.id    = id;
        this.isNew = isNew;
    }

    public static BoxDraft newBox(String id) {
        BoxDraft d = new BoxDraft(id, true);
        d.displayName = id;
        d.openSound   = "block.chest.open";
        d.winSound    = "entity.player.levelup";
        d.openCost    = 0.0;
        return d;
    }

    public static BoxDraft fromExisting(MysteryBox box) {
        BoxDraft d = new BoxDraft(box.getId(), false);
        d.displayName = box.getDisplayName();
        d.boxItem     = box.getBoxItem();
        d.openSound   = box.getOpenSound();
        d.winSound    = box.getWinSound();
        d.requiredKey = box.getRequiredKey();
        d.openCost    = box.getOpenCost();
        for (BoxReward r : box.getRewards()) d.rewards.add(RewardDraft.from(r));
        return d;
    }

    public String getId()                          { return id; }
    public boolean isNew()                         { return isNew; }
    public String getDisplayName()                 { return displayName; }
    public void   setDisplayName(String v)         { displayName = v; }
    public ItemStack getBoxItem()                  { return boxItem; }
    public void   setBoxItem(ItemStack v)          { boxItem = v; }
    public String getOpenSound()                   { return openSound; }
    public void   setOpenSound(String v)           { openSound = v == null || v.isBlank() ? "block.chest.open"       : v; }
    public String getWinSound()                    { return winSound; }
    public void   setWinSound(String v)            { winSound  = v == null || v.isBlank() ? "entity.player.levelup"  : v; }
    public KeyTier getRequiredKey()                { return requiredKey; }
    public void   setRequiredKey(KeyTier v)        { requiredKey = v; }
    public double getOpenCost()                    { return openCost; }
    public void   setOpenCost(double v)            { openCost = Math.max(0.0, v); }
    public List<RewardDraft> getRewards()          { return rewards; }
    public void   addReward(RewardDraft r)         { rewards.add(r); }

    public boolean removeReward(int index) {
        if (index < 0 || index >= rewards.size()) return false;
        rewards.remove(index);
        return true;
    }

    public String generateRewardId() {
        int n = 0;
        while (true) {
            final String candidate = "reward_" + n;
            if (rewards.stream().noneMatch(r -> r.getId().equals(candidate))) return candidate;
            n++;
        }
    }
}
