package dev.funlee.a9.core;

import java.time.Clock;
import java.time.LocalDateTime;

/** Daily-loot scheduling only; clock and durable storage are supplied by the host. */
public final class DailyTaskPolicy {
    public interface CompletionStore {
        String read(String taskId);
        void write(String taskId, String date);
    }
    private final Clock clock;
    private final CompletionStore store;
    public DailyTaskPolicy(Clock clock, CompletionStore store) { this.clock=clock; this.store=store; }
    public String skipReason(String taskId, boolean once) {
        LocalDateTime now=LocalDateTime.now(clock);
        if(now.getHour()==0 && now.getMinute()<30) return "dailyQuietHours";
        if(once && now.toLocalDate().toString().equals(store.read(taskId))) return "dailyAlreadyDone";
        return "";
    }
    public void completed(String taskId, boolean once, int completed, int target) {
        if(once && target>0 && completed>=target)
            store.write(taskId,LocalDateTime.now(clock).toLocalDate().toString());
    }
}
