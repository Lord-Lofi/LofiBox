package dev.lofibox.gui;

import dev.lofibox.LofiBox;
import dev.lofibox.box.BoxReward;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.util.ActionRunner;
import dev.lofibox.util.ItemUtil;
import dev.lofibox.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * CSGO-style spin animation. 3-row inventory:
 *   Row 1 (0-8):   Border glass panes
 *   Row 2 (9-17):  The scrolling reward strip — center = slot 13
 *   Row 3 (18-26): Border with yellow highlight under slot 13 (slot 22)
 *
 * Strip length 52, winner placed at index 47.
 * Final display offset = 43 so winner sits at center (43+4=47).
 */
public final class SpinGui extends BukkitRunnable implements InventoryHolder {

    private static final int STRIP_LEN   = 52;
    private static final int WINNER_POS  = 47;
    private static final int VISIBLE     = 9;
    private static final int CENTER_IDX  = 4;
    private static final int FINAL_OFF   = WINNER_POS - CENTER_IDX; // 43

    private final LofiBox plugin;
    private final Player player;
    private final MysteryBox box;
    private final BoxReward winner;
    private final List<ItemStack> strip;
    private final Inventory inv;

    private int currentOffset = 0;
    private int tickAccum     = 0;
    private boolean finished  = false;

    public SpinGui(LofiBox plugin, Player player, MysteryBox box) {
        this.plugin  = plugin;
        this.player  = player;
        this.box     = box;
        this.winner  = box.rollReward(player);
        this.strip   = buildStrip();
        this.inv     = buildInventory();
    }

    @Override
    public Inventory getInventory() { return inv; }

    public Player getPlayer() { return player; }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private List<ItemStack> buildStrip() {
        List<BoxReward> rewards = box.getRewards();
        Random rng = new Random();
        List<ItemStack> s = new ArrayList<>(STRIP_LEN);
        for (int i = 0; i < STRIP_LEN; i++) {
            s.add(rewards.get(rng.nextInt(rewards.size())).getDisplayItem());
        }
        s.set(WINNER_POS, winner.getDisplayItem());
        return s;
    }

    private Inventory buildInventory() {
        Component title = MessageUtil.parse(box.getDisplayName());
        Inventory inv   = Bukkit.createInventory(this, 27, title);

        ItemStack border    = ItemUtil.makeBorderItem();
        ItemStack highlight = ItemUtil.makeHighlightBorderItem();

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 18; i < 27; i++) inv.setItem(i, border);
        inv.setItem(22, highlight);

        renderStrip(inv, 0);
        return inv;
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    public void start() {
        player.openInventory(inv);
        playSound(box.getOpenSound());
        runTaskTimer(plugin, 0L, 1L);
    }

    // ── Animation loop (1 tick period) ────────────────────────────────────────

    @Override
    public void run() {
        if (finished) return;
        if (!player.isOnline()) { cancel(); cleanup(); return; }
        if (!player.getOpenInventory().getTopInventory().equals(inv)) {
            cancel(); cleanup(); return;
        }

        tickAccum++;
        if (tickAccum < ticksNeeded(currentOffset)) return;
        tickAccum = 0;

        currentOffset++;
        renderStrip(inv, currentOffset);

        if (currentOffset >= FINAL_OFF) {
            finished = true;
            cancel();
            highlightWinner();
            scheduleReward();
        }
    }

    /** Ticks required per strip shift — slows down as we near the end. */
    private int ticksNeeded(int offset) {
        if (offset < 20) return 1;
        if (offset < 30) return 2;
        if (offset < 36) return 3;
        if (offset < 40) return 5;
        return 8;
    }

    private void renderStrip(Inventory inv, int offset) {
        for (int i = 0; i < VISIBLE; i++) {
            inv.setItem(9 + i, strip.get(offset + i));
        }
    }

    // ── Win sequence ──────────────────────────────────────────────────────────

    private void highlightWinner() {
        // Replace row-2 non-center slots with border to draw focus
        ItemStack border = ItemUtil.makeBorderItem();
        for (int i = 9; i < 18; i++) {
            if (i != 13) inv.setItem(i, border);
        }
        // Put winner item prominently at center
        ItemStack winItem = applyWinnerLore(winner.getDisplayItem(), winner.getDisplayName());
        inv.setItem(13, winItem);

        playSound(box.getWinSound());
    }

    private ItemStack applyWinnerLore(ItemStack item, String rewardName) {
        ItemStack clone = item.clone();
        ItemMeta meta   = clone.getItemMeta();
        if (meta == null) return clone;
        meta.displayName(MessageUtil.parse(rewardName));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<yellow>✦ You won this! ✦"));
        meta.lore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    private void scheduleReward() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Give item
            ItemStack rewardItem = winner.getDisplayItem();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(rewardItem);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
                plugin.getMessageConfig().send(player, "inventory-full");
            }

            // Run actions
            plugin.getActionRunner().run(
                player,
                winner.getActions(),
                Map.of("reward", winner.getDisplayName(), "box", box.getDisplayName())
            );

            // Stats
            plugin.getStatsManager().increment(player.getUniqueId(), box.getId());

            // Win message
            plugin.getMessageConfig().sendRewardWon(player, box.getDisplayName(), winner.getDisplayName());

            // Firework
            if (plugin.getConfigManager().isWinFirework()) spawnFirework(player.getLocation());

            // Close GUI
            player.closeInventory();
            cleanup();
        }, 40L);
    }

    private void cleanup() {
        plugin.getBoxManager().removeActiveSpin(player.getUniqueId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void playSound(String soundName) {
        try {
            Sound s = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            player.playSound(player.getLocation(), s, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void spawnFirework(Location loc) {
        Firework fw     = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fm = fw.getFireworkMeta();
        fm.setPower(0);
        fm.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.STAR)
            .withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE)
            .withFade(org.bukkit.Color.WHITE)
            .withFlicker()
            .build());
        fw.setFireworkMeta(fm);
        Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 2L);
    }
}
