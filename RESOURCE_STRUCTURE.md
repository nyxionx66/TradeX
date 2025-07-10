# ProTrades Resource Structure Documentation

## Overview
ProTrades has been completely restructured for better organization, performance, and maintainability. The new structure eliminates the confusing `/itemx/` directory and provides a clean, logical organization.

## New Directory Structure

```
/app/src/main/resources/
├── plugin.yml              # Plugin configuration with ptrades alias
├── config.yml               # Streamlined main configuration
├── configs/                 # All configuration files
│   ├── templates/           # Trade templates
│   │   ├── starter_templates.yml      # Beginner templates
│   │   ├── advanced_templates.yml     # Advanced templates
│   │   └── endgame_templates.yml      # Endgame templates
│   ├── trades/              # Individual trade configurations
│   └── example_trades.yml   # Example trade setups
└── items/                   # Custom item definitions
    ├── weapons/             # Weapon items
    │   └── legendary_weapons.yml
    ├── armor/               # Armor items
    │   └── dragon_armor.yml
    ├── tools/               # Tool items
    │   └── master_tools.yml
    └── misc/                # Miscellaneous items
        └── magical_items.yml
```

## Key Changes

### 1. Removed `/itemx/` Directory
- Old confusing structure eliminated
- Items now organized by type in `/items/`
- Templates moved to `/configs/templates/`

### 2. Added "ptrades" Alias
- `protrades` command now has `ptrades` alias
- Shorter command for convenience

### 3. Streamlined Configuration
- Single `config.yml` with all settings
- Cleaner template structure
- Better organization of trade configurations

### 4. Enhanced Examples
- Comprehensive example items across all categories
- Realistic trading scenarios
- Progressive difficulty levels (starter → advanced → endgame)

## Item Categories

### Weapons (`/items/weapons/`)
- **legendary_sword**: High-damage legendary weapon
- **flaming_blade**: Fire-based combat weapon
- **frost_blade**: Ice-based combat weapon
- **thunder_hammer**: Lightning-powered weapon

### Armor (`/items/armor/`)
- **dragon_helmet**: Fire-resistant helmet
- **dragon_chestplate**: Powerful chest armor
- **dragon_leggings**: Speed-enhancing leggings
- **dragon_boots**: Lava-walking boots

### Tools (`/items/tools/`)
- **miners_pickaxe**: Ultimate mining tool
- **forest_axe**: Tree-felling axe
- **excavator_shovel**: Area-digging shovel
- **farmers_hoe**: Advanced farming tool

### Miscellaneous (`/items/misc/`)
- **teleport_pearl**: Instant teleportation
- **healing_potion**: Powerful healing item
- **mana_crystal**: Magical energy storage
- **lucky_coin**: Luck enhancement
- **void_bag**: Unlimited storage

## Template Categories

### Starter Templates (`/configs/templates/starter_templates.yml`)
- **basic_weapon_upgrade**: Regular → Legendary weapons
- **armor_crafting**: Create dragon armor
- **tool_enhancement**: Upgrade tools
- **magical_item_trade**: Get magical items
- **healing_supplies**: Obtain healing potions

### Advanced Templates (`/configs/templates/advanced_templates.yml`)
- **legendary_weapon_fusion**: Combine legendary weapons
- **complete_armor_set**: Full dragon armor set
- **master_tool_collection**: Complete tool set
- **magical_artifact_trade**: Powerful magical items
- **ultimate_utility_trade**: Best utility items

### Endgame Templates (`/configs/templates/endgame_templates.yml`)
- **ultimate_ascension**: Transcendent power
- **perfect_equipment_set**: Ultimate equipment
- **master_collection_trade**: Complete collections
- **transcendent_power**: Admin-level items
- **infinite_resources**: Resource generation

## Code Changes

### ConfigManager
- Now handles new directory structure
- Supports both custom and example trades
- Enhanced validation and error handling
- Better performance with async operations

### ItemManager
- Loads items from categorized directories
- Improved caching and validation
- Better error handling and logging
- Support for complex item properties

### TradeTemplateManager
- Enhanced template parsing
- Better validation of template items
- Support for metadata and properties
- Improved category management

### ProTrades Main Class
- Streamlined initialization
- Better directory creation
- Enhanced validation system
- Comprehensive logging

## Usage Examples

### Commands
```
/protrades create starter_shop    # Create starter shop
/ptrades list                     # List all trades (using alias)
/trademgmt template starter       # Apply starter templates
/itemx give legendary_sword       # Give legendary sword
```

### Configuration
```yaml
# Example trade configuration
starter_shop:
  title: "&2&lStarter Shop"
  rows: 3
  trades:
    weapon_trade:
      input: ["IRON_SWORD:1", "EMERALD:5"]
      output: "legendary_sword:1"
```

## Migration Notes

### From Old Structure
- Old `/itemx/` files automatically ignored
- New structure loaded automatically
- No data loss - existing trades preserved
- Templates now more robust and reliable

### Benefits
- Cleaner organization
- Better performance
- Easier maintenance
- More comprehensive examples
- Enhanced error handling
- Better validation

## Best Practices

1. **Item Organization**: Keep items in appropriate categories
2. **Template Design**: Use progressive difficulty (starter → advanced → endgame)
3. **Validation**: Always validate items and templates after changes
4. **Testing**: Test trades before deploying to production
5. **Documentation**: Document custom items and templates

## Troubleshooting

### Common Issues
1. **Item Not Found**: Check item ID in appropriate category file
2. **Template Errors**: Validate template structure and item references
3. **Trade Not Working**: Verify all input/output items exist
4. **Performance Issues**: Check cache settings in config.yml

### Debug Commands
```
/ptnbt stats                      # Show NBT statistics
/trademgmt stats                  # Show template statistics
/itemx reload                     # Reload all items
/protrades reload                 # Reload entire plugin
```

## Future Enhancements

The new structure supports:
- Dynamic item creation
- Complex trade conditions
- Advanced template properties
- Better integration with other plugins
- Enhanced performance monitoring
- Automated testing capabilities

---

*This documentation covers the complete restructuring of ProTrades resource system. The new structure is more maintainable, performant, and user-friendly.*