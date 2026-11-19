package dev.funlee.a9.core;

final class RunIssueChecks {
    static void run() {
        check("请先开启 A9辅助驾驶的无障碍服务",RunIssue.Action.ACCESSIBILITY);
        check("无障碍服务已开启但未连接",RunIssue.Action.ACCESSIBILITY);
        CoreChecks.check(RunIssue.from("无障碍服务已开启但未连接").title.equals("无障碍服务尚未连接"),"An enabled switch does not imply a live service");
        check("触控连续被系统取消；请检查无障碍和画面方向",RunIssue.Action.GAME);
        check("tap：触控确认超时",RunIssue.Action.GAME);
        check("未收到截屏画面，请停止后重新授权截屏",RunIssue.Action.CAPTURE);
        check("截屏授权已结束",RunIssue.Action.CAPTURE);
        check("画面方向或比例不匹配，请在设置中校准旋转方向（当前 1280×720）",RunIssue.Action.GAME);
        check("请先开启悬浮窗或通知",RunIssue.Action.SETTINGS);
        check("未找到游戏：com.example.game",RunIssue.Action.TASK_PREFERENCES);
        check("请至少启用一个项目",RunIssue.Action.QUEUE);
        check("12 秒未能确认进度，请检查游戏画面是否被遮挡",RunIssue.Action.GAME);
        check("比赛进度 90 秒未前进，请检查游戏是否卡住或暂停",RunIssue.Action.GAME);
        check("当前前台为 com.example.other，已停止恢复",RunIssue.Action.GAME);
        check("多人界面等待超时：unknown，请查看诊断截图",RunIssue.Action.GAME);
        RunIssue unknown=RunIssue.from("[string multiplayer.lua]:91: java.lang.IllegalStateException: secret-detail\nstack traceback:");
        CoreChecks.check(unknown.action==RunIssue.Action.GAME,"Unknown errors provide a surviving recovery action");
        CoreChecks.check(!unknown.title.contains("secret-detail")&&!unknown.guidance.contains("java.lang")&&!unknown.guidance.contains("stack"),"Technical errors never leak into user guidance");
        CoreChecks.check(RunIssue.from(null).guidance.contains("无法确定原因"),"Unknown cause is not invented");
    }
    private static void check(String message,RunIssue.Action action) {
        RunIssue issue=RunIssue.from(message);
        CoreChecks.check(issue.action==action,"Recovery action for "+message);
        CoreChecks.check(!issue.guidance.contains("开发者工具")&&!issue.guidance.contains("旋转方向"),"No guidance to removed advanced/help pages");
        CoreChecks.check(!issue.title.isEmpty()&&!issue.guidance.isEmpty()&&!issue.actionLabel.isEmpty(),"Recovery has both explanation and action");
    }
}
