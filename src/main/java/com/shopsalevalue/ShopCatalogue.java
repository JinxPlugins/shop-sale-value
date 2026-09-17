package com.shopsalevalue;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ShopCatalogue
{
    static final class Shop
    {
        String name;
        int buy;
        int decrease;
        boolean manualOnly;
        Map<String,Integer> stock;
        Pricing.Rule rule() { return new Pricing.Rule(buy, decrease, Math.min(1000, buy)); }
        int baseline(String item) { return stock.getOrDefault(item, 0); }
    }

    private final List<Shop> shops;
    ShopCatalogue(Gson gson)
    {
        try (InputStream stream = getClass().getResourceAsStream("/shops.json"))
        {
            if (stream == null) throw new IllegalStateException("Shop catalogue missing");
            shops = Arrays.asList(gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Shop[].class));
        }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    static String key(String title)
    {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    Shop resolve(String title, List<String> liveItems, String item)
    {
        String key = key(title);
        // A generic title is shared by several stores. Never infer its rate from the title alone.
        if (!key.equals("generalstore"))
        {
            for (Shop shop : shops)
                if (key(shop.name).equals(key)) return shop.manualOnly ? null : shop;
            return null;
        }
        Shop match = null;
        int longest = 0;
        boolean ambiguous = false;
        for (Shop shop : shops)
        {
            List<String> names = new ArrayList<>(shop.stock.keySet());
            if (names.isEmpty() || liveItems.size() < names.size()
                || !liveItems.subList(0, names.size()).equals(names)) continue;
            if (names.size() < longest) continue;
            if (names.size() > longest) { match = null; ambiguous = false; longest = names.size(); }
            if (shop.manualOnly) ambiguous = true;
            if (match != null && (match.buy != shop.buy || match.decrease != shop.decrease
                || match.baseline(item) != shop.baseline(item))) ambiguous = true;
            match = shop;
        }
        return ambiguous ? null : match;
    }
}
