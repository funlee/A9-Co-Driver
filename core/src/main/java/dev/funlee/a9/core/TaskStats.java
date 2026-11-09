package dev.funlee.a9.core;

import java.util.*;

/** Per-start counters. Unknown ranks never silently enter the top-three denominator. */
public final class TaskStats {
    private int target,started,finished,returned,fallbacks,progress,actions,eventCount,totalActions;
    private int cycle,projectTarget,projectBase,rank,ranked,topThree,restarts;
    private long matchingSeconds=-1;
    private long elapsed,coins,fame;
    private int coinRaces,fameRaces;
    private long raceCoins=-1,raceFame=-1,lastCoins=-1,lastFame=-1;
    private boolean fallback;
    private String fallbackReason="";
    private boolean active,resultRecorded,returnRecorded,rankConflict;
    private String mode="任务",modeKey="",map="等待识别",operation="等待比赛";
    private final Map<String,Integer> counts=new LinkedHashMap<>();
    public synchronized void target(int count){target=Math.max(0,count);}
    public synchronized void project(String value){fallback=false;fallbackReason="";map="等待识别";operation="等待比赛";progress=0;actions=0;eventCount=0;rank=0;modeKey=value;mode=MultiplayerUi.displayMode(value).replace("（原多人上）","").replace("（原多人下）","");}
    public synchronized void cycle(int n){cycle=n;}
    public synchronized void projectTarget(int n){projectTarget=n;projectBase=finished;}
    public synchronized void resumeProject(int n,int completed){projectTarget=n;projectBase=finished-completed;}
    public synchronized void matching(long ms){matchingSeconds=ms<0?-1:ms/1000;}
    public synchronized void recovered(){restarts++;active=false;resultRecorded=false;returnRecorded=false;matchingSeconds=-1;operation="重启后继续剩余任务";}
    public synchronized int restartCount(){return restarts;}
    public synchronized long elapsedMillis(){return elapsed;}
    public synchronized Map<String,Object> snapshot(){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("target",target);m.put("started",started);m.put("finished",finished);m.put("returned",returned);
        m.put("fallbacks",fallbacks);m.put("progress",progress);m.put("actions",actions);m.put("eventCount",eventCount);m.put("totalActions",totalActions);
        m.put("cycle",cycle);m.put("projectTarget",projectTarget);m.put("projectBase",projectBase);m.put("rank",rank);m.put("ranked",ranked);m.put("topThree",topThree);m.put("restarts",restarts);m.put("elapsed",elapsed);
        m.put("coins",coins);m.put("fame",fame);m.put("coinRaces",coinRaces);m.put("fameRaces",fameRaces);m.put("lastCoins",lastCoins);m.put("lastFame",lastFame);m.put("fallback",fallback);m.put("fallbackReason",fallbackReason);
        m.put("mode",mode);m.put("modeKey",modeKey);m.put("map",map);m.put("operation",operation);m.put("counts",new LinkedHashMap<>(counts));
        return m;
    }
    private static int number(Map<?,?> m,String key){Object v=m.get(key);if(!(v instanceof Number)||((Number)v).longValue()<0||((Number)v).longValue()>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid saved counter: "+key);return ((Number)v).intValue();}
    public static TaskStats restore(Map<String,Object> m){
        TaskStats s=new TaskStats();
        s.target=number(m,"target");s.started=number(m,"started");s.finished=number(m,"finished");s.returned=number(m,"returned");
        s.fallbacks=number(m,"fallbacks");s.progress=number(m,"progress");s.actions=number(m,"actions");s.eventCount=number(m,"eventCount");s.totalActions=number(m,"totalActions");
        s.cycle=number(m,"cycle");s.projectTarget=number(m,"projectTarget");s.projectBase=number(m,"projectBase");s.rank=number(m,"rank");s.ranked=number(m,"ranked");s.topThree=number(m,"topThree");s.restarts=number(m,"restarts");
        Object elapsed=m.get("elapsed");if(!(elapsed instanceof Number)||((Number)elapsed).longValue()<0)throw new IllegalArgumentException("Invalid elapsed time");s.elapsed=((Number)elapsed).longValue();
        for(String key:List.of("mode","modeKey","map","operation"))if(!(m.get(key) instanceof String))throw new IllegalArgumentException("Invalid saved label: "+key);
        s.mode=(String)m.get("mode");s.modeKey=(String)m.get("modeKey");s.map=(String)m.get("map");s.operation="等待恢复任务";
        if(!(m.get("counts") instanceof Map))throw new IllegalArgumentException("Invalid mode counters");
        Map<?,?> counts=(Map<?,?>)m.get("counts");long sum=0;
        for(Object key:counts.keySet()){if(!(key instanceof String))throw new IllegalArgumentException("Invalid mode");int n=number(counts,(String)key);sum+=n;s.counts.put((String)key,n);}
        if(sum!=s.finished||s.returned>s.finished||s.count("多人上")+s.count("多人下")>s.started||s.topThree>s.ranked||s.ranked>s.count("多人上")+s.count("多人下")||s.rank>8||s.projectBase>s.finished||s.progress>100)
            throw new IllegalArgumentException("Saved task counters are inconsistent");
        s.coins=savedLong(m,"coins",0,0);s.fame=savedLong(m,"fame",0,0);long goldReads=savedLong(m,"coinRaces",0,0),fameReads=savedLong(m,"fameRaces",0,0);if(goldReads>Integer.MAX_VALUE||fameReads>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid saved earnings coverage");s.coinRaces=(int)goldReads;s.fameRaces=(int)fameReads;
        s.lastCoins=savedLong(m,"lastCoins",-1,-1);s.lastFame=savedLong(m,"lastFame",-1,-1);
        if(s.coinRaces<0||s.fameRaces<0||s.coinRaces>s.finished||s.fameRaces>s.finished||s.coins>99_999_999L*s.coinRaces||s.fame>99_999_999L*s.fameRaces||s.lastCoins>99_999_999||s.lastFame>99_999_999)throw new IllegalArgumentException("Invalid saved earnings");
        s.fallback=Boolean.TRUE.equals(m.get("fallback"));s.fallbackReason=m.get("fallbackReason") instanceof String?(String)m.get("fallbackReason"):"";
        // The previous race no longer owns callbacks after a VM/service restart.
        return s;
    }
    private static long savedLong(Map<String,Object> m,String key,long defaultValue,long min){
        if(!m.containsKey(key))return defaultValue;Object v=m.get(key);
        if(!(v instanceof Number)||((Number)v).longValue()<min||((Number)v).doubleValue()!=((Number)v).longValue())throw new IllegalArgumentException("Invalid saved earnings: "+key);
        return ((Number)v).longValue();
    }
    public synchronized boolean awaitingRewards(){return active&&resultRecorded;}
    public synchronized boolean rewards(long gold,long experience){
        if(!active||!resultRecorded)return false;boolean changed=false;
        if(gold>=0&&gold<=99_999_999&&raceCoins<0){raceCoins=lastCoins=gold;coins+=gold;coinRaces++;changed=true;}
        if(experience>=0&&experience<=99_999_999&&raceFame<0){raceFame=lastFame=experience;fame+=experience;fameRaces++;changed=true;}
        return changed;
    }
    public synchronized void routeReason(String reason){fallbackReason=reason==null?"":reason;}
    public synchronized void routeMap(String displayMap,boolean invalid){if(displayMap!=null&&!displayMap.isEmpty())map=displayMap;if(invalid&&!fallback&&active){fallback=true;fallbacks++;fallbackReason="暂无有效跑法";}}
    public synchronized long coins(){return coins;}
    public synchronized long fame(){return fame;}
    public synchronized void elapsed(long ms){elapsed=Math.max(elapsed,ms);}
    public synchronized void operation(String value){operation=value;}
    public synchronized void begin(String displayMap,boolean fallback){
        this.fallback=fallback;fallbackReason=fallback?(displayMap==null||displayMap.isEmpty()?"地图未识别":"暂无有效跑法"):"";raceCoins=raceFame=-1;
        active=true;resultRecorded=false;returnRecorded=false;rankConflict=false;rank=0;started++;progress=0;actions=0;eventCount=0;
        operation=fallback?"左划 + 氮气（兜底）":"等待路线指令";
        map=displayMap==null||displayMap.isEmpty()?(fallback?"未识别 · 左划兜底":"未识别"):displayMap;
        if(fallback)fallbacks++;
    }
    public synchronized void progress(int value,int index,int count){
        if(!active||resultRecorded)return;
        if(value>=0)progress=Math.min(100,value);
        if(index>=0){totalActions+=index>=actions?index-actions:index;actions=index;eventCount=count;}
    }
    public synchronized void result(){result(0);}
    public synchronized void result(int position){
        if(!active)return;
        if(!resultRecorded){lastCoins=lastFame=-1;finished++;counts.merge(modeKey,1,Integer::sum);resultRecorded=true;progress=100;operation="结算奖励";}
        if(!isMultiplayer()||position<1||position>8||rankConflict)return;
        if(rank==0){rank=position;ranked++;if(rank<=3)topThree++;}
        else if(rank!=position){ranked--;if(rank<=3)topThree--;rank=0;rankConflict=true;}
    }
    private boolean isMultiplayer(){return modeKey.equals("多人上")||modeKey.equals("多人下");}
    public synchronized void returned(){if(active&&resultRecorded&&!returnRecorded){returned++;returnRecorded=true;active=false;operation="等待下一局";}}
    /** Existing non-multiplayer workflow reports a completed iteration, never a race start. */
    public synchronized void legacyCompleted(){legacyCompleted(-1,-1);}
    public synchronized void legacyCompleted(long gold,long experience){
        if(active&&!isMultiplayer()){result();rewards(gold,experience);returned();}
        else{finished++;returned++;counts.merge(modeKey,1,Integer::sum);lastCoins=lastFame=-1;operation="已返回赛事";}
    }
    public synchronized int finished(){return finished;}
    public synchronized int returnedCount(){return returned;}
    public synchronized int fallbackCount(){return fallbacks;}
    public synchronized int totalActions(){return totalActions;}
    public synchronized int rankedCount(){return ranked;}
    public synchronized int topThreeCount(){return topThree;}
    public synchronized int count(String key){return counts.getOrDefault(key,0);}
    public synchronized String phase(String phase){
        String first=phase==null?"":phase.split("\n",2)[0];
        if(first.contains("暂停"))return "已暂停";
        if(first.contains("正在恢复")||first.contains("正在重启")||first.contains("正在重新启动")||first.contains("确认闪退"))return "恢复游戏";
        if(first.startsWith("任务停止")||first.startsWith("已手动停止")||first.startsWith("启动失败"))return "任务停止";
        if(active&&resultRecorded)return "结算中";
        if(active)return "比赛中";
        if(matchingSeconds>=0)return "匹配中";
        if(first.contains("多人流程：")){
            String scene=first.substring(first.indexOf('：')+1).split("[ ·]",2)[0];
            switch(scene){case "home":case "series":case "event":return "选择赛事";case "cars":case "car":return "选择车辆";case "matching":return "匹配中";case "result":case "rewardChoice":case "rewardClaim":case "rewardConfirm":return "结算中";case "vehicleUnlock":case "milestoneLater":case "promo":return "处理提示";default:return "识别界面";}
        }
        if(first.contains("选车")||first.contains("车库"))return "选择车辆";
        if(first.contains("赛事")||first.contains("杯赛"))return "选择赛事";
        if(first.contains("结算")||first.contains("收益")||first.contains("奖励"))return "结算中";
        if(first.contains("匹配"))return "匹配中";
        if(first.contains("比赛中")||first.contains("选路"))return "比赛中";
        if(first.contains("完成"))return "任务完成";
        return "运行中";
    }
    private String rewardValue(long total,int reads){return finished>0&&reads==0?"未识别":String.valueOf(total);}
    public synchronized String compact(){
        String rate=ranked==0?"—":String.format(Locale.ROOT,"%.1f%%",100.0*topThree/ranked);
        String route=fallback?"左划兜底 · "+fallbackReason:operation.contains("右划")?"右划选路":operation.contains("左划")?"左划选路":eventCount>0?"本地跑法":"等待选路";
        int missingCoins=finished-coinRaces-(active&&resultRecorded&&raceCoins<0?1:0),missingFame=finished-fameRaces-(active&&resultRecorded&&raceFame<0?1:0);
        return (cycle>0?"第 "+cycle+" 轮 · ":"")+mode+(projectTarget>0?" "+Math.max(0,finished-projectBase)+"/"+projectTarget:"")+
            "\n完赛 "+finished+(target>0?"/"+target:"")+" · 多人前三 "+rate+"（"+topThree+"/"+ranked+"）"+
            "\n收益明细：金币："+rewardValue(coins,coinRaces)+"，名人堂："+rewardValue(fame,fameRaces)+
            "\n"+map+"\n"+route+(active&&!resultRecorded?" · "+progress+"%":"")+
            (missingCoins>0||missingFame>0?"\n收益未识别：金币 "+Math.max(0,missingCoins)+" 场 / 名人堂 "+Math.max(0,missingFame)+" 场":"");
    }
    public synchronized String panel(String phase){
        String first=phase==null?"":phase.split("\\n",2)[0];
        if(first.length()>20)first=first.substring(0,20)+"…";
        long seconds=elapsed/1000;
        String time=String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);
        int multi=count("多人上")+count("多人下"),unknown=multi-ranked;
        String rate=ranked==0?"—":String.format(Locale.ROOT,"%.1f%%",100.0*topThree/ranked);
        return (cycle>0?"第 "+cycle+" 轮 · ":"")+mode+
                (projectTarget>0?" "+(finished-projectBase)+"/"+projectTarget:"")+"\n"+first+
                "\n完赛 "+finished+(target>0?"/"+target:"")+" · 已返回 "+returned+
                "\n多人 "+multi+" · 世界 "+count("多人上")+" / 大师 "+count("多人下")+
                "\n多人前三名率 "+rate+"（"+topThree+"/"+ranked+"）"+
                "\n名次待确认 "+unknown+" · 本局 "+(rank>0?"第 "+rank+" 名":"—")+
                "\n杯赛 "+count("杯赛")+" · 生涯 "+count("生涯")+" · 其他 "+(finished-multi-count("杯赛")-count("生涯"))+
                "\n地图："+map+"\n进度 "+progress+"% · 指令 "+actions+"/"+eventCount+
                "\n当前操作："+operation+"\n累计指令 "+totalActions+" · 兜底 "+fallbacks+" 局"+
                (matchingSeconds>=0?"\n匹配等待 "+matchingSeconds+" 秒 / 90 秒":"")+
                "\n运行 "+time+" · 自动重启 "+restarts+" 次"+
                "\n收益明细：金币："+rewardValue(coins,coinRaces)+"，名人堂："+rewardValue(fame,fameRaces)+
                "\n收益覆盖：金币 "+coinRaces+"/"+finished+" 场 · 名人堂 "+fameRaces+"/"+finished+" 场"+
                "\n最近一局：金币 "+(lastCoins<0?"未识别":lastCoins)+" · 名人堂 "+(lastFame<0?"未识别":lastFame)+
                "\n前三名率仅统计已取得名次的多人赛；其他 "+unknown+" 场未取得名次。";
    }
}
