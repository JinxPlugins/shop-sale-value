package com.shopsalevalue;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(name="Shop Sale Value", description="Live shop sale totals and GP per item", tags={"shop", "ironman", "sell", "price"})
public class ShopSaleValuePlugin extends Plugin
{
    @Inject Client client;
    @Inject ItemManager items;
    @Inject ShopSaleValueConfig config;
    @Inject OverlayManager overlays;
    @Inject ShopSaleValueOverlay overlay;
    @Inject Gson gson;
    private ShopCatalogue catalogue;
    private String title = "";
    private int selectedId = -1;
    private boolean dirty = true;
    private int currency = net.runelite.api.gameval.ItemID.COINS;
    private final Map<Integer,Integer> stock = new HashMap<>();
    private final Map<Integer,Integer> inventory = new HashMap<>();
    private final List<String> stockNames = new ArrayList<>();
    private final Map<Integer,Quote> quotes = new HashMap<>();

    @Provides ShopSaleValueConfig provideConfig(ConfigManager manager) { return manager.getConfig(ShopSaleValueConfig.class); }
    @Override protected void startUp() { catalogue = new ShopCatalogue(gson); clear(); overlays.add(overlay); }
    @Override protected void shutDown() { overlays.remove(overlay); clear(); catalogue = null; }

    private void clear()
    {
        title = ""; selectedId = -1; dirty = true;
        currency = net.runelite.api.gameval.ItemID.COINS;
        stock.clear(); inventory.clear(); stockNames.clear(); quotes.clear();
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN) clear();
    }
    @Subscribe public void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == InterfaceID.SHOPMAIN) clear();
    }
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) { dirty = true; }
    @Subscribe public void onGameTick(GameTick event) { dirty = true; }
    @Subscribe public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
    {
        if (event.getGroup().equals("shopsalevalue")) dirty = true;
    }
    @Subscribe public void onScriptPreFired(ScriptPreFired event)
    {
        // shop_main_init: inv, title, currency item, quantity, boolean.
        if (event.getScriptId() == 1074)
        {
            clear();
            Object[] args = event.getScriptEvent() == null ? null : event.getScriptEvent().getArguments();
            if (args != null && args.length > 2 && args[2] instanceof String)
                title = Text.removeTags((String) args[2]);
            if (args != null && args.length > 3 && args[3] instanceof Integer)
                currency = (Integer) args[3];
        }
    }

    boolean shopOpen()
    {
        Widget w = client.getWidget(InterfaceID.Shopmain.ITEMS);
        return client.getGameState() == GameState.LOGGED_IN && w != null && !w.isHidden();
    }

    private int canonical(int id)
    {
        ItemComposition item = items.getItemComposition(id);
        return item.getNote() != -1 && item.getLinkedNoteId() >= 0 ? item.getLinkedNoteId() : id;
    }

    private void refresh()
    {
        if (!dirty || !shopOpen()) return;
        dirty = false;
        quotes.clear(); stock.clear(); inventory.clear(); stockNames.clear();
        if (title.isEmpty())
        {
            Widget frame = client.getWidget(InterfaceID.Shopmain.FRAME);
            Widget label = frame == null ? null : frame.getChild(1);
            if (label != null) title = Text.removeTags(label.getText());
        }
        Widget[] children = client.getWidget(InterfaceID.Shopmain.ITEMS).getChildren();
        if (children != null) for (Widget w : children)
        {
            if (w == null || w.getItemId() < 0) continue;
            int id = canonical(w.getItemId());
            stock.merge(id, Math.max(0, w.getItemQuantity()), (a,b) -> (int)Math.min(Integer.MAX_VALUE, (long)a+b));
            stockNames.add(items.getItemComposition(id).getName());
        }
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null) for (Item item : inv.getItems())
        {
            if (item.getId() < 0) continue;
            inventory.merge(item.getId(), item.getQuantity(), (a,b) -> (int)Math.min(Integer.MAX_VALUE, (long)a+b));
        }
    }

    Quote quote(int id)
    {
        if (id < 0 || !shopOpen() || catalogue == null) return null;
        refresh();
        if (!inventory.containsKey(id)) return null;
        if (quotes.containsKey(id)) return quotes.get(id);
        ItemComposition item = items.getItemComposition(canonical(id));
        String problem = null;
        Pricing.Rule rule = null;
        int baseline = 0;
        String ruleName = "Unrecognised shop";
        boolean custom = !config.overrideTitle().trim().isEmpty()
            && ShopCatalogue.key(config.overrideTitle()).equals(ShopCatalogue.key(title));
        if (custom)
        {
            try { rule = new Pricing.Rule(config.buyPercent()*100, config.decreaseBps(), config.floorPercent()*100); }
            catch (IllegalArgumentException ex) { problem = "Check custom percentages"; }
            baseline = Math.max(0, config.baseline());
            ruleName = "Custom rules";
        }
        else
        {
            ShopCatalogue.Shop shop = catalogue.resolve(title, stockNames, item.getName());
            if (shop != null)
            {
                rule = shop.rule(); baseline = shop.baseline(item.getName());
                ruleName = String.format(Locale.US,"%.0f%% buy / %.2f%% drop", rule.buy/100.0, rule.decrease/100.0);
            }
            else problem = "Unknown shop / variant: set custom rules";
        }
        int current = stock.getOrDefault(item.getId(), 0);
        if (stockNames.isEmpty()) problem = "Waiting for shop stock";
        if (currency != net.runelite.api.gameval.ItemID.COINS) problem = "Non-coin shop is not supported";
        if (!item.isTradeable() || item.getId() == net.runelite.api.gameval.ItemID.COINS)
            problem = "Item cannot normally be sold";
        if (current < baseline && rule != null && rule.decrease > 0)
            problem = "Below normal stock: check Value";
        if (!stock.containsKey(item.getId()) && stockNames.size() >= 40)
            problem = "Shop may be full: check Value";
        if (rule == null && problem == null) problem = "No pricing rule";
        Quote result = new Quote(item.getName(), title, ruleName, problem, item.getPrice(), current,
            baseline, inventory.get(id), rule);
        quotes.put(id, result);
        return result;
    }

    @Subscribe public void onMenuEntryAdded(MenuEntryAdded event)
    {
        MenuEntry entry = event.getMenuEntry();
        if (entry.getParam1() != InterfaceID.Shopside.ITEMS) return;
        String option = Text.removeTags(entry.getOption());
        if (!option.equals("Value") && !option.startsWith("Sell ")) return;
        selectedId = entry.getItemId();
        decorate(entry);
    }

    @Subscribe public void onClientTick(ClientTick event)
    {
        if (!shopOpen()) return;
        refresh();
        // Menus remain open across stock updates; refresh labels without changing their actions.
        if (client.isMenuOpen()) for (MenuEntry entry : client.getMenuEntries()) decorate(entry);
    }

    private static String undecorated(String target)
    {
        int start = target.indexOf(" <col=80ff80> | ");
        return start < 0 ? target : target.substring(0, start);
    }

    private void decorate(MenuEntry entry)
    {
        if (entry.getParam1() != InterfaceID.Shopside.ITEMS) return;
        String option = Text.removeTags(entry.getOption());
        String original = undecorated(entry.getTarget());
        entry.setTarget(original);
        if (!config.menu()) return;
        Quote q = quote(entry.getItemId());
        if (q == null || q.problem != null) return;
        int requested;
        if (option.equals("Value")) requested = 1;
        else if (option.equals("Sell All")) requested = q.available;
        else if (option.matches("Sell [0-9]+"))
        {
            try { requested = Integer.parseInt(option.substring(5)); }
            catch (NumberFormatException ignored) { return; }
        }
        else return; // Sell X is unknown until entered; never pretend it is the comparison quantity.
        String suffix = q.price(requested);
        if (requested > q.available) suffix += " (" + q.available + " items)";
        entry.setTarget(original + " <col=80ff80> | " + suffix + "</col>");
    }

    Quote selectedQuote() { return quote(selectedId); }
}
