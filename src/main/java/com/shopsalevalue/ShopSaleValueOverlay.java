package com.shopsalevalue;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class ShopSaleValueOverlay extends WidgetItemOverlay
{
    private final ShopSaleValuePlugin plugin;
    private final ShopSaleValueConfig config;

    @Inject ShopSaleValueOverlay(ShopSaleValuePlugin plugin, ShopSaleValueConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        showOnInterfaces(InterfaceID.SHOPSIDE);
    }

    @Override public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
    {
        if (!config.panel() || !plugin.shopOpen()) return;
        Quote quote = plugin.quote(itemId);
        if (quote == null || quote.problem != null) return;
        Rectangle bounds = item.getCanvasBounds();
        String label = quote.first() + "gp";
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setFont(FontManager.getRunescapeSmallFont());
            int x = bounds.x;
            int y = bounds.y + 9;
            // Leave the stack count readable on noted/stackable items.
            if (item.getQuantity() > 1) y += 10;
            g.setColor(Color.BLACK);
            g.drawString(label, x + 1, y + 1);
            g.setColor(Color.WHITE);
            g.drawString(label, x, y);
        }
        finally { g.dispose(); }
    }
}
