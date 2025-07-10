# ===========================================
# ProTrades + ItemX Integration Guide
# ===========================================

# This file demonstrates the complete integration between ProTrades and ItemX
# with trade templates and dynamic trade creation.

# ===========================================
# 1. ITEMX ITEMS SETUP
# ===========================================

# Items are defined in: /itemx/items/
# Categories: weapons/, armor/, tools/, misc/

# Example: legendary_sword in weapons/example_sword.yml
legendary_sword:
  material: DIAMOND_SWORD
  name: "<gradient:#FF0000:#0000FF>Legendary Sword</gradient>"
  lore:
    - "&7A powerful weapon forged in"
    - "<gradient:#FFD700:#FF4500>ancient fires</gradient>"
  unbreakable: true
  enchants:
    SHARPNESS: 10
    FIRE_ASPECT: 3
  nbt-id: "legendary_sword"

# ===========================================
# 2. TRADE TEMPLATES SETUP
# ===========================================

# Templates are defined in: /itemx/templates/
# Categories: starter/, advanced/, endgame/

# Example template that uses ItemX items:
weapon_upgrade:
  name: "Weapon Upgrade Trade"
  description: "Trade regular sword for ItemX legendary sword"
  category: "starter"
  enabled: true
  inputs:
    sword:
      type: "regular"      # Regular Minecraft item
      item: "DIAMOND_SWORD"
      amount: 1
    emeralds:
      type: "regular"
      item: "EMERALD"
      amount: 5
  output:
    type: "itemx"         # ItemX custom item
    item: "legendary_sword"
    amount: 1

# ===========================================
# 3. COMMAND USAGE EXAMPLES
# ===========================================

# ITEMX COMMANDS:
# /itemx give legendary_sword PlayerName     - Give specific ItemX item
# /itemx get category:weapons               - Get all weapons
# /itemx reload                             - Reload ItemX config

# TRADE MANAGEMENT COMMANDS:
# /trademgmt create shop1                   - Open trade creation GUI for shop1
# /trademgmt template list                  - List all templates
# /trademgmt template list starter          - List starter templates
# /trademgmt template info weapon_upgrade   - Show template details
# /trademgmt apply weapon_upgrade shop1     - Apply template to shop1 GUI
# /trademgmt gui list                       - List all trade GUIs
# /trademgmt gui create newshop             - Create new trade GUI
# /trademgmt list shop1                     - List trades in shop1
# /trademgmt stats                          - Show comprehensive stats

# ===========================================
# 4. WORKFLOW EXAMPLES
# ===========================================

# SCENARIO 1: Setting up a new shop with ItemX items
# 1. Create trade GUI:        /protrades create weaponshop
# 2. Open trade creator:      /trademgmt create weaponshop
# 3. Use GUI to create trades with ItemX items
# 4. Or apply templates:      /trademgmt apply weapon_upgrade weaponshop

# SCENARIO 2: Using templates for quick setup
# 1. List available templates: /trademgmt template list starter
# 2. Check template details:   /trademgmt template info weapon_upgrade
# 3. Apply to existing GUI:    /trademgmt apply weapon_upgrade myshop
# 4. Open shop for players:    /trade myshop

# SCENARIO 3: Creating custom ItemX items for trades
# 1. Create item in YAML:     /itemx/items/weapons/custom_blade.yml
# 2. Reload items:            /itemx reload
# 3. Test item:               /itemx give custom_blade YourName
# 4. Use in trade creator:    /trademgmt create shop1 (then place in GUI)

# ===========================================
# 5. INTEGRATION FEATURES
# ===========================================

# ✅ ItemX items work seamlessly in all trades
# ✅ NBT data is preserved during trading
# ✅ Templates can mix ItemX and regular items
# ✅ Dynamic GUI creation with drag-and-drop
# ✅ Category-based item organization
# ✅ Full color support (MiniMessage, legacy, hex)
# ✅ Armor trim support for 1.20+
# ✅ Unsafe enchantments
# ✅ Usage control with disable-use option
# ✅ Full tab completion
# ✅ Comprehensive statistics

# ===========================================
# 6. PERMISSIONS
# ===========================================

# protrades.admin    - Full access to all features
# protrades.trade    - Access to trade GUIs
# itemx.give         - Give ItemX items (admin)
# itemx.get          - Get ItemX categories (player)
# itemx.reload       - Reload ItemX config (admin)

# ===========================================
# 7. CONFIGURATION FILES
# ===========================================

# Main configs:
# - /config.yml                    - ProTrades main config
# - /itemx-config.yml             - ItemX configuration

# Items:
# - /itemx/items/weapons/         - Weapon items
# - /itemx/items/armor/           - Armor items  
# - /itemx/items/tools/           - Tool items
# - /itemx/items/misc/            - Miscellaneous items

# Templates:
# - /itemx/templates/starter/     - Beginner templates
# - /itemx/templates/advanced/    - Advanced templates
# - /itemx/templates/endgame/     - Endgame templates

# Trades:
# - /trades/*.yml                 - Trade GUI configurations

# ===========================================
# This integration provides a complete
# custom item and advanced trading system
# for your Minecraft server!
# ===========================================