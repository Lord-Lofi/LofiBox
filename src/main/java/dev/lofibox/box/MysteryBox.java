package dev.lofibox.box;

import dev.lofibox.key.KeyTier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public final class MysteryBox {

    private static final Random RNG = new Random();

    private final String id;
    private final String displayName;
    private final ItemStack boxItem;
    private final List<BoxReward> rewards;
    private final String openSound;
    private final String winSound;
    private final int totalWeight;
    private final KeyTier requiredKey;

    public MysteryBox(String id, String displayName, ItemStack boxItem,
                      List<BoxReward> rewards, String openSound, String winSound,
                      KeyTier requiredKey) {
        this.id          = id;
        this.displayName = displayName;
        this.boxItem     = boxItem;
        this.rewards     = List.copyOf(rewards);
        this.openSound   = openSound;
        this.winSound    = winSound;
        this.totalWeight = rewards.stream().mapToInt(BoxReward::getWeight).sum();
        this.requiredKey = requiredKey;
    }

    /** Rolls a random reward, respecting per-player permission gates. Re-rolls if needed. */
    public BoxReward rollReward(Player player) {
        List<BoxReward> eligible = rewards.stream().filter(r -> r.isAvailableTo(player)).toList();
        if (eligible.isEmpty()) return rewards.get(0);

        int total = eligible.stream().mapToInt(BoxReward::getWeight).sum();
        int roll  = RNG.nextInt(total);
        int cum   = 0;
        for (BoxReward r : eligible) {
            cum += r.getWeight();
            if (roll < cum) return r;
        }
        return eligible.get(eligible.size() - 1);
    }

    public String getId()                    { return id; }
    public String getDisplayName()           { return displayName; }
    public ItemStack getBoxItem()            { return boxItem.clone(); }
    public List<BoxReward> getRewards()      { return rewards; }
    public String getOpenSound()             { return openSound; }
    public String getWinSound()              { return winSound; }
    public int getTotalWeight()              { return totalWeight; }
    /** Null means no key is required to open this box. */
    public KeyTier getRequiredKey()          { return requiredKey; }
    public boolean requiresKey()             { return requiredKey != null; }
}
