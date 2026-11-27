package dev.funlee.a9;

import android.content.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import dev.funlee.a9.core.*;

public final class LocalStore {
    static final int DEFAULT_RACE_TIMEOUT_SECONDS = 480;
    public static final String[] MODES={MultiplayerUi.WORLD,MultiplayerUi.MASTER,MultiplayerUi.WEEKEND,"杯赛","特殊赛事","车联"};
    private static final String[] RETIRED_GLOBAL_OPTIONS={
        "coinAD","cupTicketAD","spCupTicketAD","dsTicketAD","onceAD","dsCupAD","AdTime","coinADNum",
        "coinPkg","Coupons","clearPrize","spPrize","spPos"
    };
    private static final String[] RETIRED_PROJECT_OPTIONS={"广告修车","代币买票","买票次数","每日只执行一次"};
    private static final String[] RETIRED_DRIVING_OPTIONS={"杯赛漂流","杯赛两圈赛制","特殊赛事漂流","特殊赛事两圈赛制",MultiplayerUi.LEGACY_WORLD+"漂流",MultiplayerUi.LEGACY_MASTER+"漂流"};
    private static final String[] RETIRED_SERIES_OPTIONS={
        "使用车组","车组选择","车组青铜用车","车组白银用车","车组黄金用车","车组铂金用车","车组钻石用车","车组传奇用车",
        MultiplayerUi.LEGACY_WORLD+"漂流",MultiplayerUi.LEGACY_MASTER+"漂流","传奇停跑","传奇分数",MultiplayerUi.LEGACY_WORLD+"掉分","掉分段位","掉分段位结束","多人选路"
    };
    public final Context context;
    public final MapMatcher matcher;
    public final DigitOcr digits;
    public final LinkedHashMap<String,Route> routes=new LinkedHashMap<>();
    private final Map<String,Route> bundledRoutes=new LinkedHashMap<>();
    private final JSONObject customMaps;
    public final JSONObject forms;
    public JSONObject config;
    public LocalStore(Context context) throws Exception {
        this.context=context.getApplicationContext();
        matcher=new MapMatcher(new StringReader(asset("data/map-rules.tsv")));
        digits=new DigitOcr(new StringReader(asset("data/progress.lib")));
        String[] lines=asset("data/routes.txt").split("\\r?\\n",-1);
        for(int i=1;i+1<lines.length;i+=2) routes.put(lines[i],Route.parse(lines[i],lines[i+1]));
        bundledRoutes.putAll(routes);
        JSONObject custom=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));
        for(Iterator<String> i=custom.keys();i.hasNext();) { String k=i.next(); routes.put(k,Route.parse(k,custom.getString(k))); }
        customMaps=new JSONObject(this.context.getSharedPreferences("local",0).getString("customMaps","{}"));
        for(Iterator<String> i=customMaps.keys();i.hasNext();) {
            String key=i.next();JSONObject map=customMaps.getJSONObject(key);
            matcher.addCustomMap(key,map.getString("track"),map.getString("name"));
            routes.putIfAbsent(key,Route.parse(key,""));
        }
        forms=new JSONObject(asset("data/forms.json"));
        config=new JSONObject(context.getSharedPreferences("local",0).getString("config","{}"));
        boolean queueMigrated=config.optInt("taskQueueVersion",0)<1;
        boolean isNewConfig=!config.has("projects");
        boolean seriesMigrated=queueMigrated&&migrateSeriesTasks(config.optJSONArray("projects"));
        boolean initialized=isNewConfig||(queueMigrated&&isUntouchedLegacyQueue());
        if(initialized)config.put("projects",defaultProjects());
        if(isNewConfig&&!config.has("global"))config.put("global",new JSONObject().put("autoToken",false).put("freePkg",false));
        if(queueMigrated)config.put("taskQueueVersion",1);
        // Persist identity separately from editable task fields and queue position.
        boolean migrated=migrateConfig()||seriesMigrated||initialized||queueMigrated;Set<String> ids=new HashSet<>();
        JSONArray tasks=config.getJSONArray("projects");
        for(int i=0;i<tasks.length();i++) {
            JSONObject task=tasks.getJSONObject(i);String id=task.optString("taskId");
            if(id.isEmpty()||!ids.add(id)) {id=UUID.randomUUID().toString();task.put("taskId",id);ids.add(id);migrated=true;}
        }
        if(migrated && !context.getSharedPreferences("local",0).edit().putString("config",config.toString()).commit())
            throw new IOException("无法保存迁移后的任务配置");
        if(!config.has("gamePkg")) config.put("gamePkg","com.aligames.kuang.kybc.aligames");
        if(!config.has("rotation")) config.put("rotation",90);
        if(!config.has("accuracy")) config.put("accuracy",80);
        if(!config.has("delay")) config.put("delay",0);
        if(!config.has("raceTimeout")) config.put("raceTimeout",DEFAULT_RACE_TIMEOUT_SECONDS);
        if(!config.has("driverMode")) config.put("driverMode",1);
    }
    /** Also runs on a saved session so resuming cannot reactivate retired options. */
    boolean migrateConfig() throws JSONException {
        boolean changed=removeOptions(config.optJSONObject("global"),RETIRED_GLOBAL_OPTIONS);
        JSONArray tasks=config.optJSONArray("projects");
        changed|=migrateSeriesTasks(tasks);
        if(tasks!=null)for(int i=0;i<tasks.length();i++){
            JSONObject task=tasks.getJSONObject(i);
            if("每日战利品".equals(task.optString("项目"))) {
                task.put("项目","杯赛");
                changed=true;
            }
            changed|=removeOptions(task.optJSONObject("data"),RETIRED_PROJECT_OPTIONS);
            if("杯赛".equals(task.optString("项目"))&&task.optJSONObject("data")!=null)
                changed|=task.getJSONObject("data").remove("每日战利品")!=null;
            if("车联".equals(task.optString("项目"))&&task.optJSONObject("data")!=null)
                changed|=task.getJSONObject("data").remove("车手联会选路")!=null
                        |task.getJSONObject("data").remove("杯赛线路5")!=null;
            JSONObject data=task.optJSONObject("data");
            if("特殊赛事".equals(task.optString("项目"))&&data!=null) {
                if(!data.has("赛事模式")) {
                    data.put("赛事模式","0".equals(data.optString("称霸模式"))?"称霸特赛":"经典特赛");
                    changed=true;
                }
                changed|=data.remove("称霸模式")!=null;
            }
            if(hasTaskRoute(task.optString("项目")))changed|=migrateTaskRoute(task.optString("项目"),data);
            if(MultiplayerUi.WORLD.equals(task.optString("项目")))
                changed|=migrateWorldCars(task.optJSONObject("data"));
            changed|=removeOptions(task.optJSONObject("data"),RETIRED_DRIVING_OPTIONS);
            if(MultiplayerUi.SERIES.contains(task.optString("项目")))
                changed|=removeOptions(task.optJSONObject("data"),RETIRED_SERIES_OPTIONS);
        }
        return removeRetiredProjects(config)||changed;
    }
    private static boolean migrateSeriesTasks(JSONArray tasks) throws JSONException {
        boolean changed=false;
        for(int i=0;tasks!=null&&i<tasks.length();i++){
            JSONObject task=tasks.getJSONObject(i);
            String saved=task.optString("项目"), mode=MultiplayerUi.migrateMode(saved);
            if(!mode.equals(saved)){task.put("项目",mode);changed=true;}
            JSONObject data=task.optJSONObject("data");
            if(data==null||!MultiplayerUi.SERIES.contains(mode))continue;
            String name=data.optString("项目名称");
            if(mode.equals(MultiplayerUi.migrateMode(name))&&!mode.equals(name)){
                data.put("项目名称",mode);changed=true;
            }
            String oldCars=MultiplayerUi.LEGACY_MASTER+"用车";
            if(MultiplayerUi.MASTER.equals(mode)&&data.has(oldCars)){
                if(!data.has("赛事用车"))data.put("赛事用车",data.get(oldCars));
                data.remove(oldCars);changed=true;
            }
        }
        return changed;
    }
    public static boolean hasTaskRoute(String mode) {
        return "杯赛".equals(mode)||"特殊赛事".equals(mode);
    }
    public static boolean supportsAutoRoute(String mode) {return hasTaskRoute(mode);}
    private boolean migrateTaskRoute(String mode,JSONObject data)throws JSONException {
        if(data==null)return false;
        String prefix="特殊赛事".equals(mode)?mode:"杯赛";boolean changed=false;
        if(!data.has("路线来源")) {
            String raw=data.optString(prefix+"线路","").trim(),selected="";
            if("4".equals(data.optString(prefix+"选路"))) {
                if(routes.containsKey(raw))selected=raw;
                else if(!raw.isEmpty()) {MapMatcher.Match match=matcher.match(raw);if(match.unique())selected=match.candidates.get(0).internal;}
            }
            String source="2";
            if("特殊赛事".equals(mode)&&!selected.isEmpty())source="0";
            else if(!raw.isEmpty()&&!"4".equals(data.optString(prefix+"选路")))source="1";
            data.put("路线来源",source);
            data.put("已配置地图",selected).put("跑法代码",selected.isEmpty()?raw:"");changed=true;
        }
        for(String key:List.of("已配置地图","跑法代码"))if(!data.has(key)){data.put(key,"");changed=true;}
        if(data.optInt("路线配置版本",0)<1) {
            if(supportsAutoRoute(mode)&&"0".equals(data.optString("路线来源"))&&data.optString("已配置地图").isEmpty()&&data.optString("跑法代码").isEmpty())data.put("路线来源","2");
            data.put("路线配置版本",1);changed=true;
        }
        if("特殊赛事".equals(mode)) {
            String source=data.optString("路线来源");
            if(("1".equals(source)&&data.optString("跑法代码").trim().isEmpty())||
                    ("0".equals(source)&&data.optString("已配置地图").isEmpty())) {
                data.put("路线来源","2");changed=true;
            }
        }
        changed|=data.remove(prefix+"选路")!=null;changed|=data.remove(prefix+"线路")!=null;
        return changed;
    }
    /** Empty values may be saved as drafts; configured values must be executable. */
    public void validateTaskRoute(JSONObject data)throws JSONException {
        if("2".equals(data.optString("路线来源")))return;
        if("0".equals(data.optString("路线来源","0"))) {
            String key=data.optString("已配置地图");if(key.isEmpty())return;
            Route route=routes.get(key);
            if(route==null||!route.valid())throw new IllegalArgumentException("所选地图暂无有效跑法，请先在地图配置中完善");
        } else {
            String code=data.optString("跑法代码");if(code.trim().isEmpty())return;
            Route route=Route.parse("自定义跑法",code);
            if(!route.valid())throw new IllegalArgumentException(String.join("；",route.errors));
        }
    }
    /** Preserve explicit choices, including an empty list, when upgrading the world leagues. */
    private boolean migrateWorldCars(JSONObject settings) throws JSONException {
        if(settings==null)return false;
        boolean changed=false;
        if(settings.has("铂金用车")) {
            if(!settings.has("白金用车"))settings.put("白金用车",settings.get("铂金用车"));
            settings.remove("铂金用车");changed=true;
        }
        JSONArray spec=forms.getJSONArray(MultiplayerUi.WORLD);
        for(int i=0;i<spec.length();i++) {
            JSONObject field=spec.getJSONObject(i);String key=field.getString("key");
            if((key.endsWith("用车")||key.equals("选车筛选"))&&!settings.has(key)) {
                settings.put(key,field.getString("value"));changed=true;
            }
        }
        return changed;
    }
    /** Retire old task slots without shifting the saved queue position or enabling a replacement. */
    public boolean removeRetiredProjects(JSONObject settings) throws JSONException {
        boolean changed=false;JSONArray tasks=settings.optJSONArray("projects");
        if(tasks==null)return false;
        for(int i=0;i<tasks.length();i++) {
            JSONObject task=tasks.getJSONObject(i);
            if("生涯".equals(task.optString("项目"))) {
                task.put("enabled",false).put("项目",MODES[0]);
                task.put("data",defaults(MODES[0]).put("项目名称","未配置任务"));
                changed=true;
            }
        }
        return changed;
    }
    private static boolean removeOptions(JSONObject settings,String[] keys) {
        if(settings==null)return false;
        boolean changed=false;
        for(String key:keys)changed|=settings.remove(key)!=null;
        return changed;
    }
    public DailyTaskPolicy dailyTaskPolicy() {
        return new DailyTaskPolicy(java.time.Clock.systemDefaultZone());
    }
    public String asset(String name) throws IOException {
        try(InputStream in=context.getAssets().open(name); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n); return out.toString("UTF-8");
        }
    }
    public JSONArray formFields(String mode) throws JSONException {
        return forms.getJSONArray(MultiplayerUi.usesSeriesCars(mode)?MultiplayerUi.MASTER:mode);
    }
    public JSONObject defaults(String mode) throws JSONException {
        JSONObject o=new JSONObject(); JSONArray f=formFields(mode);
        for(int i=0;i<f.length();i++) { JSONObject x=f.getJSONObject(i);o.put(x.getString("key"),x.getString("value")); }
        if(hasTaskRoute(mode))o.put("已配置地图","").put("跑法代码","").put("路线配置版本",1);
        if(MultiplayerUi.SERIES.contains(mode))o.put("项目名称",mode);
        o.put("次数","1");
        // Standard modes infer map names; cup-like modes use a user-selected route or OCR.
        return o;
    }
    public void save() { context.getSharedPreferences("local",0).edit().putString("config",config.toString()).apply(); }
    private JSONObject newProject(String mode,JSONObject data) throws JSONException {
        if(!Arrays.asList(MODES).contains(mode))throw new IllegalArgumentException("任务类型无效");
        return new JSONObject().put("taskId",UUID.randomUUID().toString()).put("项目",mode)
                .put("enabled",false).put("data",new JSONObject(data.toString()));
    }
    private JSONArray defaultProjects() throws JSONException,IOException {
        JSONArray template=new JSONArray(asset("data/default-projects.json")),projects=new JSONArray();
        for(int i=0;i<template.length();i++) {
            JSONObject task=template.getJSONObject(i);
            projects.put(newProject(task.getString("项目"),task.getJSONObject("data")).put("enabled",task.getBoolean("enabled")));
        }
        return projects;
    }
    /** Only replace the old untouched placeholders; custom queues, including empty ones, survive upgrades. */
    private boolean isUntouchedLegacyQueue() throws JSONException {
        JSONArray projects=config.optJSONArray("projects");
        if(projects==null||projects.length()!=20)return false;
        JSONObject defaults=defaults(MODES[0]);
        for(int i=0;i<projects.length();i++) {
            JSONObject project=projects.optJSONObject(i);
            if(project==null||!MODES[0].equals(project.optString("项目"))||project.optBoolean("enabled"))return false;
            JSONObject data=project.optJSONObject("data");
            if(data==null)return false;
            JSONObject retained=new JSONObject(data.toString());removeOptions(retained,RETIRED_SERIES_OPTIONS);migrateWorldCars(retained);
            if(!javaValue(defaults).equals(javaValue(retained)))return false;
        }
        return true;
    }
    public void addProject(String mode,JSONObject data) throws JSONException {
        config.getJSONArray("projects").put(newProject(mode,data));save();
    }
    public void deleteProject(int index) throws JSONException {
        JSONArray projects=config.getJSONArray("projects");
        if(index<0||index>=projects.length())throw new IllegalArgumentException("任务位置无效");
        projects.remove(index);save();
    }
    public void resetProjects() throws JSONException,IOException {
        config.put("projects",defaultProjects());save();
    }
    public int defaultProjectCount() {
        try{return new JSONArray(asset("data/default-projects.json")).length();}catch(Exception e){return 0;}
    }
    public void moveProject(int from,int to) throws JSONException {
        JSONArray projects=config.getJSONArray("projects");
        if(from<0||to<0||from>=projects.length()||to>=projects.length())throw new IllegalArgumentException("任务位置无效");
        if(from==to)return;
        Object moved=projects.get(from);
        if(from<to)for(int i=from;i<to;i++)projects.put(i,projects.get(i+1));
        else for(int i=from;i>to;i--)projects.put(i,projects.get(i-1));
        projects.put(to,moved);save();
    }
    public void saveRoute(String key,String raw) throws JSONException {
        Route route=Route.parse(key,raw); if(!route.valid()&&!(isCustomMap(key)&&route.raw.isEmpty())) throw new IllegalArgumentException(String.join("；",route.errors));
        JSONObject all=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));
        all.put(key,raw); context.getSharedPreferences("local",0).edit().putString("routes",all.toString()).apply(); routes.put(key,route);
    }
    public boolean isCustomMap(String key) { return customMaps.has(key); }
    public String customMapTrack(String key) { return customMaps.optJSONObject(key).optString("track"); }
    public String addCustomMap(String track,String name,String raw) throws JSONException {
        track=track.trim();name=name.trim();
        if(MapMatcher.normalize(track).isEmpty())throw new IllegalArgumentException("请填写赛道名称");
        if(MapMatcher.normalize(name).isEmpty())throw new IllegalArgumentException("请填写地图名称");
        String display=MapMatcher.normalize(track+name);
        for(MapMatcher.Rule rule:matcher.rules)
            if(display.equals(MapMatcher.normalize(rule.display))||display.equals(MapMatcher.normalize(rule.internal)))
                throw new IllegalArgumentException("该赛道下已存在同名地图，请直接编辑已有地图");
        String key="custom:"+UUID.randomUUID();Route route=Route.parse(key,raw);
        if(!route.raw.isEmpty()&&!route.valid())throw new IllegalArgumentException(String.join("；",route.errors));
        JSONObject maps=new JSONObject(customMaps.toString());
        JSONObject map=new JSONObject().put("track",track).put("name",name);maps.put(key,map);
        JSONObject all=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));all.put(key,route.raw);
        if(!context.getSharedPreferences("local",0).edit().putString("customMaps",maps.toString()).putString("routes",all.toString()).commit())
            throw new IllegalStateException("无法保存自定义地图，请重试");
        customMaps.put(key,map);routes.put(key,route);matcher.addCustomMap(key,track,name);return key;
    }
    public void deleteCustomMap(String key) throws JSONException {
        if(!isCustomMap(key))throw new IllegalArgumentException("只能删除自定义地图");
        JSONArray projects=config.optJSONArray("projects");
        for(int i=0;projects!=null&&i<projects.length();i++){
            JSONObject project=projects.getJSONObject(i),data=project.optJSONObject("data");
            if(data!=null&&key.equals(data.optString("已配置地图")))
                throw new IllegalArgumentException("任务“"+data.optString("项目名称",project.optString("项目"))+"”仍引用此地图，请先在任务配置中更换地图后再删除");
        }
        JSONObject maps=new JSONObject(customMaps.toString());maps.remove(key);
        JSONObject all=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));all.remove(key);
        if(!context.getSharedPreferences("local",0).edit().putString("customMaps",maps.toString()).putString("routes",all.toString()).commit())
            throw new IllegalStateException("无法删除自定义地图，请重试");
        customMaps.remove(key);routes.remove(key);matcher.rules.removeIf(rule->rule.internal.equals(key));
    }
    public void resetRoute(String key) throws JSONException {
        Route original=bundledRoutes.get(key);
        if(original==null)throw new IllegalArgumentException("未找到该地图的内置跑法");
        JSONObject all=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));
        all.remove(key);
        context.getSharedPreferences("local",0).edit().putString("routes",all.toString()).apply();
        routes.put(key,original);
    }
    public List<String> routeKeys() {
        Set<String> catalog=new LinkedHashSet<>();for(MapMatcher.Rule rule:matcher.rules)catalog.add(rule.internal);
        List<String> keys=new ArrayList<>(catalog); keys.sort(Comparator.comparing(matcher::displayName)); return keys;
    }
    public static Object javaValue(Object o) throws JSONException {
        if(o==JSONObject.NULL) return null;
        if(o instanceof JSONObject) {
            Map<String,Object> m=new LinkedHashMap<>(); JSONObject j=(JSONObject)o;
            for(Iterator<String> i=j.keys();i.hasNext();) { String k=i.next();m.put(k,javaValue(j.get(k))); } return m;
        }
        if(o instanceof JSONArray) {
            List<Object> l=new ArrayList<>(); JSONArray a=(JSONArray)o; for(int i=0;i<a.length();i++)l.add(javaValue(a.get(i))); return l;
        }
        return o;
    }
    @SuppressWarnings("unchecked") public Map<String,Object> luaConfig() throws Exception {
        Map<String,Object> c=new LinkedHashMap<>();
        // Empty string is the workflow's disabled-checkbox representation.
        String[] flags={"getCarInfo","freePkg","autoToken","checkAuto","starWay","rest","setRestTime","closeGame","testTrack","track","adjustDist","rebootShot"};
        JSONObject global=config.optJSONObject("global");
        if(global==null)global=new JSONObject();
        for(String key:flags)c.put(key,global.optBoolean(key)?"0":"");
        c.put("gamePkg",config.getString("gamePkg")); c.put("picCheck","0");
        c.put("delay",config.optInt("delay",0)); c.put("accuracy",config.optInt("accuracy",80));
        c.put("ocrMode",1); c.put("driverMode",config.optInt("driverMode",1)); c.put("conflictTime",30);
        c.put("routeOnly",config.optString("routeOnly",""));
        List<Object> projects=new ArrayList<>(), active=new ArrayList<>();
        JSONArray a=config.getJSONArray("projects");
        for(int i=0;i<a.length();i++) {
            JSONObject p=a.getJSONObject(i); Map<String,Object> item=(Map<String,Object>)javaValue(p);
            item.put("项目显示名",MultiplayerUi.displayMode(p.optString("项目")));
            Map<String,Object> d=(Map<String,Object>)item.get("data");
            if(MultiplayerUi.WORLD.equals(p.optString("项目"))&&"0".equals(d.get("自选奖励"))) {
                List<Object> rewardRules=new ArrayList<>();
                DynamicRewards.rules(String.valueOf(d.getOrDefault("自选奖励规则",""))).forEach((score,position)->
                        rewardRules.add(Map.of("score",score,"position",position)));
                d.put("worldRewardRules",rewardRules);
            }
            for(String key:new ArrayList<>(d.keySet())) if(key.endsWith("用车") && d.get(key) instanceof String) {
                String raw=(String)d.get(key); d.put(key,raw.isEmpty()?List.of("0"):Arrays.asList(raw.replace('，',',').split(",")));
            }
            projects.add(item); active.add(p.optBoolean("enabled",false));
        }
        c.put("projects",projects); c.put("ActiveProj",active); return c;
    }
}
