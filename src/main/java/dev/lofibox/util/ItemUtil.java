package dev.lofibox.util;

import dev.lofibox.LofiBox;
import net.kyori.adventure.text.Component;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                Enchantment ench = findEnchantment(key);
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

    // Old Bukkit enchantment names (used by Enchantment.getByName) -> Minecraft keys,
    // so existing configs keep working.
    private static final Map<String, String> LEGACY_ENCHANTS = Map.ofEntries(
            Map.entry("PROTECTION_ENVIRONMENTAL", "protection"),
            Map.entry("PROTECTION_FIRE", "fire_protection"),
            Map.entry("PROTECTION_FALL", "feather_falling"),
            Map.entry("PROTECTION_EXPLOSIONS", "blast_protection"),
            Map.entry("PROTECTION_PROJECTILE", "projectile_protection"),
            Map.entry("OXYGEN", "respiration"),
            Map.entry("WATER_WORKER", "aqua_affinity"),
            Map.entry("DAMAGE_ALL", "sharpness"),
            Map.entry("DAMAGE_UNDEAD", "smite"),
            Map.entry("DAMAGE_ARTHROPODS", "bane_of_arthropods"),
            Map.entry("LOOT_BONUS_MOBS", "looting"),
            Map.entry("SWEEPING_EDGE", "sweeping_edge"),
            Map.entry("SWEEPING", "sweeping_edge"),
            Map.entry("DIG_SPEED", "efficiency"),
            Map.entry("DURABILITY", "unbreaking"),
            Map.entry("LOOT_BONUS_BLOCKS", "fortune"),
            Map.entry("ARROW_DAMAGE", "power"),
            Map.entry("ARROW_KNOCKBACK", "punch"),
            Map.entry("ARROW_FIRE", "flame"),
            Map.entry("ARROW_INFINITE", "infinity"),
            Map.entry("LUCK", "luck_of_the_sea")
    );

    /**
     * Finds an enchantment by its key ("sharpness", "minecraft:sharpness", or a
     * custom "namespace:key") or by its old Bukkit name ("DAMAGE_ALL").
     * Returns null if there is no such enchantment.
     */
    public static Enchantment findEnchantment(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.strip();
        String key = LEGACY_ENCHANTS.getOrDefault(trimmed.toUpperCase(Locale.ROOT), trimmed.toLowerCase(Locale.ROOT));
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return null;
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(namespacedKey);
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
