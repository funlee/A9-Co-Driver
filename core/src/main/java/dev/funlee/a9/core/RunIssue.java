package dev.funlee.a9.core;

/** User-facing recovery guidance. Raw engine errors remain in runtime logs. */
public final class RunIssue {
    public enum Action { ACCESSIBILITY, CAPTURE, SETTINGS, TASK_PREFERENCES, GAME, QUEUE }
    public final String title, guidance, actionLabel;
    public final Action action;
    private RunIssue(String title,String guidance,String actionLabel,Action action) {
        this.title=title;this.guidance=guidance;this.actionLabel=actionLabel;this.action=action;
    }
    private static boolean has(String text,String... words) {
        for(String word:words)if(text.contains(word))return true;
        return false;
    }
    public static String reason(String detail){
        String reason=(detail==null?"":detail).split("\n",2)[0].trim()
                    .replaceFirst("^(?:\\[string [^\\]]+\\]:\\d+:\\s*)+", "")
                    .replaceFirst("^(?:[\\w.$]+\\.)?[\\w$]*(?:Exception|Error):\\s*", "")
                    .replaceFirst("^_\\w+[：:]\\s*", "");
        if(reason.isEmpty())reason="暂时无法确定原因，请回到助手查看任务状态。";
        return reason;
    }
    public static String stopMessage(String detail,boolean saved){
        return reason(detail)+"\n\n"+(saved?"异常界面已截图保存，可在程序【偏好设置 → 问题排查】中查看。":"本次异常未能保存截图。");
    }
    public static RunIssue from(String detail) {
        String text=detail==null?"":detail;
        if(text.contains(AccountLoginDialog.STOP_REASON))
            return new RunIssue(AccountLoginDialog.STOP_REASON,"请在需要继续使用本机时手动登录游戏，再启动任务。","打开游戏",Action.GAME);
        if(has(text,"触控连续被系统取消","触控确认超时"))
            return new RunIssue("游戏触控未完成","上一次点击或滑动未收到系统确认。请打开游戏，等当前加载或弹窗处理完成后，再返回任务编排续跑。","打开游戏",Action.GAME);
        if(has(text,"无障碍服务已开启但未连接"))
            return new RunIssue("无障碍服务尚未连接","系统开关已开启。请稍等后重试。若持续未连接，可在系统设置中关闭再开启 A9辅助驾驶。","查看无障碍设置",Action.ACCESSIBILITY);
        if(has(text,"无障碍"))
            return new RunIssue("请开启无障碍触控","请在系统无障碍设置中开启 A9辅助驾驶，然后返回任务编排重新开始任务。","开启触控权限",Action.ACCESSIBILITY);
        if(has(text,"设备分辨率不符合要求"))
            return new RunIssue("设备分辨率不受支持","与原版一样，仅支持 720×1280（游戏横屏 1280×720）。请修改设备或模拟器的分辨率，关闭分屏和小窗后重新开始；不再适配其他尺寸。","查看设置",Action.SETTINGS);
        if(has(text,"画面方向或比例不匹配","校准旋转方向"))
            return new RunIssue("游戏画面方向不匹配","请打开游戏并保持横屏，关闭分屏或小窗后，再返回任务编排重新开始。","打开游戏",Action.GAME);
        if(has(text,"截屏","MediaProjection"))
            return new RunIssue("无法获取游戏画面","请重新允许屏幕录制。授权后会进入游戏，并优先继续未完成的任务。","重新授权截屏",Action.CAPTURE);
        if(has(text,"未找到游戏","找不到游戏启动入口","包名"))
            return new RunIssue("没有找到游戏","请确认游戏已安装，并在任务偏好中选择对应的目标游戏。","检查游戏设置",Action.TASK_PREFERENCES);
        if(has(text,"悬浮窗或通知"))
            return new RunIssue("尚未开启游戏内控制","请在偏好设置中允许悬浮窗或通知，以便在游戏中暂停或停止任务。","开启控制权限",Action.SETTINGS);
        if(has(text,"至少启用一个项目","没有可继续的未完成任务","无法读取此版本的任务记录"))
            return new RunIssue("请先选择要运行的项目","在下方项目队列中启用需要运行的项目，然后点击“开始”。","查看项目队列",Action.QUEUE);
        if(has(text,"10 分钟内已尝试恢复","重启后未能确认","未能确认游戏菜单加载完成","强行停止","系统应用信息页"))
            return new RunIssue("游戏自动恢复未成功","请手动打开游戏，确认可以正常操作，再返回任务编排；若显示“续跑”，可继续未完成的任务。","打开游戏",Action.GAME);
        if(has(text,"锁屏","前台","已停止恢复"))
            return new RunIssue("游戏没有保持在前台","请解锁设备并打开游戏。运行时保持游戏可见，再返回任务编排启动任务。","打开游戏",Action.GAME);
        if(has(text,"游戏弹窗遮挡画面","游戏弹窗关闭后仍遮挡画面"))
            return new RunIssue("游戏弹窗遮挡画面","请关闭游戏内的弹窗后续跑，当前任务进度已保留。","打开游戏",Action.GAME);
        if(has(text,"比赛奖励页面未完成结算","多人结算长时间未能切页"))
            return new RunIssue("多人赛结算停在当前页面","请在游戏中完成结算后续跑，已经确认的完赛进度会保留。","打开游戏",Action.GAME);
        if(has(text,"每日战利品车库最左起点","卡片与详情性能分不一致","未读到每日战利品第","未能确认选车页面或目标车位"))
            return new RunIssue("未能核验每日战利品车辆","请在游戏中核对车库筛选和配置的车辆序号，再继续任务。","打开游戏",Action.GAME);
        if(has(text,"未能确认进度","进度未知"))
            return new RunIssue("暂时无法读取比赛进度","请打开游戏，确认已进入比赛，并关闭遮挡画面的弹窗后重试。若反复出现，请停止任务并反馈问题。","打开游戏",Action.GAME);
        if(has(text,"点击每日赛事后仍在主菜单","未识别要进入的赛事标签"))
            return new RunIssue("未能进入赛事列表","请在游戏里点击「探索赛事」。确认看到赛事列表后，可回到任务编排续跑。","打开游戏",Action.GAME);
        if(has(text,"超宽屏未能确认配置的赛事位置","超宽屏赛事卡片未完整识别"))
            return new RunIssue("未能确认配置的赛事位置","请检查任务配置中的赛事位置与当前赛事列表，运行进度已保留。","打开游戏",Action.GAME);
        if(has(text,"未前进","单局时限","单局跑图超时"))
            return new RunIssue("比赛长时间没有完成","请检查游戏是否暂停、卡住或断开连接，恢复正常后再启动任务。","打开游戏",Action.GAME);
        if(has(text,"等待超时","匹配超过","界面超时"))
            return new RunIssue("等待游戏响应超时","请检查游戏连接并处理画面上的提示，回到赛事页面后再启动任务。","打开游戏",Action.GAME);
        return new RunIssue("任务遇到问题，已停止","暂时无法确定原因。请打开游戏，确认画面和网络正常后重试；若再次出现，请停止任务并反馈问题。","打开游戏",Action.GAME);
    }
}
