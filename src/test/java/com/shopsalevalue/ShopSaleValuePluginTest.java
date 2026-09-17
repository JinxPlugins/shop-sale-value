package com.shopsalevalue;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShopSaleValuePluginTest
{
    ShopSaleValuePlugin plugin;
    Widget shopItem;
    ShopSaleValueConfig config;
    @Before public void setup()
    {
        plugin = new ShopSaleValuePlugin();
        plugin.client = mock(Client.class); plugin.items = mock(ItemManager.class);
        plugin.overlays = mock(OverlayManager.class); plugin.gson = new Gson();
        config = new ShopSaleValueConfig() {};
        plugin.config = config;
        when(plugin.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        Widget shop = mock(Widget.class), frame = mock(Widget.class), title = mock(Widget.class);
        when(plugin.client.getWidget(InterfaceID.Shopmain.ITEMS)).thenReturn(shop);
        when(plugin.client.getWidget(InterfaceID.Shopmain.FRAME)).thenReturn(frame);
        when(frame.getChild(1)).thenReturn(title);
        when(title.getText()).thenReturn("Lumbridge General Store");
        shopItem = mock(Widget.class);
        when(shopItem.getItemId()).thenReturn(54);
        when(shopItem.getItemQuantity()).thenReturn(0);
        when(shop.getChildren()).thenReturn(new Widget[]{shopItem});
        ItemComposition bow = mock(ItemComposition.class);
        when(bow.getId()).thenReturn(54); when(bow.getName()).thenReturn("Oak shortbow (u)");
        when(bow.getPrice()).thenReturn(50); when(bow.getNote()).thenReturn(-1); when(bow.isTradeable()).thenReturn(true);
        when(plugin.items.getItemComposition(54)).thenReturn(bow);
        ItemComposition note = mock(ItemComposition.class);
        when(note.getNote()).thenReturn(799); when(note.getLinkedNoteId()).thenReturn(54);
        when(plugin.items.getItemComposition(55)).thenReturn(note);
        ItemContainer inv = mock(ItemContainer.class);
        when(inv.getItems()).thenReturn(new Item[]{new Item(55,28)});
        when(plugin.client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        plugin.startUp();
    }
    private MenuEntry entry(String option)
    {
        MenuEntry entry = mock(MenuEntry.class);
        AtomicReference<String> target = new AtomicReference<>("<col=ff9040>Oak shortbow (u)</col>");
        when(entry.getTarget()).thenAnswer(x->target.get());
        when(entry.setTarget(anyString())).thenAnswer(x->{target.set(x.getArgument(0));return entry;});
        when(entry.getParam1()).thenReturn(InterfaceID.Shopside.ITEMS);
        when(entry.getItemId()).thenReturn(55);
        when(entry.getOption()).thenReturn(option);
        return entry;
    }
    @Test public void menuTotalsCapToInventoryAndDoNotChangeAction()
    {
        MenuEntry entry = entry("Sell 50");
        plugin.onMenuEntryAdded(new MenuEntryAdded(entry));
        assertTrue(entry.getTarget().contains("220 gp [7.86 ea] (28 items)"));
        verify(entry,never()).setOption(anyString());
        verify(entry,never()).setIdentifier(anyInt());
        verify(entry,never()).setType(any());
    }
    @Test public void stockUpdatesInvalidateQuotesAndOpenMenus()
    {
        MenuEntry entry = entry("Sell 5");
        plugin.onMenuEntryAdded(new MenuEntryAdded(entry));
        assertTrue(entry.getTarget().contains("84 gp"));
        when(shopItem.getItemQuantity()).thenReturn(5);
        when(plugin.client.isMenuOpen()).thenReturn(true);
        when(plugin.client.getMenuEntries()).thenReturn(new MenuEntry[]{entry});
        plugin.onGameTick(new GameTick()); plugin.onClientTick(new ClientTick());
        assertTrue(entry.getTarget().contains("46 gp"));
        assertEquals(1,entry.getTarget().split(" \\| ",-1).length-1);
    }
    @Test public void sellXStaysUnpriced()
    {
        MenuEntry entry = entry("Sell X"); plugin.onMenuEntryAdded(new MenuEntryAdded(entry));
        assertFalse(entry.getTarget().contains("gp"));
    }
    @Test public void customFixedRateIsScopedToMatchingTitle()
    {
        plugin.config = new ShopSaleValueConfig() {
            public String overrideTitle(){return "Lumbridge General Store";}
            public int buyPercent(){return 60;}
            public int decreaseBps(){return 0;}
        };
        when(shopItem.getItemQuantity()).thenReturn(999);
        assertEquals(150,plugin.quote(55).total(5));
    }
    @Test public void logoutRemovesSelectedItem()
    {
        plugin.onMenuEntryAdded(new MenuEntryAdded(entry("Value")));
        assertNotNull(plugin.selectedQuote());
        GameStateChanged event = new GameStateChanged(); event.setGameState(GameState.HOPPING);
        plugin.onGameStateChanged(event);
        assertNull(plugin.selectedQuote());
    }
    @Test public void renderComparisonOverlay() throws Exception
    {
        plugin.onMenuEntryAdded(new MenuEntryAdded(entry("Value")));
        assertNotNull(plugin.selectedQuote());
        assertNull(plugin.selectedQuote().problem);
        ShopSaleValueOverlay overlay = new ShopSaleValueOverlay(plugin, config);
        BufferedImage image = new BufferedImage(350,400,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(new Font("Arial",Font.PLAIN,12));
        // RuneLite's PanelComponent reports the previous frame's cached dimensions.
        overlay.render(graphics);
        Dimension size = overlay.render(graphics);
        graphics.dispose();
        assertNotNull(size);
        assertTrue(size.width <= 350 && size.height <= 400);
        ImageIO.write(image.getSubimage(0,0,size.width,size.height),"png",new File("build/overlay-preview.png"));
        assertTrue("Rendered size: " + size, size.height > 150);
    }
}
