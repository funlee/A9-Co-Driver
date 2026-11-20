package dev.funlee.a9.core;

/** Coalesces ordinary game hints to the latest operation while a previous notice is readable. */
public final class ActivityHintPolicy {
    public static final long SHOW_MS=2000, INTERVAL_MS=5000;
    private String displayed="", pending="";
    private long nextAllowed;

    public void offer(String message){
        pending=message.equals(displayed)?"":message;
    }
    public long delay(long now){return Math.max(0,nextAllowed-now);}
    public String take(long now){
        if(pending.isEmpty()||delay(now)>0)return "";
        displayed=pending;
        pending="";
        nextAllowed=now+INTERVAL_MS;
        return displayed;
    }
    public void reset(long quietUntil){
        pending=displayed="";
        nextAllowed=quietUntil;
    }

    /** Raw HUD also carries OCR polling, driving instructions and counters. */
    public static String fromHud(String value){
        if(value==null)return "";
        int end=value.indexOf('\n');
        String line=(end<0?value:value.substring(0,end)).trim();
        if(line.startsWith("项目 ")&&line.matches("项目 [0-9]+：[^： ]+")){
            int separator=line.indexOf('：');
            return line.substring(0,separator+1)+MultiplayerUi.displayMode(line.substring(separator+1));
        }
        if(line.equals("返回游戏主界面")||line.equals("请手动进入比赛，等待比赛画面"))return line;
        if(line.equals("进入杯赛界面")||line.equals("选择指定杯赛")||line.equals("选择杯赛")||
                line.equals("选择配置的赛事"))return "正在选择配置的赛事";
        if(line.equals("进入特殊赛事")||line.equals("特殊赛事：继续进入配置阶段"))return "正在进入特殊赛事";
        if(line.equals("开始选车")||line.equals("即将进入车库")||line.equals("正在定位配置车辆")||
                line.startsWith("选择车辆序号 ")||line.startsWith("使用配置车辆 "))return "正在选择配置车辆";
        if(line.equals("匹配中")||line.equals("等待多人匹配")||line.startsWith("匹配 ")||
                line.startsWith("等待匹配中-"))return "正在匹配对手";
        return "";
    }
}
