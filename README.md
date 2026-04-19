# LofiBox

A configurable mystery box / crate plugin for Paper 1.21.x.

Players right-click a physical crate item to trigger a CSGO-style spin animation, then receive a randomly weighted reward — complete with custom actions, sounds, and fireworks.

---

## Features

- **YAML-driven crates** — define as many boxes as you want in `plugins/LofiBox/boxes/`
- **Weighted reward pools** — fine-grained control over drop chances
- **Permission-gated rewards** — rewards can require a LuckPerms node; re-rolls for ineligible players
- **7-tier key system** — optional per-box key requirement (Wooden → Netherite); admins bypass automatically
- **Vault economy cost** — optional per-box open cost charged via any Vault-compatible economy plugin
- **In-game admin editor** — full GUI to create, edit, and delete boxes without touching YAML
- **CSGO-style spin animation** — decelerating strip GUI that stops on the winner
- **Reward preview GUI** — paginated view of all rewards with chance percentages
- **Action system** — `[message]` `[actionbar]` `[title]` `[sound]` `[command]` `[console]` fire on win
- **HeadDatabase support** — specific heads by ID, random heads by HDB category, or custom search-term pools (e.g. `starwars`, `fantasy`) built from HDB's name/tag data
- **Custom item support** — base64 snapshot preserves full PDC data (MMOItems, Oraxen, etc.)
- **PlaceholderAPI** — expose opened-count stats to scoreboards and other plugins
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

---

## Installation

1. Drop `LofiBox.jar` into your `plugins/` folder.
2. Restart or reload the server.
3. Edit `plugins/LofiBox/boxes/example.yml` or create your own box files.
4. Edit `plugins/LofiBox/head-categories.yml` to define custom HeadDatabase search pools (optional).
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
| `/lofibox stats [player]` | `lofibox.use` | View open counts |
| `/lofibox reload` | `lofibox.reload` | Reload all configs and boxes |
| `/lofibox editor` | `lofibox.editor` | Open the in-game box editor GUI |

Aliases: `/mb`, `/crate`, `/lbox`

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `lofibox.admin` | op | Full access |
| `lofibox.use` | true | Open boxes and view previews |
| `lofibox.give` | op | Give box items |
| `lofibox.reload` | op | Reload configs |
| `lofibox.stats.others` | op | View another player's stats |
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

  # Random head from a custom search pool defined in head-categories.yml
  random_head:
    weight: 8
    display-name: "<green>Random Fantasy Head"
    item:
      head-database-category: "fantasy"
      material: PLAYER_HEAD
    actions:
      - "[message] <green>You received a random fantasy head!"
```

---

## Wiki

Full documentation is available on the [Wiki](https://github.com/Lord-Lofi/LofiBox/wiki).

---

## License

[GPL-3.0](LICENSE)
