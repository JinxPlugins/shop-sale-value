package com.shopsalevalue;

/** All percentages use basis points (40% = 4000); round each item, never the sum. */
final class Pricing
{
    static final class Rule
    {
        final int buy, decrease, floor;
        Rule(int buy, int decrease, int floor)
        {
            if (buy < 0 || buy > 10000 || decrease < 0 || decrease > 10000 || floor < 0 || floor > buy)
                throw new IllegalArgumentException("Invalid shop percentages");
            this.buy = buy;
            this.decrease = decrease;
            this.floor = floor;
        }
    }

    static long unit(int value, long surplus, Rule rule)
    {
        if (value < 0 || surplus < 0) throw new IllegalArgumentException("Negative value or surplus");
        long rate = Math.max(rule.floor, rule.buy - surplus * rule.decrease);
        return (long) value * rate / 10000;
    }

    static long total(int value, long surplus, int quantity, Rule rule)
    {
        if (quantity < 0) throw new IllegalArgumentException("Negative quantity");
        long first = unit(value, surplus, rule);
        if (rule.decrease == 0) return first * quantity;
        long untilFloor = Math.max(0, (rule.buy - rule.floor + rule.decrease - 1L) / rule.decrease - surplus);
        int changing = (int) Math.min(quantity, untilFloor);
        long sum = 0;
        for (int i = 0; i < changing; i++) sum += unit(value, surplus + i, rule);
        return sum + (quantity - (long) changing) * unit(value, surplus + changing, rule);
    }
}
