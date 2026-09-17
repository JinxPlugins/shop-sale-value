package com.shopsalevalue;

import net.runelite.client.config.*;

@ConfigGroup("shopsalevalue")
public interface ShopSaleValueConfig extends Config
{
    @ConfigItem(keyName="menu", name="Show menu prices", description="Add total GP and average GP each to Sell options", position=0)
    default boolean menu() { return true; }

    @ConfigItem(keyName="panel", name="Show item prices", description="Show a white next-sale price above each sellable inventory item while a shop is open", position=1)
    default boolean panel() { return true; }

    @ConfigItem(keyName="customQuantity", name="Comparison quantity", description="Additional amount in the overlay; does not change Sell X", position=2)
    @Range(min=1, max=2147483647)
    default int customQuantity() { return 20; }

    @ConfigItem(keyName="ctrlLookup", name="Ctrl+click price lookup", description="Hold Ctrl and click an inventory item for a local chat estimate at a normal general store. Replaces the item's normal click action.", position=3)
    default boolean ctrlLookup() { return true; }

    @ConfigItem(keyName="overrideTitle", name="Custom shop title", description="Exact shop title to override. Leave blank for automatic detection. Custom rules apply only to this title.", position=10)
    default String overrideTitle() { return ""; }

    @ConfigItem(keyName="buyPercent", name="Custom buy percent", description="Percent of item value paid at normal stock: 40 for normal, 60 for high alch", position=11)
    @Range(min=0,max=100)
    default int buyPercent() { return 40; }

    @ConfigItem(keyName="decreaseBps", name="Custom drop (0.01%)", description="300 = 3%, 30 = 0.3%, 0 = fixed price regardless of stock", position=12)
    @Range(min=0,max=10000)
    default int decreaseBps() { return 300; }

    @ConfigItem(keyName="floorPercent", name="Custom minimum percent", description="Minimum percentage of item value, usually 10", position=13)
    @Range(min=0,max=100)
    default int floorPercent() { return 10; }

    @ConfigItem(keyName="baseline", name="Custom normal stock", description="Baseline for the item being evaluated. Applies to all items in this custom shop; normally 0 for your crafted items.", position=14)
    @Range(min=0,max=2147483647)
    default int baseline() { return 0; }
}
