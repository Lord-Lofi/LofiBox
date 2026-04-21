# LofiBox

A configurable mystery box / crate plugin for Paper 1.21.x.

Players right-click a physical crate item to trigger a CSGO-style spin animation, then receive a randomly weighted reward — complete with custom actions, sounds, and fireworks.

---

## Features

- **YAML-driven crates** — define as many boxes as you want in `plugins/LofiBox/boxes/`
- **33 default lootboxes** — one per head category, ready to use out of the box
- **Weighted reward pools** — fine-grained control over drop chances
- **Permission-gated rewards** — rewards can require a LuckPerms node; re-rolls for ineligible players
- **7-tier key system** — optional per-box key requirement (Wooden → Netherite); admins bypass automatically
- **Vault economy cost** — optional per-box open cost charged via any Vault-compatible economy plugin
- **In-game admin editor** — full GUI to create, edit, and delete boxes without touching YAML
- **CSGO-style spin animation** — decelerating strip GUI that stops on the winner
- **Reward preview GUI** — paginated view of all rewards with chance percentages
- **Action system** — `[message]` `[actionbar]` `[title]` `[sound]` `[command]` `[console]` fire on win
- **HeadDatabase support** — specific heads by ID, random heads by HDB category, or 26 custom search-term pools (e.g. `halloween`, `anime`, `pokemon`) built from HDB's name/tag data
- **Seasonal availability windows** — head categories go live and expire on configurable dates; year-wrap (Dec–Jan) supported
- **Double-reward mechanic** — per-category bonus head chance, optionally scoped to a date range (e.g. 50% on Pokémon Day)
- **Pending rewards system** — full-inventory rewards saved to disk; player redeems via `/lofibox redeem` GUI; EssX mail notification sent automatically
- **Server broadcast & Discord announce** — independent toggles: broadcast head finds to chat and/or post to EssentialsXDiscord
- **Player stats GUI** — paginated inventory showing each player's per-box open counts; entry-click opens the reward preview
- **NPC integration** — console-triggered stats command opens the GUI directly on a player's screen (Citizens, etc.)
- **Public developer API** — `LofiBoxAPI` service for reading box metadata and seasonal state from other plugins
- **Custom item support** — base64 snapshot preserves full PDC data (MMOItems, Oraxen, etc.)
- **PlaceholderAPI** — expose opened-count stats to scoreboards and other plugins
- **Automatic config migration** — new keys merged into existing files on every upgrade; no manual editing required
- **MiniMessage formatting** — gradients, colors, and hover text everywhere
- **Hot reload** — `/lofibox reload` with no server restart

---

## Requirements

| Dependency | Required |
|---|---|
| Paper 1.21.x | ✅ Yes |
| PlaceholderAPI | ❌ Optional |
| HeadDatabase | ❌ Optional |
| LuckPerms | ❌ Optional (uses standard Bukkit permissions) |
| Vault + economy plugin | ❌ Optional (required for open-cost) |
| EssentialsX | ❌ Optional (EssX mail notifications for pending rewards) |
| EssentialsXDiscord | ❌ Optional (required for Discord announce) |

---

## Installation

1. Drop `LofiBox.jar` into your `plugins/` folder.
2. Restart or reload the server.
3. Edit `plugins/LofiBox/boxes/example.yml` or use `/lofibox editor` to create boxes in-game.
4. Edit `plugins/LofiBox/head-categories.yml` to configure seasonal windows and double-reward chances (optional).
5. Run `/lofibox reload` to apply changes without restarting.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/lofibox give <box> <player> [amount]` | `lofibox.give` | Give a box item to a player |
| `/lofibox givekey <tier> <player> [amount]` | `lofibox.give` | Give a key item to a player |
| `/lofibox open <box>` | `lofibox.use` | Open a box (admin bypass — no item or key consumed) |
| `/lofibox preview <box>` | `lofibox.use` | Preview all rewards and chances |
| `/lofibox list` | `lofibox.use` | List all loaded boxes |
| `/lofibox stats [player]` | `lofibox.stats` | Open the stats GUI for yourself or another player |
| `/lofibox redeem` | `lofibox.redeem` | Open the pending rewards GUI to claim full-inventory items |
| `/lofibox reload` | `lofibox.reload` | Reload all configs and boxes |
| `/lofibox editor` | `lofibox.editor` | Open the in-game box editor GUI |

Aliases: `/mb`, `/crate`, `/lbox`

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `lofibox.admin` | op | Full access |
| `lofibox.use` | true | Open boxes and view previews |
| `lofibox.give` | op | Give box and key items |
| `lofibox.reload` | op | Reload configs |
| `lofibox.stats` | false | View own stats GUI |
| `lofibox.stats.others` | op | View another player's stats; console opens GUI on target's screen |
| `lofibox.redeem` | true | Open the pending rewards GUI |
| `lofibox.bypass` | op | Bypass permission-gated rewards |
| `lofibox.editor` | op | Open the in-game box editor GUI |

---

## PlaceholderAPI

| Placeholder | Description |
|---|---|
| `%lofibox_total_opened%` | Total boxes opened by the player |
| `%lofibox_opened_<boxId>%` | Boxes of a specific type opened by the player |

---

## Quick Box Example

```yaml
# plugins/LofiBox/boxes/mythic.yml
name: "<gradient:#a78bfa:#6366f1>Mythic Crate</gradient>"

item:
  material: CHEST
  name: "<gradient:#a78bfa:#6366f1>✦ Mythic Crate ✦</gradient>"
  lore:
    - ""
    - "<gray>Right-click to open!"

sounds:
  open: "block.chest.open"
  win: "entity.player.levelup"

rewards:
  diamonds:
    weight: 40
    display-name: "<aqua>Diamonds x5"
    item:
      material: DIAMOND
      amount: 5
    actions:
      - "[message] <aqua>You received 5 Diamonds!"

  netherite:
    weight: 5
    display-name: "<dark_purple>Netherite Ingot"
    item:
      material: NETHERITE_INGOT
    actions:
      - "[title] <dark_purple>EPIC!;<gray>Netherite Ingot"
      - "[console] eco give {player} 1000"

  # Seasonal Halloween head (active Oct 1 – Nov 15, 5% double-reward chance)
  halloween_head:
    weight: 8
    display-name: "<gold>Random Halloween Head"
    item:
      head-database-category: "halloween"
      material: CARVED_PUMPKIN
    actions:
      - "[message] <gold>You received a spooky head!"
```

---

## Developer API

Other plugins can read LofiBox's box metadata and seasonal state via the `LofiBoxAPI` service:

```java
LofiBoxAPI api = LofiBoxAPI.get();
if (api == null) return; // LofiBox not loaded

// Gate a shop listing on seasonal availability
if (api.isBoxAvailableNow("halloween")) {
    shop.listItem("halloween");
}

// Display the active season window to a player
LofiBoxAPI.SeasonWindow window = api.getBoxPrimarySeasonWindow("halloween");
if (window != null) {
    player.sendMessage("Available: " + window.toDisplayString()); // "Oct 1 – Nov 15"
}
```

See the [Developer API wiki page](https://github.com/Lord-Lofi/LofiBox/wiki/Developer-API) for full documentation.

---

## Wiki

Full documentation is available on the [Wiki](https://github.com/Lord-Lofi/LofiBox/wiki).

---

## License

[GPL-3.0](LICENSE)
