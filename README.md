# OakVillagers

Keeps villagers from ever paying out emeralds, rerolling those trades from the vanilla trade pool instead of leaving villagers empty.

## Features

- Blocks villager and wandering trader trades that pay the player a configured item (emeralds by default) as they are rolled, and refuses any that still reach the trade screen
- Rerolls the gap from the villager's own vanilla trade set, so a villager still ends up with its usual 2 trades per level as far as its pool allows (plain removal leaves farmers, shepherds and butchers with no trades more than half the time)
- Heals existing villagers when a player right-clicks them: blocked trades are removed and the villager is topped up once for its level, so villagers left empty before this plugin fix themselves
- Leaves Shopkeepers shops and NPCs alone
- Per-world opt-out

## Requirements

- Paper 26.3
- Java 25

## Installation

1. Drop the JAR in `plugins/`
2. Remove TradeManager or any other plugin that blocks the same trades
3. Restart the server

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/oakvillagers reload` | Reload configuration | `oakvillagers.reload` |

## Permissions

| Node | Description | Default |
|------|-------------|---------|
| `oakvillagers.*` | All permissions | op |
| `oakvillagers.reload` | Reload configuration | op |

## Configuration

- `blocked-results`: items villagers may never give (`EMERALD`).
- `disabled-worlds`: worlds left alone.
- `wandering-traders`: also block on wandering traders (not rerolled).
- `reroll`: top-up target (`trades-per-level`) and the roll budget per top-up (`max-attempts`).
- `heal-on-interact`: fix villagers when a player right-clicks them.
