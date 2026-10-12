package com.breathingroom.backend.logic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safety check for AI wording. The AI may only repeat dollar figures that Java already calculated.
 * If a $ amount in the AI text is not in the allowed list, the app throws the AI text away
 * and shows the built-in explanation instead.
 */
public final class DollarGuard {

    private static final Pattern DOLLAR = Pattern.compile("\\$\\s?([0-9][0-9,]*(?:\\.[0-9]+)?)");

    private DollarGuard() {}

    private static double parse(String s) {
        return Double.parseDouble(s.replace(",", ""));
    }

    /** @param allowedSources strings that contain the dollar figures Java calculated (e.g. "$350.00"). */
    public static boolean containsUnknownDollarAmount(String text, Collection<String> allowedSources) {
        if (text == null) return false;
        Set<Double> allowed = new HashSet<>();
        for (String source : allowedSources) {
            Matcher m = DOLLAR.matcher(source);
            while (m.find()) allowed.add(parse(m.group(1)));
        }
        Matcher m = DOLLAR.matcher(text);
        while (m.find()) {
            double value = parse(m.group(1));
            boolean ok = false;
            for (double a : allowed) {
                if (Math.abs(a - value) <= 0.51) { ok = true; break; } // allow "$65" for 64.50
            }
            if (!ok) return true;
        }
        return false;
    }
}
