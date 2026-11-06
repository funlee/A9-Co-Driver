package dev.funlee.a9.core;

import java.util.*;

/** Per-start counters. Unknown ranks never silently enter the top-three denominator. */
public final class TaskStats {
    private int target,started,finished,returned,fallbacks,progress,actions,eventCount,totalActions;
    private int cycle,projectTarget,projectBase,rank,ranked,topThree,restarts;
    private long matchingSeconds=-1;
    private long elapsed;
    private boolean active,resultRecorded,returnRecorded,rankConflict;
    private String mode="任务",modeKey="",map="等待识别",operation="等待比赛";
    private final Map<String,Integer> counts=new LinkedHashMap<>();
    public synchronized void target(int count){target=Math.max(0,count);}
    public synchronized void project(String value){map="等待识别";operation="等待比赛";progress=0;actions=0;eventCount=0;rank=0;modeKey=value;mode=MultiplayerUi.displayMode(value).replace("（原多人上）","").replace("（原多人下）","");}
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
        // The previous race no longer owns callbacks after a VM/service restart.
        return s;
    }
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
                (matchingSeconds>=0?"\n匹配等待 "+matchingSeconds+" 秒 / 90 秒":"")+
                "\n运行 "+time+" · 自动重启 "+restarts+" 次";
    }
}
