package com.shopsalevalue;

import com.google.gson.Gson;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShopCatalogueTest
{
    final ShopCatalogue catalogue = new ShopCatalogue(new Gson());
    @Test public void normalAndExceptionalNames()
    {
        assertEquals(4000,catalogue.resolve("Lumbridge General Store.",Collections.emptyList(),"Oak shortbow (u)").buy);
        assertEquals(6000,catalogue.resolve("Martin Thwait's Lost and Found.",Collections.emptyList(),"Oak shortbow (u)").buy);
        assertEquals(30,catalogue.resolve("Void Knight General Store",Collections.emptyList(),"Oak shortbow (u)").decrease);
        assertEquals(5500,catalogue.resolve("West Ardougne General Store",Collections.emptyList(),"Oak shortbow (u)").buy);
    }
    @Test public void refusesUnknownAndDiaryVariants()
    {
        assertNull(catalogue.resolve("Unknown General Store",Collections.emptyList(),"Oak shortbow (u)"));
        assertNull(catalogue.resolve("General Store",Collections.emptyList(),"Oak shortbow (u)"));
        assertNull(catalogue.resolve("Jiminua's Jungle Store.",Collections.emptyList(),"Oak shortbow (u)"));
    }
    @Test public void baselineComesFromCatalogueNotOpeningStock()
    {
        ShopCatalogue.Shop shop = catalogue.resolve("Lumbridge General Store",Collections.emptyList(),"Pot");
        assertTrue(shop.baseline("Pot")>0);
        assertEquals(0,shop.baseline("Oak shortbow (u)"));
    }
    @Test public void genericTitleUsesWholeOrderedDefaultStockPrefix()
    {
        ShopCatalogue.Shop lumbridge = catalogue.resolve("Lumbridge General Store",Collections.emptyList(),"Oak shortbow (u)");
        List<String> names = new ArrayList<>(lumbridge.stock.keySet());
        names.add("Oak shortbow (u)");
        assertEquals(4000,catalogue.resolve("General Store",names,"Oak shortbow (u)").buy);
        Collections.reverse(names);
        assertNull(catalogue.resolve("General Store",names,"Oak shortbow (u)"));
    }
}
