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
    private void openWithCurrency(int currency)
    {
        ScriptEvent script = mock(ScriptEvent.class);
        when(script.getArguments()).thenReturn(new Object[]{1074, 4, "Lumbridge General Store", currency, 1, false});
        ScriptPreFired event = mock(ScriptPreFired.class);
        when(event.getScriptId()).thenReturn(1074);
        when(event.getScriptEvent()).thenReturn(script);
        plugin.onScriptPreFired(event);
    }
    @Test public void defaultCurrencyAllowsMenuAndItemLabel()
    {
        openWithCurrency(-1);
        MenuEntry menu = entry("Sell 5");
        plugin.onMenuEntryAdded(new MenuEntryAdded(menu));
        assertTrue(menu.getTarget().contains("84 gp [16 avg ea]"));
        assertNull(plugin.quote(55).problem);
        net.runelite.api.widgets.WidgetItem widgetItem = mock(net.runelite.api.widgets.WidgetItem.class);
        when(widgetItem.getCanvasBounds()).thenReturn(new Rectangle(4,4,32,32));
        Graphics2D graphics = mock(Graphics2D.class);
        when(graphics.create()).thenReturn(graphics);
        new ShopSaleValueOverlay(plugin, config).renderItemOverlay(graphics, 55, widgetItem);
        verify(graphics).setColor(Color.WHITE);
        verify(graphics).drawString("20gp", 4, 13);
    }
    @Test public void explicitNonCoinCurrencyRemainsUnsupported()
    {
        openWithCurrency(6529);
        assertEquals("Non-coin shop is not supported", plugin.quote(55).problem);
        MenuEntry menu = entry("Sell 5");
        plugin.onMenuEntryAdded(new MenuEntryAdded(menu));
        assertFalse(menu.getTarget().contains("gp"));
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
    @Test public void controlLookupConsumesActionAndPricesNotesWithoutShop()
    {
        when(plugin.client.getWidget(InterfaceID.Shopmain.ITEMS)).thenReturn(null);
        when(plugin.client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
        MenuOptionClicked event = mock(MenuOptionClicked.class);
        when(event.getParam1()).thenReturn(net.runelite.api.widgets.WidgetInfo.INVENTORY.getId());
        when(event.getItemId()).thenReturn(55);
        plugin.onMenuOptionClicked(event);
        verify(event).consume();
        verify(plugin.client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""),
            contains("Oak shortbow (u): normal general store, no excess stock: 20 gp each initially (40%); minimum 5 gp each"), isNull());
    }
    @Test public void ordinaryClickIsUnchanged()
    {
        MenuOptionClicked event = mock(MenuOptionClicked.class);
        when(event.getParam1()).thenReturn(net.runelite.api.widgets.WidgetInfo.INVENTORY.getId());
        when(event.getItemId()).thenReturn(55);
        plugin.onMenuOptionClicked(event);
        verify(event, never()).consume();
        verify(plugin.client, never()).addChatMessage(any(), anyString(), anyString(), any());
    }
    @Test public void menuTotalsCapToInventoryAndDoNotChangeAction()
    {
        MenuEntry entry = entry("Sell 50");
        plugin.onMenuEntryAdded(new MenuEntryAdded(entry));
        assertTrue(entry.getTarget().contains("220 gp [7 avg ea] (28 items)"));
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
    @Test public void emptyShopSlotsDoNotBlockMenuPrices()
    {
        Widget[] slots = new Widget[40];
        slots[0] = shopItem;
        for (int i = 1; i < slots.length; i++)
        {
            slots[i] = mock(Widget.class);
            when(slots[i].getItemId()).thenReturn(0);
            when(slots[i].getItemQuantity()).thenReturn(0);
        }
        when(plugin.client.getWidget(InterfaceID.Shopmain.ITEMS).getChildren()).thenReturn(slots);
        MenuEntry entry = entry("Sell 5");
        plugin.onMenuEntryAdded(new MenuEntryAdded(entry));
        assertNull(plugin.quote(55).problem);
        assertTrue(entry.getTarget().contains("84 gp [16 avg ea]"));
    }
}
