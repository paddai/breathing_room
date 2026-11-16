package com.breathingroom.backend.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    /** The $ and % figures in the text that Java did not supply, as they appear in the text (for the retry message and the note). */
    public static List<String> unknownNumbers(String text, Collection<String> allowedSources) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        Set<Double> money = new HashSet<>();
        Set<Double> pcts = new HashSet<>();
        java.util.regex.Pattern dollar = java.util.regex.Pattern.compile("\\$\\s?([0-9][0-9,]*(?:\\.[0-9]+)?)");
        for (String source : allowedSources) {
            Matcher d = dollar.matcher(source);
            while (d.find()) money.add(Double.parseDouble(d.group(1).replace(",", "")));
            Matcher p = PERCENT.matcher(source);
            while (p.find()) pcts.add(Double.parseDouble(p.group(1)));
        }
        Matcher d = dollar.matcher(text);
        while (d.find()) {
            double v = Double.parseDouble(d.group(1).replace(",", ""));
            if (money.stream().noneMatch(a -> Math.abs(a - v) <= 0.51)) out.add("$" + d.group(1));
        }
        Matcher p = PERCENT.matcher(text);
        while (p.find()) {
            double v = Double.parseDouble(p.group(1));
            if (pcts.stream().noneMatch(a -> Math.abs(a - v) <= 0.51)) out.add(p.group(1) + "%");
        }
        return out;
    }

    public static boolean containsUnknownNumber(String text, Collection<String> allowedSources) {
        return DollarGuard.containsUnknownDollarAmount(text, allowedSources) || containsUnknownPercent(text, allowedSources);
    }
}
