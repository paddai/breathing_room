package com.breathingroom.backend.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Slows down password guessing: after too many failures for one email, sign-in is paused for a while. */
public final class LoginThrottle {

    private static final class Entry {
        int failures;
        long firstFailureAt;
        long lockedUntil;
    }

    private final int maxFailures;
    private final long windowMillis;
    private final int maxKeys;
    private final LongSupplier nowMillis;
    private final Map<String, Entry> entries = new HashMap<>();

    public LoginThrottle(int maxFailures, long windowMillis, int maxKeys, LongSupplier nowMillis) {
        this.maxFailures = maxFailures;
        this.windowMillis = windowMillis;
        this.maxKeys = maxKeys;
        this.nowMillis = nowMillis;
    }

    public synchronized boolean isLocked(String key) {
        Entry e = entries.get(key);
        if (e == null) return false;
        long now = nowMillis.getAsLong();
        if (e.lockedUntil > now) return true;
        if (e.lockedUntil != 0 || now - e.firstFailureAt > windowMillis) entries.remove(key); // lock or window has ended
        return false;
    }

    public synchronized void recordFailure(String key) {
        long now = nowMillis.getAsLong();
        if (entries.size() >= maxKeys && !entries.containsKey(key)) {
            entries.values().removeIf(x -> x.lockedUntil <= now && now - x.firstFailureAt > windowMillis);
            if (entries.size() >= maxKeys) entries.clear(); // memory safety valve
        }
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        if (e.failures == 0 || now - e.firstFailureAt > windowMillis) {
            e.failures = 0;
            e.firstFailureAt = now;
            e.lockedUntil = 0;
        }
        e.failures++;
        if (e.failures >= maxFailures) e.lockedUntil = now + windowMillis;
    }

    public synchronized void reset(String key) {
        entries.remove(key);
    }
}
