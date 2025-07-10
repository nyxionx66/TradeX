#!/bin/bash

echo "=== ProTrades Resource Structure Verification ==="
echo

# Check if old itemx directory was removed
if [ -d "/app/src/main/resources/itemx" ]; then
    echo "❌ Old itemx directory still exists - should be removed"
else
    echo "✅ Old itemx directory successfully removed"
fi

# Check new directory structure
echo
echo "=== Directory Structure Check ==="

directories=(
    "/app/src/main/resources/configs"
    "/app/src/main/resources/configs/templates"
    "/app/src/main/resources/items"
    "/app/src/main/resources/items/weapons"
    "/app/src/main/resources/items/armor"
    "/app/src/main/resources/items/tools"
    "/app/src/main/resources/items/misc"
)

for dir in "${directories[@]}"; do
    if [ -d "$dir" ]; then
        echo "✅ $dir exists"
    else
        echo "❌ $dir missing"
    fi
done

echo
echo "=== Configuration Files Check ==="

config_files=(
    "/app/src/main/resources/config.yml"
    "/app/src/main/resources/plugin.yml"
    "/app/src/main/resources/configs/templates/starter_templates.yml"
    "/app/src/main/resources/configs/templates/advanced_templates.yml"
    "/app/src/main/resources/configs/templates/endgame_templates.yml"
    "/app/src/main/resources/configs/example_trades.yml"
)

for file in "${config_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file missing"
    fi
done

echo
echo "=== Item Files Check ==="

item_files=(
    "/app/src/main/resources/items/weapons/legendary_weapons.yml"
    "/app/src/main/resources/items/armor/dragon_armor.yml"
    "/app/src/main/resources/items/tools/master_tools.yml"
    "/app/src/main/resources/items/misc/magical_items.yml"
)

for file in "${item_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file missing"
    fi
done

echo
echo "=== Plugin.yml Alias Check ==="

if grep -q "ptrades" /app/src/main/resources/plugin.yml; then
    echo "✅ ptrades alias found in plugin.yml"
else
    echo "❌ ptrades alias missing from plugin.yml"
fi

echo
echo "=== Java Files Check ==="

java_files=(
    "/app/src/main/java/org/mindle/protrades/ProTrades.java"
    "/app/src/main/java/org/mindle/protrades/managers/ConfigManager.java"
    "/app/src/main/java/org/mindle/protrades/itemx/ItemManager.java"
    "/app/src/main/java/org/mindle/protrades/itemx/templates/TradeTemplateManager.java"
)

for file in "${java_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file missing"
    fi
done

echo
echo "=== File Count Summary ==="
echo "Weapon items: $(find /app/src/main/resources/items/weapons -name "*.yml" | wc -l)"
echo "Armor items: $(find /app/src/main/resources/items/armor -name "*.yml" | wc -l)"
echo "Tool items: $(find /app/src/main/resources/items/tools -name "*.yml" | wc -l)"
echo "Misc items: $(find /app/src/main/resources/items/misc -name "*.yml" | wc -l)"
echo "Template files: $(find /app/src/main/resources/configs/templates -name "*.yml" | wc -l)"

echo
echo "=== Verification Complete ==="