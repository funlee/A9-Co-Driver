package dev.funlee.a9.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import party.iroiro.luajava.Lua;

/** Runs the production Lua queue across fresh runtimes with persisted checkpoints. */
final class QueueRetryChecks {
    private static final List<String> HOST_CALLS = List.of(
            "_queueResume", "_queueBeginRewards", "_queueRewardsDone", "_queueEnter", "_queueDone",
            "_queueHomeConfirmed", "_sessionProject", "_sessionCycle", "_hud", "print", "_taskNotice",
            "settle", "shouldFail", "reachHome", "observeQuota");
    private TaskCheckpoint queue = new TaskCheckpoint();
    private final List<String> entries = new ArrayList<>();
    private final List<String> quotas = new ArrayList<>();
    private final List<String> settlements = new ArrayList<>();
    private boolean strictHome, done;
    private int cycle, index, rewardAttempts, confirmedHomes, launches, checks;

    static int run(Path assets) throws Exception {
        QueueRetryChecks replay = new QueueRetryChecks();
        replay.checkpointRoundTrip();
        replay.soloSettlementBeforeReturn();
        String bootstrap = Files.readString(assets.resolve("lua/bootstrap.lua"));
        String fixture;
        try (var input = QueueRetryChecks.class.getResourceAsStream("/queue-retry.lua")) {
            fixture = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        replay.watchdog(bootstrap);
        replay.play(bootstrap, fixture);
        return replay.checks;
    }

    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private TaskCheckpoint fromDisk(Map<String, Object> saved) {
        TaskCheckpoint restored = new TaskCheckpoint();
        restored.restore((int) saved.get("cycle"), (int) saved.get("index"), (int) saved.get("completed"),
                (int) saved.getOrDefault("rewardCycle", saved.get("cycle")));
        restored.restoreRecovery((int) saved.getOrDefault("failures", 0),
                (boolean) saved.getOrDefault("needsHome", false), (boolean) saved.getOrDefault("homeRestarted", false),
                (boolean) saved.getOrDefault("rewardWork", false), (String) saved.getOrDefault("firstFailure", ""),
                (String) saved.getOrDefault("lastFailure", ""));
        return restored;
    }

    private void checkpointRoundTrip() {
        TaskCheckpoint pending = fromDisk(Map.of("cycle", 3, "index", 2, "completed", 1));
        check(pending.completed() == 1 && !pending.needsHome(), "Older records resume their saved race count");
        pending.enter(3, 2, 5);
        pending.failure("first");
        check(pending.beginHomeRestart(), "First failure to reach home permits one restart");
        pending = fromDisk(pending.resume());
        check(!pending.beginHomeRestart() && pending.needsHome() && pending.completed() == 1,
                "Another VM cannot consume a second restart or advance without confirming home");
        pending.homeConfirmed();
        pending = fromDisk(pending.resume());
        pending.enter(3, 2, 5);
        check((int) pending.resume().get("failures") == 1, "Restart after reaching home preserves the used retry");
        pending.failure("second");
        check(pending.failureSummary().contains("first") && pending.failureSummary().contains("second"),
                "Both failures remain available when deciding to skip");
        pending.homeConfirmed();
        Map<String, Object> saved = pending.resume();
        check((int) saved.get("index") == 3 && (int) saved.get("completed") == 0,
                "Skipping a failed project creates no extra race completions");
    }

    private void soloSettlementBeforeReturn() {
        TaskStats stats = new TaskStats();
        stats.project("特殊赛事");
        stats.begin("测试", false);
        TaskCheckpoint checkpoint = new TaskCheckpoint();
        checkpoint.enter(1, 1, 3);
        checkpoint.recordResult(stats, 0);
        checkpoint.recordResult(stats, 0);
        stats.rewards(2880, 207);
        checkpoint.failure("赛后返回被弹窗打断");
        stats = TaskStats.restore(stats.snapshot());
        checkpoint = fromDisk(checkpoint.resume());
        checkpoint.homeConfirmed();
        checkpoint.enter(1, 1, 3);
        check(stats.finished() == 1 && stats.returnedCount() == 0 && stats.coins() == 2880,
                "An interruption after solo results preserves the settlement and its observed rewards");
        check(checkpoint.completed() == 1, "Seeing both results and rewards consumes a single race from the quota");
    }

    private void watchdog(String bootstrap) {
        try (LuaRuntime lua = new LuaRuntime((name, stack) -> 0, List.of("_hud"))) {
            lua.load("queue", bootstrap);
            lua.load("clock", "UIconfig={routeOnly=''};data={['心跳时间']=1000,['等待时间']=420};" +
                    "now=1420;os.time=function()return now end;" +
                    "function _requestGameRestart(reason) error('restart:'..reason) end");
            lua.start("monitor");
            check(lua.step(0), "Seven-minute threshold is measured from the business heartbeat");
            lua.load("home in progress", "queueRecovering=true;now=2000");
            check(lua.step(1000), "Home recovery owns its own deadline");
            lua.load("business resumed", "queueRecovering=false;data['心跳时间']=2000;now=2010");
            check(lua.step(2000), "A real progress update renews the business deadline");
            lua.load("stalled", "now=2421");
            try {
                lua.step(3000);
                throw new AssertionError("Hung business must leave the VM even if its own loop never checks the flag");
            } catch (RuntimeException failure) {
                check(failure.getMessage().contains("restart:heartbeat"), "Hung work requests a heartbeat restart");
            }
        }
    }

    private void play(String bootstrap, String fixture) {
        List<Map<String, Object>> projects = List.of(project("特殊赛事", 3), project("多人上", 2),
                project("杯赛", 80), project("车联", 1));
        Map<String, Object> config = Map.of("routeOnly", "", "projects", projects,
                "ActiveProj", List.of(true, true, false, true));
        for (int vm = 0; vm < 10 && !done; vm++) {
            strictHome = false;
            try (LuaRuntime lua = new LuaRuntime(this::call, HOST_CALLS)) {
                lua.set("UIconfig", config);
                lua.load("queue", bootstrap);
                lua.load("fixture", fixture);
                lua.start("main");
                for (int step = 0; step < 150 && !done; step++) lua.step(step * 1000L);
            } catch (RuntimeException failure) {
                String reason = failure.getMessage();
                if (reason.contains("回主流程未确认游戏主界面")) {
                    check(queue.beginHomeRestart(), "Home recovery restarts once");
                    launches++;
                } else if (reason.contains("fixture: reward blocked") || reason.contains("fixture: cannot read car")) {
                    queue.failure(reason);
                } else throw failure;
                queue = fromDisk(queue.resume());
            }
        }
        check(done && launches == 1 && confirmedHomes == 5, "Retry and skip only proceed from confirmed home");
        check(rewardAttempts == 3, "Twice-failed rewards are skipped this cycle and collected in the next");
        check(entries.equals(List.of("1/1", "1/1", "1/2", "1/2", "1/4", "2/1", "2/2", "2/4")),
                "Only enabled projects run in order: " + entries);
        check(quotas.equals(List.of("1/1:3", "1/1:2", "1/2:2", "1/2:1", "1/4:1", "2/1:3", "2/2:2", "2/4:1")),
                "Retry runs only the unfinished quota and the next cycle gets a fresh quota: " + quotas);
        check(settlements.equals(List.of("1/1", "1/1", "1/1", "1/2", "1/4",
                        "2/1", "2/1", "2/1", "2/2", "2/2", "2/4")),
                "Previously settled races survive and skipped races never count: " + settlements);
    }

    private static Map<String, Object> project(String name, int target) {
        return Map.of("项目", name, "data", Map.of("次数", Integer.toString(target)));
    }

    private int call(String name, Lua lua) {
        switch (name) {
            case "_queueResume":
                LuaRuntime.push(lua, queue.resume());
                return 1;
            case "_queueBeginRewards":
                queue.beginRewards((int) lua.toInteger(1));
                break;
            case "_queueRewardsDone":
                queue.rewardsDone((int) lua.toInteger(1));
                break;
            case "reachHome":
                check(queue.needsHome(), "Strict home recovery runs only for a pending failure");
                strictHome = launches > 0;
                lua.push(strictHome);
                return 1;
            case "_queueHomeConfirmed":
                check(strictHome, "Cannot advance on a failed home confirmation");
                queue.homeConfirmed();
                confirmedHomes++;
                LuaRuntime.push(lua, queue.resume());
                return 1;
            case "_sessionCycle":
                cycle = (int) lua.toInteger(1);
                break;
            case "_queueEnter":
                index = (int) lua.toInteger(2);
                queue.enter((int) lua.toInteger(1), index, (int) lua.toInteger(3));
                entries.add(cycle + "/" + index);
                lua.push(queue.completed());
                return 1;
            case "_sessionProject":
                lua.push(queue.completed());
                return 1;
            case "_queueDone":
                queue.projectDone();
                done = cycle == 2 && index == 4;
                break;
            case "shouldFail":
                if (lua.toString(1).equals("reward")) {
                    rewardAttempts++;
                    lua.push(cycle == 1);
                } else {
                    int used = (int) queue.resume().get("failures");
                    lua.push(cycle == 1 && queue.completed() == 1 && (index == 2 || (index == 1 && used == 0)));
                }
                return 1;
            case "observeQuota":
                quotas.add(cycle + "/" + index + ":" + lua.toInteger(1));
                break;
            case "settle":
                queue.completed(1);
                settlements.add(cycle + "/" + index);
                break;
            default:
                break;
        }
        return 0;
    }
}
