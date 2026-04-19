package dev.lofibox.key;

import dev.lofibox.LofiBox;
import dev.lofibox.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and identifies the seven physical key items.
 * Keys are tripwire hooks tagged with lofibox:key_tier in PDC.
 * A dummy Unbreaking I enchant + HIDE_ENCHANTS gives the enchanted glow.
 */
public final class KeyManager {

    private final NamespacedKey keyTierKey;

    public KeyManager(LofiBox plugin) {
        this.keyTierKey = new NamespacedKey(plugin, "key_tier");
    }

    // ── Item creation ─────────────────────────────────────────────────────────

    public ItemStack createKey(KeyTier tier) {
        return createKey(tier, 1);
    }

    public ItemStack createKey(KeyTier tier, int amount) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, amount));
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;

        // Display name
        meta.displayName(MessageUtil.parse(tier.getDisplayName()));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<dark_gray>" + tier.getTierLabel()));
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<gray>Use this key to open"));
        lore.add(MessageUtil.parse(tier.getColor() + tier.name().charAt(0)
                + tier.name().substring(1).toLowerCase() + " <gray>crates."));
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<dark_gray><i>" + tier.getFlavourText()));
        meta.lore(lore);

        // Glow effect — hidden enchant
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );

        // Custom model data for resource pack overrides
        meta.setCustomModelData(tier.getCustomModelData());

        // PDC tag — used to identify and match keys
        meta.getPersistentDataContainer().set(keyTierKey, PersistentDataType.STRING, tier.name());

        item.setItemMeta(meta);
        return item;
    }

    // ── Key identification ────────────────────────────────────────────────────

    /**
     * Returns the KeyTier of the given item, or null if it is not a LofiBox key.
     */
    public KeyTier getKeyTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta()
                .getPersistentDataContainer()
                .get(keyTierKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return KeyTier.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isKey(ItemStack item) {
        return getKeyTier(item) != null;
    }

    public NamespacedKey getKeyTierKey() { return keyTierKey; }
}
