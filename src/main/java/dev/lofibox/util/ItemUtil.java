package dev.lofibox.util;

import dev.lofibox.LofiBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {}

    /**
     * Builds an ItemStack from a config section. Supports:
     *  - head-database-id (HeadDatabase)
     *  - item-data (base64 Paper serialized bytes for custom plugin items)
     *  - material + name + lore + amount + custom-model-data + enchants
     */
    public static ItemStack buildItem(LofiBox plugin, ConfigurationSection sec, String fallbackName) {
        if (sec == null) return defaultItem(fallbackName);

        // HeadDatabase head
        String hdbId = sec.getString("head-database-id");
        if (hdbId != null && !hdbId.isEmpty()) {
            ItemStack head = plugin.getHeadDatabaseHook().getHead(hdbId);
            if (head != null) return applyMeta(head, sec, fallbackName);
        }

        // Base64 custom item data (PDC-preserving, for MMOItems / Oraxen / etc.)
        String base64 = sec.getString("item-data");
        if (base64 != null && !base64.isEmpty()) {
            ItemStack item = deserialize(base64);
            if (item != null) return applyMeta(item, sec, fallbackName);
        }

        // Standard material
        String matStr = sec.getString("material", "CHEST");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.CHEST;

        ItemStack item = new ItemStack(mat, Math.max(1, sec.getInt("amount", 1)));
        return applyMeta(item, sec, fallbackName);
    }

    private static ItemStack applyMeta(ItemStack item, ConfigurationSection sec, String fallbackName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display name
        String nameStr = sec.getString("name", fallbackName);
        if (nameStr != null) meta.displayName(MessageUtil.parse(nameStr));

        // Lore
        List<String> loreStrings = sec.getStringList("lore");
        if (!loreStrings.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreStrings) lore.add(MessageUtil.parse(line));
            meta.lore(lore);
        }

        // Custom model data
        int cmd = sec.getInt("custom-model-data", 0);
        if (cmd != 0) meta.setCustomModelData(cmd);

        // Enchantments
        ConfigurationSection enchSec = sec.getConfigurationSection("enchants");
        if (enchSec != null) {
            for (String key : enchSec.getKeys(false)) {
                Enchantment ench = Enchantment.getByName(key.toUpperCase());
                if (ench == null) continue;
                int level = enchSec.getInt(key, 1);
                if (meta instanceof EnchantmentStorageMeta esm) {
                    esm.addStoredEnchant(ench, level, true);
                } else {
                    meta.addEnchant(ench, level, true);
                }
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack defaultItem(String name) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeHighlightBorderItem() {
        ItemStack item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String serialize(ItemStack item) {
        if (item == null) return null;
        try {
            return Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
