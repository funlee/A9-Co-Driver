package dev.funlee.a9;

import android.content.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import dev.funlee.a9.core.*;

public final class LocalStore {
    public static final String[] MODES={"多人上","多人下","杯赛","特殊赛事","车联","生涯","每日战利品"};
    public final Context context;
    public final MapMatcher matcher;
    public final DigitOcr digits;
    public final LinkedHashMap<String,Route> routes=new LinkedHashMap<>();
    public final JSONObject forms;
    public JSONObject config;
    public LocalStore(Context context) throws Exception {
        this.context=context.getApplicationContext();
        if(!BuildConfig.PRIVATE_ASSETS_ENABLED)throw new IllegalStateException("此公开构建未包含本地私有运行资源");
        matcher=new MapMatcher(new StringReader(asset("data/map-rules.tsv")));
        digits=new DigitOcr(new StringReader(asset("data/progress.lib")));
        String[] lines=asset("data/routes.txt").split("\\r?\\n");
        for(int i=1;i+1<lines.length;i+=2) routes.put(lines[i],Route.parse(lines[i],lines[i+1]));
        JSONObject custom=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));
        for(Iterator<String> i=custom.keys();i.hasNext();) { String k=i.next(); routes.put(k,Route.parse(k,custom.getString(k))); }
        forms=new JSONObject(asset("data/forms.json"));
        config=new JSONObject(context.getSharedPreferences("local",0).getString("config","{}"));
        if(!config.has("projects")) {
            JSONArray projects=new JSONArray();
            for(int i=0;i<20;i++) projects.put(new JSONObject().put("项目",MODES[0]).put("enabled",false).put("data",defaults(MODES[0])));
            config.put("projects",projects);
        }
        if(!config.has("gamePkg")) config.put("gamePkg","com.aligames.kuang.kybc.aligames");
        if(!config.has("rotation")) config.put("rotation",90);
        if(!config.has("accuracy")) config.put("accuracy",80);
        if(!config.has("delay")) config.put("delay",0);
        if(!config.has("raceTimeout")) config.put("raceTimeout",900);
        if(!config.has("driverMode")) config.put("driverMode",1);
    }
    public String asset(String name) throws IOException {
        try(InputStream in=context.getAssets().open(name); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n); return out.toString("UTF-8");
        }
    }
    public JSONObject defaults(String mode) throws JSONException {
        JSONObject o=new JSONObject(); JSONArray f=forms.getJSONArray(mode);
        for(int i=0;i<f.length();i++) { JSONObject x=f.getJSONObject(i);o.put(x.getString("key"),x.getString("value")); }
        o.put("次数","1");
        // Standard modes infer map names; cup-like modes use a user-selected route or OCR.
        return o;
    }
    public void save() { context.getSharedPreferences("local",0).edit().putString("config",config.toString()).apply(); }
    public void saveRoute(String key,String raw) throws JSONException {
        Route route=Route.parse(key,raw); if(!route.valid()) throw new IllegalArgumentException(String.join("；",route.errors));
        JSONObject all=new JSONObject(context.getSharedPreferences("local",0).getString("routes","{}"));
        all.put(key,raw); context.getSharedPreferences("local",0).edit().putString("routes",all.toString()).apply(); routes.put(key,route);
    }
    public List<String> routeKeys() {
        List<String> keys=new ArrayList<>(routes.keySet()); keys.sort(Comparator.comparing(matcher::displayName)); return keys;
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
        // Empty string is the original workflow's disabled-checkbox representation.
        String[] flags={"getCarInfo","coinAD","cupTicketAD","spCupTicketAD","dsTicketAD","onceAD","freePkg","coinPkg","autoToken","Coupons","clearPrize","spPrize","checkAuto","starWay","dsCupAD","rest","setRestTime","closeGame","testTrack","track","adjustDist","rebootShot"};
        for(String key:flags)c.put(key,"");
        JSONObject global=config.optJSONObject("global");
        if(global==null)global=new JSONObject();
        for(String key:flags)if(global.optBoolean(key,false))c.put(key,"0");
        c.put("gamePkg",config.getString("gamePkg")); c.put("picCheck","0"); c.put("AdTime",global.optInt("AdTime",40));
        c.put("coinADNum",global.optInt("coinADNum",1)); c.put("delay",config.optInt("delay",0)); c.put("accuracy",config.optInt("accuracy",80));
        c.put("ocrMode",1); c.put("driverMode",config.optInt("driverMode",1)); c.put("conflictTime",30); c.put("spPos",Arrays.asList(global.optString("spPos","1").split(",")));
        c.put("repeatProjects",config.optBoolean("repeat",false)); c.put("routeOnly",config.optString("routeOnly",""));
        List<Object> projects=new ArrayList<>(), active=new ArrayList<>();
        JSONArray a=config.getJSONArray("projects");
        for(int i=0;i<a.length();i++) {
            JSONObject p=a.getJSONObject(i); Map<String,Object> item=(Map<String,Object>)javaValue(p);
            Map<String,Object> d=(Map<String,Object>)item.get("data");
            for(String key:new ArrayList<>(d.keySet())) if(key.endsWith("用车") && d.get(key) instanceof String) {
                String raw=(String)d.get(key); d.put(key,raw.isEmpty()?List.of("0"):Arrays.asList(raw.replace('，',',').split(",")));
            }
            projects.add(item); active.add(p.optBoolean("enabled",false));
        }
        c.put("projects",projects); c.put("ActiveProj",active); return c;
    }
}
