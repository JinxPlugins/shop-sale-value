package com.shopsalevalue;

import java.util.Locale;

final class Quote
{
    final String name, shop, ruleName, problem;
    final int value, stock, baseline, available;
    final Pricing.Rule rule;

    Quote(String name, String shop, String ruleName, String problem, int value, int stock,
          int baseline, int available, Pricing.Rule rule)
    {
        this.name = name; this.shop = shop; this.ruleName = ruleName; this.problem = problem;
        this.value = value; this.stock = stock; this.baseline = baseline; this.available = available; this.rule = rule;
    }
    int quantity(int requested) { return Math.min(Math.max(0, requested), available); }
    long total(int requested) { return Pricing.total(value, Math.max(0L, (long) stock - baseline), quantity(requested), rule); }
    long first() { return Pricing.unit(value, Math.max(0L, (long) stock - baseline), rule); }
    String price(int requested)
    {
        int n = quantity(requested);
        if (n == 0) return "No items";
        long total = total(n);
        return String.format(Locale.US, "%,d gp [%,.2f ea]", total, total / (double) n);
    }
}
