package com.shopsalevalue;

import java.awt.*;
import java.util.TreeSet;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;

public class ShopSaleValueOverlay extends OverlayPanel
{
    private final ShopSaleValuePlugin plugin;
    private final ShopSaleValueConfig config;

    @Inject ShopSaleValueOverlay(ShopSaleValuePlugin plugin, ShopSaleValueConfig config)
    {
        super(plugin);
        this.plugin = plugin; this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(310, 0));
    }

    private void line(String left, String right)
    {
        panelComponent.getChildren().add(LineComponent.builder().left(left).right(right).build());
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        if (!config.panel() || !plugin.shopOpen()) return null;
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text("Shop Sale Value").color(new Color(128,255,128)).build());
        Quote q = plugin.selectedQuote();
        if (q == null)
        {
            line("Hover an inventory item", "");
            return super.render(graphics);
        }
        line(q.name, "");
        line(q.shop, "");
        if (q.problem != null)
        {
            line(q.problem, "");
            return super.render(graphics);
        }
        line(q.ruleName, "");
        line("Stock / normal", q.stock + " / " + q.baseline);
        line("Next item", String.format("%,d gp", q.first()));
        TreeSet<Integer> amounts = new TreeSet<>();
        for (int amount : new int[]{1,5,10,50,config.customQuantity(),q.available}) amounts.add(q.quantity(amount));
        for (int amount : amounts) if (amount > 0) line("Sell " + amount, q.price(amount));
        int batch = q.quantity(Math.max(1,config.customQuantity()));
        long fresh = Pricing.total(q.value, 0, batch, q.rule);
        line("Fresh stock, sell " + batch, String.format("%,d gp", fresh));
        line("Extra after hopping*", String.format("%,d gp", fresh - q.total(batch)));
        line("*Assumes normal stock on next world", "");
        line("Estimates; no restocks during sale", "");
        return super.render(graphics);
    }
}
