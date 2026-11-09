package com.breathingroom.backend.logic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Like DollarGuard, but also checks percentages. Any $ or % figure in AI wording must already appear in the facts Java supplied. */
public final class NumberGuard {

    private static final Pattern PERCENT = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s?%");

    private NumberGuard() {}

    public static boolean containsUnknownPercent(String text, Collection<String> allowedSources) {
        if (text == null) return false;
        Set<Double> allowed = new HashSet<>();
        for (String source : allowedSources) {
            Matcher m = PERCENT.matcher(source);
            while (m.find()) allowed.add(Double.parseDouble(m.group(1)));
        }
        Matcher m = PERCENT.matcher(text);
        while (m.find()) {
            double v = Double.parseDouble(m.group(1));
            if (allowed.stream().noneMatch(a -> Math.abs(a - v) <= 0.51)) return true;
        }
        return false;
    }

    public static boolean containsUnknownNumber(String text, Collection<String> allowedSources) {
        return DollarGuard.containsUnknownDollarAmount(text, allowedSources) || containsUnknownPercent(text, allowedSources);
    }
}
