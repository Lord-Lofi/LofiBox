package dev.lofibox.api;

import dev.lofibox.box.BoxReward;
import dev.lofibox.box.BoxManager;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.integration.HeadCategoryManager;
import dev.lofibox.key.KeyTier;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class LofiBoxAPIImpl extends LofiBoxAPI {

    private final BoxManager boxManager;
    private final HeadCategoryManager hcm;

    public LofiBoxAPIImpl(BoxManager boxManager, HeadCategoryManager hcm) {
        this.boxManager = boxManager;
        this.hcm        = hcm;
    }

    @Override
    public Set<String> getBoxIds() {
        return boxManager.getBoxIds();
    }

    @Override
    public boolean hasBox(String boxId) {
        return boxManager.getBox(boxId) != null;
    }

    @Override
    @Nullable
    public String getBoxDisplayName(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        return box == null ? null : box.getDisplayName();
    }

    @Override
    @Nullable
    public ItemStack getBoxItem(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        return box == null ? null : box.getBoxItem();
    }

    @Override
    @Nullable
    public KeyTier getBoxRequiredKey(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        return box == null ? null : box.getRequiredKey();
    }

    @Override
    public double getBoxOpenCost(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        return box == null ? 0 : box.getOpenCost();
    }

    @Override
    public boolean isBoxAvailableNow(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        if (box == null) return false;

        boolean hasSeasonalReward = false;
        for (BoxReward reward : box.getRewards()) {
            if (!reward.isHdbCategoryReward()) continue;
            String cat = reward.getHdbCategory();
            MonthDay from = hcm.getCategoryAvailableFrom(cat);
            if (from == null) return true; // at least one year-round HDB reward → available
            hasSeasonalReward = true;
            SeasonWindow window = new SeasonWindow(from, hcm.getCategoryAvailableTo(cat));
            if (window.isActiveNow()) return true;
        }

        // No HDB rewards at all → always available (non-HDB items only)
        // All HDB rewards are seasonal and none are in-window → unavailable
        return !hasSeasonalReward;
    }

    @Override
    public boolean isBoxSeasonal(String boxId) {
        return getBoxPrimarySeasonWindow(boxId) != null;
    }

    @Override
    @Nullable
    public SeasonWindow getBoxPrimarySeasonWindow(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        if (box == null) return null;

        SeasonWindow best = null;
        int bestWeight = -1;
        for (BoxReward reward : box.getRewards()) {
            if (!reward.isHdbCategoryReward()) continue;
            String cat = reward.getHdbCategory();
            MonthDay from = hcm.getCategoryAvailableFrom(cat);
            if (from == null) continue; // year-round, skip
            if (reward.getWeight() > bestWeight) {
                bestWeight = reward.getWeight();
                best = new SeasonWindow(from, hcm.getCategoryAvailableTo(cat));
            }
        }
        return best;
    }

    @Override
    public List<RewardInfo> getBoxRewards(String boxId) {
        MysteryBox box = boxManager.getBox(boxId);
        if (box == null) return List.of();

        List<RewardInfo> result = new ArrayList<>();
        for (BoxReward reward : box.getRewards()) {
            String cat = reward.getHdbCategory();
            SeasonWindow avail = null;
            SeasonWindow dbl   = null;
            int doubleChance   = 0;

            if (cat != null) {
                MonthDay af = hcm.getCategoryAvailableFrom(cat);
                MonthDay at = hcm.getCategoryAvailableTo(cat);
                if (af != null && at != null) avail = new SeasonWindow(af, at);

                doubleChance = hcm.getCategoryBaseDoubleChance(cat);
                MonthDay df  = hcm.getCategoryDoubleFrom(cat);
                MonthDay dt  = hcm.getCategoryDoubleTo(cat);
                if (df != null && dt != null) dbl = new SeasonWindow(df, dt);
            }

            result.add(new RewardInfo(
                reward.getId(),
                reward.getDisplayName(),
                reward.getWeight(),
                reward.isHdbCategoryReward(),
                cat,
                avail,
                doubleChance,
                dbl
            ));
        }
        return Collections.unmodifiableList(result);
    }
}
