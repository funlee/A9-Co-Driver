package dev.funlee.a9.core;

import java.time.Clock;
import java.time.LocalDateTime;

/** Daily-loot quiet hours in the device's local timezone. */
public final class DailyTaskPolicy {
    private final Clock clock;
    public DailyTaskPolicy(Clock clock) { this.clock=clock; }
    public String skipReason() {
        LocalDateTime now=LocalDateTime.now(clock);
        if(now.getHour()==0 && now.getMinute()<30) return "dailyQuietHours";
        return "";
    }
}
