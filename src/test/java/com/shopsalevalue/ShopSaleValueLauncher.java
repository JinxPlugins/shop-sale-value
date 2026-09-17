package com.shopsalevalue;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShopSaleValueLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ShopSaleValuePlugin.class);
        RuneLite.main(args);
    }
}
