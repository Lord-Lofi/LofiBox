package dev.lofibox.api;

import dev.lofibox.key.KeyTier;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Public API for LofiBox. Obtain the instance via {@link #get()}.
 *
 * <pre>{@code
 * LofiBoxAPI api = LofiBoxAPI.get();
 * if (api == null) return; // LofiBox not loaded
 *
 * // Check if a seasonal box is currently active
 * if (api.isBoxAvailableNow("halloween")) {
 *     shop.listItem("halloween");
 * }
 *
 * // Get the seasonal window of a box
 * LofiBoxAPI.SeasonWindow window = api.getBoxPrimarySeasonWindow("halloween");
 * if (window != null) {
 *     player.sendMessage("Available: " + window.toDisplayString());
 * }
 * }</pre>
 */
public abstract class LofiBoxAPI {

    // ── Static accessor ───────────────────────────────────────────────────────

    /**
     * Returns the LofiBoxAPI instance, or null if LofiBox is not loaded.
     * Check for null before use.
     */
    @Nullable
    public static LofiBoxAPI get() {
        RegisteredServiceProvider<LofiBoxAPI> rsp =
                Bukkit.getServicesManager().getRegistration(LofiBoxAPI.class);
        return rsp == null ? null : rsp.getProvider();
    }

    // ── Box enumeration ───────────────────────────────────────────────────────

    /** Returns the IDs of all currently loaded boxes. */
    public abstract Set<String> getBoxIds();

    /** Returns true if a box with the given ID is loaded. */
    public abstract boolean hasBox(String boxId);

    // ── Box metadata ──────────────────────────────────────────────────────────

    /**
     * Returns the MiniMessage display name of the box, or null if not found.
     * Example: {@code "<gradient:#a78bfa:#6366f1>Mythic Crate</gradient>"}
     */
    @Nullable
    public abstract String getBoxDisplayName(String boxId);

    /**
     * Returns a copy of the box's physical item stack, or null if not found.
     */
    @Nullable
    public abstract ItemStack getBoxItem(String boxId);

    /**
     * Returns the key tier required to open this box, or null if no key is needed.
     */
    @Nullable
    public abstract KeyTier getBoxRequiredKey(String boxId);

    /**
     * Returns the Vault economy cost to open this box, or 0 if free.
     */
    public abstract double getBoxOpenCost(String boxId);

    // ── Seasonal availability ─────────────────────────────────────────────────

    /**
     * Returns true when this box's head-category rewards are currently in season
     * (or if the box has no seasonal restrictions at all).
     *
     * <p>A box is considered unavailable only when it contains at least one
     * seasonal HDB category reward AND every such reward is outside its
     * availability window right now.
     *
     * <p>Use this to gate shop listings: if {@code false}, the box's special
     * content is out of season and it may not be worth selling.
     */
    public abstract boolean isBoxAvailableNow(String boxId);

    /**
     * Returns true if this box has any reward whose head category has a
     * seasonal availability window (i.e. it is not a year-round box).
     */
    public abstract boolean isBoxSeasonal(String boxId);

    /**
     * Returns the {@link SeasonWindow} of the seasonal HDB reward with the
     * highest weight in this box, or null if the box has no seasonal rewards.
     *
     * <p>Use this to display "Available: Oct 1 – Nov 15" to players or
     * shop admins.
     */
    @Nullable
    public abstract SeasonWindow getBoxPrimarySeasonWindow(String boxId);

    // ── Reward detail ─────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of every reward in the box, in definition order.
     * Returns an empty list if the box is not found.
     */
    public abstract List<RewardInfo> getBoxRewards(String boxId);

    // ── Value types ───────────────────────────────────────────────────────────

    /**
     * An inclusive MM-DD to MM-DD date range. Handles year-wrap automatically
     * (e.g. Dec 1 – Jan 31).
     */
    public record SeasonWindow(MonthDay from, MonthDay to) {

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d");

        /** True if today falls within this window. */
        public boolean isActiveNow() {
            MonthDay today = MonthDay.now();
            if (!from.isAfter(to)) {
                return !today.isBefore(from) && !today.isAfter(to);
            }
            // Wraps year-end (e.g. Dec 1 – Jan 31)
            return !today.isBefore(from) || !today.isAfter(to);
        }

        /** Human-readable form, e.g. {@code "Oct 1 – Nov 15"}. */
        public String toDisplayString() {
            return from.format(FMT) + " \u2013 " + to.format(FMT);
        }
    }

    /**
     * A read-only snapshot of a single reward's properties.
     *
     * @param id                 The reward's YAML key.
     * @param displayName        MiniMessage display name.
     * @param weight             Roll weight.
     * @param hdbCategoryReward  True if this reward draws from a HeadDatabase category.
     * @param hdbCategory        The category name, or null if not an HDB reward.
     * @param availabilityWindow The seasonal window for this reward's HDB category,
     *                           or null if year-round.
     * @param doubleChance       Base double-reward % (0 = none).
     * @param doubleChanceWindow The window where the double chance is active,
     *                           or null if always active when {@code doubleChance > 0}.
     */
    public record RewardInfo(
        String id,
        String displayName,
        int weight,
        boolean hdbCategoryReward,
        @Nullable String hdbCategory,
        @Nullable SeasonWindow availabilityWindow,
        int doubleChance,
        @Nullable SeasonWindow doubleChanceWindow
    ) {
        /** True if this reward's head category is currently in season (or year-round). */
        public boolean isAvailableNow() {
            return availabilityWindow == null || availabilityWindow.isActiveNow();
        }

        /** True if the double-reward chance is active right now. */
        public boolean isDoubleChanceActiveNow() {
            if (doubleChance <= 0) return false;
            return doubleChanceWindow == null || doubleChanceWindow.isActiveNow();
        }
    }
}
