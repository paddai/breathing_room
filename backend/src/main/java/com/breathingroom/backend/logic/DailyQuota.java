package com.breathingroom.backend.logic;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Caps how many AI calls each user can trigger per day, so one account cannot run up the OpenAI bill. */
public final class DailyQuota {

    private final int max;
    private final Clock clock;
    private final Map<Long, Integer> counts = new HashMap<>();
    private LocalDate day;

    public DailyQuota(int max, Clock clock) {
        this.max = max;
        this.clock = clock;
    }

    public synchronized boolean tryConsume(long userId) {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            counts.clear();
            day = today;
        }
        int used = counts.getOrDefault(userId, 0);
        if (used >= max) return false;
        counts.put(userId, used + 1);
        return true;
    }
}
