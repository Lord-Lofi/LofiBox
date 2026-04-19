package dev.lofibox.key;

/**
 * The seven key tiers, ordered from lowest to highest.
 * Each maps to a physical tripwire hook item with unique styling.
 */
public enum KeyTier {

    WOODEN(
        1001,
        "<#8B6914>Wooden Key",
        "<#8B6914>",
        "Tier I",
        "A worn key carved from old wood."
    ),
    STONE(
        1002,
        "<#8a8a8a>Stone Key",
        "<#8a8a8a>",
        "Tier II",
        "A rough key chipped from grey stone."
    ),
    COPPER(
        1003,
        "<#b87333>Copper Key",
        "<#b87333>",
        "Tier III",
        "A tarnished key cast from copper."
    ),
    IRON(
        1004,
        "<gray>Iron Key",
        "<gray>",
        "Tier IV",
        "A sturdy key forged from iron."
    ),
    GOLDEN(
        1005,
        "<gold>Golden Key",
        "<gold>",
        "Tier V",
        "A gleaming key cast from pure gold."
    ),
    DIAMOND(
        1006,
        "<aqua>Diamond Key",
        "<aqua>",
        "Tier VI",
        "A flawless key cut from diamond."
    ),
    NETHERITE(
        1007,
        "<gradient:#9d4dff:#ff4dd2>Netherite Key</gradient>",
        "<dark_purple>",
        "Tier VII",
        "A legendary key forged in the Nether."
    );

    private final int customModelData;
    private final String displayName;
    private final String color;
    private final String tierLabel;
    private final String flavourText;

    KeyTier(int customModelData, String displayName, String color, String tierLabel, String flavourText) {
        this.customModelData = customModelData;
        this.displayName     = displayName;
        this.color           = color;
        this.tierLabel       = tierLabel;
        this.flavourText     = flavourText;
    }

    public int getCustomModelData() { return customModelData; }
    public String getDisplayName()  { return displayName; }
    public String getColor()        { return color; }
    public String getTierLabel()    { return tierLabel; }
    public String getFlavourText()  { return flavourText; }

    /** Returns the tier one step above this one, or null if already at the top. */
    public KeyTier next() {
        KeyTier[] values = values();
        int idx = ordinal() + 1;
        return idx < values.length ? values[idx] : null;
    }

    /** Returns the tier one step below this one, or null if already at the bottom. */
    public KeyTier previous() {
        int idx = ordinal() - 1;
        return idx >= 0 ? values()[idx] : null;
    }
}
