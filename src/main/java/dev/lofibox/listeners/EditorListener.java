package dev.lofibox.listeners;

import dev.lofibox.LofiBox;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.editor.BoxDraft;
import dev.lofibox.editor.EditorManager;
import dev.lofibox.editor.RewardDraft;
import dev.lofibox.gui.editor.BoxEditorGui;
import dev.lofibox.gui.editor.MainEditorGui;
import dev.lofibox.gui.editor.RewardEditorGui;
import dev.lofibox.key.KeyTier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class EditorListener implements Listener {

    private final LofiBox plugin;

    public EditorListener(LofiBox plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Object holder = e.getInventory().getHolder();
        if (holder instanceof MainEditorGui gui) {
            e.setCancelled(true);
            handleMain(e, player, gui);
        } else if (holder instanceof BoxEditorGui gui) {
            boolean inTop = e.getClickedInventory() != null && e.getClickedInventory().equals(gui.getInventory());
            if (inTop || e.isShiftClick()) e.setCancelled(true);
            if (inTop) handleBox(e, player, gui);
        } else if (holder instanceof RewardEditorGui gui) {
            boolean inTop = e.getClickedInventory() != null && e.getClickedInventory().equals(gui.getInventory());
            if (inTop || e.isShiftClick()) e.setCancelled(true);
            if (inTop) handleReward(e, player, gui);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        Object holder = e.getInventory().getHolder();
        if (!(holder instanceof MainEditorGui || holder instanceof BoxEditorGui || holder instanceof RewardEditorGui)) return;
        int topSize = e.getInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < topSize) { e.setCancelled(true); return; }
        }
    }

    // ── Main editor ───────────────────────────────────────────────────────────

    private void handleMain(InventoryClickEvent e, Player player, MainEditorGui gui) {
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(gui.getInventory())) return;
        int slot = e.getSlot();

        if (slot == MainEditorGui.SLOT_PREV && gui.getPage() > 0) {
            new MainEditorGui(plugin, player, gui.getPage() - 1).open();
        } else if (slot == MainEditorGui.SLOT_NEXT && gui.getPage() < gui.getTotalPages() - 1) {
            new MainEditorGui(plugin, player, gui.getPage() + 1).open();
        } else if (slot == MainEditorGui.SLOT_NEW) {
            plugin.getChatInputManager().await(player,
                "Enter a new box ID (letters, numbers, underscores):", null, rawId -> {
                    String boxId = rawId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    if (plugin.getBoxManager().getBox(boxId) != null) {
                        player.sendMessage(plugin.getMessageConfig().get("editor-box-id-taken", "id", boxId));
                        new MainEditorGui(plugin, player).open();
                        return;
                    }
                    BoxDraft draft = BoxDraft.newBox(boxId);
                    plugin.getEditorManager().setActiveDraft(player.getUniqueId(), draft);
                    new BoxEditorGui(plugin, player, draft).open();
                });
        } else if (slot < MainEditorGui.SLOTS_PER_PAGE) {
            MysteryBox box = gui.getBoxAt(slot);
            if (box == null) return;
            BoxDraft draft = BoxDraft.fromExisting(box);
            plugin.getEditorManager().setActiveDraft(player.getUniqueId(), draft);
            new BoxEditorGui(plugin, player, draft).open();
        }
    }

    // ── Box editor ────────────────────────────────────────────────────────────

    private void handleBox(InventoryClickEvent e, Player player, BoxEditorGui gui) {
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(gui.getInventory())) return;
        int slot = e.getSlot();
        BoxDraft draft = gui.getDraft();
        EditorManager em = plugin.getEditorManager();

        // ── Box item: capture from cursor ─────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_BOX_ITEM) {
            ItemStack cursor = e.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage(plugin.getMessageConfig().get("editor-hold-item")); return;
            }
            ItemStack captured = cursor.clone();
            draft.setBoxItem(captured);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setItemOnCursor(null);
                player.getInventory().addItem(captured.clone()).values()
                    .forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
                new BoxEditorGui(plugin, player, draft, gui.getRewardPage()).open();
            }, 1L);
            return;
        }

        // ── Display name ──────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_NAME) {
            plugin.getChatInputManager().await(player,
                "Enter display name (MiniMessage formatting supported):",
                gui.getInventory(), name -> {
                    draft.setDisplayName(name);
                    new BoxEditorGui(plugin, player, draft, gui.getRewardPage()).open();
                });
            return;
        }

        // ── Required key: left-click cycles, right-click clears ───────────────
        if (slot == BoxEditorGui.SLOT_REQ_KEY) {
            if (e.getClick() == ClickType.RIGHT) {
                draft.setRequiredKey(null);
            } else {
                KeyTier[] tiers = KeyTier.values();
                KeyTier cur = draft.getRequiredKey();
                if (cur == null) {
                    draft.setRequiredKey(tiers[0]);
                } else {
                    int next = cur.ordinal() + 1;
                    draft.setRequiredKey(next >= tiers.length ? null : tiers[next]);
                }
            }
            new BoxEditorGui(plugin, player, draft, gui.getRewardPage()).open();
            return;
        }

        // ── Open cost ─────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_COST) {
            plugin.getChatInputManager().await(player,
                "Enter open cost (e.g. 500.0) — enter 0 to disable:",
                gui.getInventory(), costStr -> {
                    try {
                        draft.setOpenCost(Double.parseDouble(costStr));
                    } catch (NumberFormatException ex) {
                        player.sendMessage(plugin.getMessageConfig().get("editor-invalid-number"));
                    }
                    new BoxEditorGui(plugin, player, draft, gui.getRewardPage()).open();
                });
            return;
        }

        // ── Open sound ────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_OPEN_SOUND) {
            plugin.getChatInputManager().await(player,
                "Enter open sound key (e.g. block.chest.open):",
                gui.getInventory(), sound -> {
                    draft.setOpenSound(sound);
                    new BoxEditorGui(plugin, player, draft, gui.getRewardPage()).open();
                });
            return;
        }

        // ── Win sound ─────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_WIN_SOUND) {
            plugin.getChatInputManager().await(player,
                "Enter win sound key (e.g. entity.player.levelup):",
                gui.getInventory(), sound -> {
                    draft.setWinSound(sound);
                    new BoxEditorGui(plugin, player, draft, gui.getRewardPage()).open();
                });
            return;
        }

        // ── Add reward ────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_ADD_REWARD) {
            String id     = draft.generateRewardId();
            RewardDraft r = RewardDraft.blank(id);
            int newIndex  = draft.getRewards().size();
            draft.addReward(r);
            em.setEditingRewardIndex(player.getUniqueId(), newIndex);
            new RewardEditorGui(plugin, player, r, newIndex).open();
            return;
        }

        // ── Reward pagination ─────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_PREV && gui.getRewardPage() > 0) {
            new BoxEditorGui(plugin, player, draft, gui.getRewardPage() - 1).open();
            return;
        }
        if (slot == BoxEditorGui.SLOT_NEXT && gui.getRewardPage() < gui.getTotalRewardPages() - 1) {
            new BoxEditorGui(plugin, player, draft, gui.getRewardPage() + 1).open();
            return;
        }

        // ── Save ──────────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_SAVE) {
            String error = em.saveBox(draft);
            if (error != null) {
                player.sendMessage(plugin.getMessageConfig().get("editor-save-error", "error", error));
            } else {
                player.sendMessage(plugin.getMessageConfig().get("editor-save-success", "box", draft.getId()));
                em.clearDraft(player.getUniqueId());
                new MainEditorGui(plugin, player).open();
            }
            return;
        }

        // ── Delete ────────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_DELETE) {
            if (e.getClick() != ClickType.SHIFT_LEFT && e.getClick() != ClickType.SHIFT_RIGHT) {
                player.sendMessage(plugin.getMessageConfig().get("editor-shift-to-delete"));
                return;
            }
            em.deleteBox(draft.getId());
            em.clearDraft(player.getUniqueId());
            player.sendMessage(plugin.getMessageConfig().get("editor-deleted", "box", draft.getId()));
            new MainEditorGui(plugin, player).open();
            return;
        }

        // ── Back ──────────────────────────────────────────────────────────────
        if (slot == BoxEditorGui.SLOT_BACK) {
            em.clearDraft(player.getUniqueId());
            new MainEditorGui(plugin, player).open();
            return;
        }

        // ── Reward slot click ─────────────────────────────────────────────────
        int rewardIndex = gui.getRewardIndexAt(slot);
        if (rewardIndex >= 0) {
            RewardDraft reward = draft.getRewards().get(rewardIndex);
            if (e.getClick() == ClickType.SHIFT_RIGHT) {
                draft.removeReward(rewardIndex);
                int safePage = Math.min(gui.getRewardPage(), gui.getTotalRewardPages() - 1);
                new BoxEditorGui(plugin, player, draft, Math.max(0, safePage)).open();
            } else {
                em.setEditingRewardIndex(player.getUniqueId(), rewardIndex);
                new RewardEditorGui(plugin, player, reward, rewardIndex).open();
            }
        }
    }

    // ── Reward editor ─────────────────────────────────────────────────────────

    private void handleReward(InventoryClickEvent e, Player player, RewardEditorGui gui) {
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(gui.getInventory())) return;
        int slot = e.getSlot();
        RewardDraft reward = gui.getDraft();
        EditorManager em   = plugin.getEditorManager();
        BoxDraft boxDraft  = em.getActiveDraft(player.getUniqueId());

        if (boxDraft == null) {
            new MainEditorGui(plugin, player).open();
            return;
        }

        // ── Capture reward item from cursor ──────────────────────────────────
        if (slot == RewardEditorGui.SLOT_DISPLAY_ITEM) {
            ItemStack cursor = e.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage(plugin.getMessageConfig().get("editor-hold-item")); return;
            }
            ItemStack captured = cursor.clone();
            reward.setDisplayItem(captured);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setItemOnCursor(null);
                player.getInventory().addItem(captured.clone()).values()
                    .forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
                new RewardEditorGui(plugin, player, reward, gui.getRewardIndex()).open();
            }, 1L);
            return;
        }

        // ── Display name ──────────────────────────────────────────────────────
        if (slot == RewardEditorGui.SLOT_NAME) {
            plugin.getChatInputManager().await(player,
                "Enter reward display name (MiniMessage supported):",
                gui.getInventory(), name -> {
                    reward.setDisplayName(name);
                    new RewardEditorGui(plugin, player, reward, gui.getRewardIndex()).open();
                });
            return;
        }

        // ── Weight ────────────────────────────────────────────────────────────
        if (slot == RewardEditorGui.SLOT_WEIGHT) {
            int delta = switch (e.getClick()) {
                case LEFT        -> 1;
                case RIGHT       -> -1;
                case SHIFT_LEFT  -> 10;
                case SHIFT_RIGHT -> -10;
                default          -> 0;
            };
            if (delta != 0) {
                reward.setWeight(reward.getWeight() + delta);
                new RewardEditorGui(plugin, player, reward, gui.getRewardIndex()).open();
            }
            return;
        }

        // ── Permission ────────────────────────────────────────────────────────
        if (slot == RewardEditorGui.SLOT_PERMISSION) {
            plugin.getChatInputManager().await(player,
                "Enter permission node (leave blank to remove):",
                gui.getInventory(), perm -> {
                    reward.setPermissionRequired(perm.trim());
                    new RewardEditorGui(plugin, player, reward, gui.getRewardIndex()).open();
                });
            return;
        }

        // ── Add action ────────────────────────────────────────────────────────
        if (slot == RewardEditorGui.SLOT_ADD_ACTION) {
            plugin.getChatInputManager().await(player,
                "Enter action (e.g. [message] <green>Congrats!):",
                gui.getInventory(), action -> {
                    reward.addAction(action);
                    new RewardEditorGui(plugin, player, reward, gui.getRewardIndex()).open();
                });
            return;
        }

        // ── Remove action ─────────────────────────────────────────────────────
        int actionIndex = gui.getActionIndexAt(slot);
        if (actionIndex >= 0) {
            reward.removeAction(actionIndex);
            new RewardEditorGui(plugin, player, reward, gui.getRewardIndex()).open();
            return;
        }

        // ── Save and return to box editor ─────────────────────────────────────
        if (slot == RewardEditorGui.SLOT_SAVE) {
            if (reward.getDisplayItem() == null) {
                player.sendMessage(plugin.getMessageConfig().get("editor-reward-no-item"));
                return;
            }
            int rPage = em.getEditingRewardIndex(player.getUniqueId()) / BoxEditorGui.REWARDS_PER_PAGE;
            new BoxEditorGui(plugin, player, boxDraft, rPage).open();
            return;
        }

        // ── Back ──────────────────────────────────────────────────────────────
        if (slot == RewardEditorGui.SLOT_BACK) {
            int rPage = em.getEditingRewardIndex(player.getUniqueId()) / BoxEditorGui.REWARDS_PER_PAGE;
            new BoxEditorGui(plugin, player, boxDraft, rPage).open();
        }
    }
}
