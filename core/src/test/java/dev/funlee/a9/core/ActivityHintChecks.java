package dev.funlee.a9.core;

final class ActivityHintChecks {
    static void run(){
        ActivityHintPolicy hints=new ActivityHintPolicy();
        CoreChecks.check(ActivityHintPolicy.fromHud("请手动进入比赛，等待比赛画面").equals("请手动进入比赛，等待比赛画面"),
                "Manual driving prompts the user to enter a race");
        String project=ActivityHintPolicy.fromHud("项目 1：多人上\n0%");
        CoreChecks.check(project.equals("项目 1：世界系列赛"),"Project hint uses the visible event name");
        hints.offer(project);
        CoreChecks.check(hints.take(100).equals(project),"First operation is visible immediately");
        hints.offer(project);
        CoreChecks.check(hints.take(9000).isEmpty(),"A stable operation is not repeated");
        hints.offer("选择赛事");
        hints.take(10000);
        hints.offer("选择车辆");
        hints.offer("匹配对手");
        CoreChecks.check(hints.take(14999).isEmpty()&&hints.take(15000).equals("匹配对手"),
                "Short transitions coalesce to the latest phase at the original deadline");
        hints.offer("比赛开始");
        hints.reset(20000);
        hints.offer("重新进入赛事");
        CoreChecks.check(hints.take(19999).isEmpty()&&hints.take(20000).equals("重新进入赛事"),
                "Important notices retain their time and discard superseded operations");
        CoreChecks.check(ActivityHintPolicy.fromHud("匹配 8 秒\n0%").equals(ActivityHintPolicy.fromHud("匹配 12 秒\n0%"))
                && ActivityHintPolicy.fromHud("智能选路 53%").isEmpty()&&ActivityHintPolicy.fromHud("多人流程：cars").isEmpty(),
                "Polls and changing progress do not produce new activity hints");
    }
}
