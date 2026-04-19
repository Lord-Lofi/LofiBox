package dev.lofibox.editor;

import dev.lofibox.LofiBox;
import dev.lofibox.gui.editor.BoxEditorGui;
import dev.lofibox.gui.editor.MainEditorGui;
import dev.lofibox.util.ItemUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EditorManager {

    private final LofiBox plugin;
    private final Map<UUID, BoxDraft> activeDrafts        = new HashMap<>();
    private final Map<UUID, Integer>  editingRewardIndex  = new HashMap<>();

    public EditorManager(LofiBox plugin) {
        this.plugin = plugin;
    }

    // ── Session management ────────────────────────────────────────────────────

    public void openMainEditor(Player player) {
        new MainEditorGui(plugin, player).open();
    }

    public BoxDraft getActiveDraft(UUID uuid)                 { return activeDrafts.get(uuid); }
    public void     setActiveDraft(UUID uuid, BoxDraft draft) { activeDrafts.put(uuid, draft); }
    public void     clearDraft(UUID uuid) {
        activeDrafts.remove(uuid);
        editingRewardIndex.remove(uuid);
    }

    public void setEditingRewardIndex(UUID uuid, int index)   { editingRewardIndex.put(uuid, index); }
    public int  getEditingRewardIndex(UUID uuid)              { return editingRewardIndex.getOrDefault(uuid, 0); }

    // ── Saving ────────────────────────────────────────────────────────────────

    /**
     * Validates, serializes, and saves the BoxDraft to plugins/LofiBox/boxes/<id>.yml,
     * then reloads all boxes. Returns null on success or an error string on failure.
     */
    public String saveBox(BoxDraft draft) {
        if (draft.getBoxItem() == null) {
            return "Box item is not set — hold an item and click the box item slot.";
        }
        long validRewards = draft.getRewards().stream().filter(r -> r.getDisplayItem() != null).count();
        if (validRewards == 0) {
            return "At least one reward with an item must be added.";
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", draft.getDisplayName());
        cfg.set("item.item-data", ItemUtil.serialize(draft.getBoxItem()));
        cfg.set("sounds.open", draft.getOpenSound());
        cfg.set("sounds.win",  draft.getWinSound());

        if (draft.getRequiredKey() != null) {
            cfg.set("required-key", draft.getRequiredKey().name().toLowerCase());
        }
        if (draft.getOpenCost() > 0) {
            cfg.set("open-cost", draft.getOpenCost());
        }

        for (RewardDraft r : draft.getRewards()) {
            if (r.getDisplayItem() == null) continue;
            String base = "rewards." + r.getId();
            cfg.set(base + ".weight", r.getWeight());
            cfg.set(base + ".display-name", r.getDisplayName());
            if (!r.getPermissionRequired().isEmpty()) {
                cfg.set(base + ".permission-required", r.getPermissionRequired());
            }
            if (!r.getActions().isEmpty()) {
                cfg.set(base + ".actions", r.getActions());
            }
            cfg.set(base + ".item.item-data", ItemUtil.serialize(r.getDisplayItem()));
        }

        File dir  = new File(plugin.getDataFolder(), "boxes");
        File file = new File(dir, draft.getId() + ".yml");
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save box '" + draft.getId() + "': " + e.getMessage());
            return "File write failed: " + e.getMessage();
        }

        plugin.getBoxManager().loadAll();
        return null;
    }

    /**
     * Deletes the box YAML file and reloads all boxes.
     */
    public void deleteBox(String boxId) {
        File file = new File(plugin.getDataFolder(), "boxes/" + boxId + ".yml");
        if (file.exists()) file.delete();
        plugin.getBoxManager().loadAll();
    }
}
