# SimpleDexRewards

A comprehensive Pokedex reward system for Cobblemon 1.6+ running on Minecraft 1.21.1 with Fabric. Provides configurable rewards for Pokedex completion milestones, supporting multiple reward types including items, Pokemon, and custom commands!

## Installation and Dependencies

This mod requires the following dependencies:
- [Cobblemon](https://modrinth.com/mod/cobblemon) (1.6+)
- [GooeyLibs](https://modrinth.com/mod/gooeylibs)

GooeyLibs is only needed server-side.

## Features

### Reward System
- Multiple reward types (Items, Pokemon, Commands)
- Configurable completion tiers with trainer ranks
- Interactive UI with hover text showing all rewards
- Direct reward claiming through UI
- Server-side functionality with client UI
- Dynamic configuration reloading without server restarts

### Reward Types
- `ITEM`: Any item from any mod
- `POKEMON`: Customizable Pokemon rewards (species, level, shiny status)
- `COMMAND`: Custom command execution with placeholders

## Configuration

### rewardconfig.json
```json
{
  "enablePermissionNodes": true,
  "completionTiers": [10, 20, 30, 40, 50, 60, 70, 80, 90, 100],
  "rewards": {
    "100": {
      "row": 5,             // Position of the reward tier on the UI grid
      "slot": 7,            // Position of the reward tier on the UI grid
      "rewards": [          // All actual rewards for that tier
        {
          "type": "ITEM",
          "data": {
            "id": "minecraft:nether_star",
            "Count": 1
          }
        },
        {
          "type": "POKEMON",
          "data": {
            "species": "Rayquaza",
            "shiny": true,
            "level": 50
          }
        },
        {
          "type": "COMMAND",
          "data": {
            "id": "minecraft:paper",
            "display_name": "Totem of Undying"
          },
          "command": "give @p minecraft:totem_of_undying 1"      //You can also use %player% for custom commands like assigning ranks
        }
      ],
      "display": {
        "type": "tier_100",
        "format": "Master Trainer",               // Name of the Tier
        "item": "cobblemon:master_ball"           // UI element displayed of that Tier
      }
    }
  }
}
```

### Player Data
```json
{
  "version": "1.0",
  "claimedRewards": [25],           // Claimed completion tiers
  "caughtPokemon": ["Pikachu", "Charmander"],  // Tracked Pokemon
  "settings": {
    "notifications": true
  }
}
```

## Commands

### Player Commands
- `/dexrewards` - Open the rewards UI

### Admin Commands
- `/dexrewards reload` - Reload configuration files


## UI Layout

The rewards interface is organized in a grid layout where:
- Tiers are arranged in rows (1-5) and slots (1-9)
- Each tier displays its custom name and completion percentage
- Hovering over a tier shows all available rewards
- The display item for each tier can be customized
- Claimed rewards are visually marked

## Permissions
- `cobblemonpokedex.use` - Access to player commands
- `cobblemonpokedex.admin` - Access to admin commands


