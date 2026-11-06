package dev.funlee.a9.core;

import java.util.*;

/** Per-start counters. Unknown ranks never silently enter the top-three denominator. */
public final class TaskStats {
    private int target,started,finished,returned,fallbacks,progress,actions,eventCount,totalActions;
    private int cycle,projectTarget,projectBase,rank,ranked,topThree;
    private long elapsed;
    private boolean active,resultRecorded,returnRecorded,rankConflict;
    private String mode="任务",modeKey="",map="等待识别",operation="等待比赛";
    private final Map<String,Integer> counts=new LinkedHashMap<>();
    public synchronized void target(int count){target=Math.max(0,count);}
    public synchronized void project(String value){map="等待识别";operation="等待比赛";progress=0;actions=0;eventCount=0;rank=0;modeKey=value;mode=MultiplayerUi.displayMode(value).replace("（原多人上）","").replace("（原多人下）","");}
    public synchronized void cycle(int n){cycle=n;}
    public synchronized void projectTarget(int n){projectTarget=n;projectBase=finished;}
    public synchronized void elapsed(long ms){elapsed=Math.max(elapsed,ms);}
    public synchronized void operation(String value){operation=value;}
    public synchronized void begin(String displayMap,boolean fallback){
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
        if(!resultRecorded){finished++;counts.merge(modeKey,1,Integer::sum);resultRecorded=true;progress=100;operation="结算奖励";}
        if(!isMultiplayer()||position<1||position>8||rankConflict)return;
        if(rank==0){rank=position;ranked++;if(rank<=3)topThree++;}
        else if(rank!=position){ranked--;if(rank<=3)topThree--;rank=0;rankConflict=true;}
    }
    private boolean isMultiplayer(){return modeKey.equals("多人上")||modeKey.equals("多人下");}
    public synchronized void returned(){if(active&&resultRecorded&&!returnRecorded){returned++;returnRecorded=true;active=false;operation="等待下一局";}}
    /** Existing non-multiplayer workflow reports a completed iteration, never a race start. */
    public synchronized void legacyCompleted(){finished++;returned++;counts.merge(modeKey,1,Integer::sum);operation="已返回赛事";}
    public synchronized int finished(){return finished;}
    public synchronized int returnedCount(){return returned;}
    public synchronized int fallbackCount(){return fallbacks;}
    public synchronized int totalActions(){return totalActions;}
    public synchronized int rankedCount(){return ranked;}
    public synchronized int topThreeCount(){return topThree;}
    public synchronized int count(String key){return counts.getOrDefault(key,0);}
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
                "\n前三名率 "+rate+"（"+topThree+"/"+ranked+"）"+
                "\n名次待确认 "+unknown+" · 本局 "+(rank>0?"第 "+rank+" 名":"—")+
                "\n杯赛 "+count("杯赛")+" · 生涯 "+count("生涯")+" · 其他 "+(finished-multi-count("杯赛")-count("生涯"))+
                "\n地图："+map+"\n进度 "+progress+"% · 指令 "+actions+"/"+eventCount+
                "\n当前操作："+operation+"\n累计指令 "+totalActions+" · 兜底 "+fallbacks+" 局"+
                "\n运行 "+time+" · 自动重启未启用";
    }
}
