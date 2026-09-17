package com.shopsalevalue;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Random;

public class PricingTest
{
    private final Pricing.Rule normal = new Pricing.Rule(4000,300,1000);
    @Test public void oakShortbowUnstrungAtEmptyStore()
    {
        assertEquals(20, Pricing.unit(50,0,normal));
        assertEquals(84, Pricing.total(50,0,5,normal));
        assertEquals(130, Pricing.total(50,0,10,normal));
        assertEquals(330, Pricing.total(50,0,50,normal));
        assertEquals(5, Pricing.unit(50,10,normal));
    }
    @Test public void overstockStartsAtCurrentStock()
    {
        assertEquals(12, Pricing.unit(50,5,normal));
        assertEquals(46, Pricing.total(50,5,5,normal));
        assertEquals(250, Pricing.total(50,10,50,normal));
    }
    @Test public void higherBuyRateStillDrops()
    {
        Pricing.Rule martin = new Pricing.Rule(6000,200,1000);
        assertEquals(30, Pricing.unit(50,0,martin));
        assertEquals(140, Pricing.total(50,0,5,martin));
    }
    @Test public void fractionalDropAndRounding()
    {
        assertEquals(19, Pricing.unit(50,1,new Pricing.Rule(4000,30,1000)));
        assertEquals(0, Pricing.total(1,0,50,normal));
        assertEquals(0, Pricing.total(50,0,0,normal));
    }
    @Test public void fixedPricesIgnoreStockAndHandleHugeStacks()
    {
        assertEquals(429496729400L, Pricing.total(400,Integer.MAX_VALUE,Integer.MAX_VALUE,new Pricing.Rule(5000,0,1000)));
        assertEquals(10737418235L, Pricing.total(50,Integer.MAX_VALUE,Integer.MAX_VALUE,normal));
    }
    @Test public void optimizedTotalMatchesIndependentPerItemSum()
    {
        Random random = new Random(719);
        for (int t=0;t<1000;t++)
        {
            int value = random.nextInt(100000), stock = random.nextInt(1000), n = random.nextInt(1000);
            int buy = 1000+random.nextInt(5001), drop = random.nextInt(301);
            long expected = 0;
            for(int i=0;i<n;i++) expected += (long)value*Math.max(1000,buy-(long)(stock+i)*drop)/10000;
            assertEquals(expected,Pricing.total(value,stock,n,new Pricing.Rule(buy,drop,1000)));
        }
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsFloorAboveStartingRate()
    {
        new Pricing.Rule(4000,300,6000);
    }
}
