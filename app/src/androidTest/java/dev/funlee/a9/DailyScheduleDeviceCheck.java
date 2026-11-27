package dev.funlee.a9;

import dev.funlee.a9.core.MultiplayerUi;

import android.app.Instrumentation;
import android.content.*;
import android.os.Bundle;
import java.time.LocalDate;
import java.util.UUID;
import org.json.*;

/** Exercises real preference persistence only; never launches or sends input to the game. */
final class DailyScheduleDeviceCheck {
    static void run(Instrumentation instrumentation,Bundle report)throws Exception {
        var base=instrumentation.getTargetContext();
        String preferences="daily-schedule-check-"+UUID.randomUUID();
        Context context=new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(preferences+"-"+name,mode);}
        };
        var history=context.getSharedPreferences("daily-task-history",0);
        String key="test-"+UUID.randomUUID();
        try {
            LocalStore first=new LocalStore(context);
            JSONObject retired=new JSONObject().put("项目","生涯").put("taskId","retired-task").put("enabled",true)
                    .put("data",new JSONObject().put("次数","7").put("生涯用车","1,2"));
            JSONObject retained=new JSONObject().put("项目","特殊赛事").put("taskId","retained-task").put("enabled",true)
                    .put("data",first.defaults("特殊赛事"));
            String retainedBefore=retained.toString();
            JSONObject fixture=new JSONObject().put("projects",new JSONArray().put(retired).put(retained));
            if(!first.removeRetiredProjects(fixture)||retired.getBoolean("enabled")||retired.getString("项目").equals("生涯")
                    ||retired.getJSONObject("data").has("生涯用车")||!retired.getString("taskId").equals("retired-task")
                    ||!retained.toString().equals(retainedBefore)||fixture.getJSONArray("projects").length()!=2
                    ||first.removeRetiredProjects(fixture))throw new AssertionError("Retired task migration changed queue or active tasks");
            if(first.forms.has("生涯")||java.util.Arrays.asList(LocalStore.MODES).contains("生涯"))throw new AssertionError("Retired mode remains configurable");
            JSONObject daily=first.config.getJSONArray("projects").getJSONObject(0);
            JSONObject cup=new JSONObject().put("项目","杯赛").put("taskId","ordinary-cup").put("enabled",true)
                    .put("data",first.defaults("杯赛"));
            first.config.put("projects",new JSONArray().put(cup).put(daily));
            daily.put("项目","每日战利品").put("taskId",key).put("enabled",false);
            daily.getJSONObject("data").put("每日只执行一次","0").put("杯赛漂流","0").put("杯赛两圈赛制","0")
                    .put("次数","5").put("杯赛位置","8").put("杯赛用车","2,1").put("路线来源","1").put("跑法代码","0,21");
            JSONObject expectedDaily=new JSONObject(daily.toString()).put("项目","杯赛");
            for(String field:java.util.List.of("每日只执行一次","杯赛漂流","杯赛两圈赛制"))expectedDaily.getJSONObject("data").remove(field);
            cup.getJSONObject("data").put("每日战利品","0").put("杯赛漂流","0").put("杯赛两圈赛制","0");JSONObject expectedCup=new JSONObject(cup.toString());expectedCup.getJSONObject("data").remove("每日战利品");expectedCup.getJSONObject("data").remove("杯赛漂流");expectedCup.getJSONObject("data").remove("杯赛两圈赛制");String cupBefore=expectedCup.toString();first.save();
            history.edit().putString(key,LocalDate.now().toString()).commit();
            LocalStore recreated=new LocalStore(context);
            JSONObject migrated=recreated.config.getJSONArray("projects").getJSONObject(1);
            if(!LocalStore.javaValue(expectedDaily).equals(LocalStore.javaValue(migrated)))
                throw new AssertionError("Merged task lost its identity, enabled state, order, or settings");
            if(!cupBefore.equals(recreated.config.getJSONArray("projects").getJSONObject(0).toString()))throw new AssertionError("Cup cleanup changed retained settings");
            if(recreated.forms.has("每日战利品")||java.util.Arrays.asList(LocalStore.MODES).contains("每日战利品"))
                throw new AssertionError("Daily loot remains a separate task type");
            JSONArray cupForm=recreated.forms.getJSONArray("杯赛");boolean hasClubReward=false;
            for(int i=0;i<cupForm.length();i++){
                JSONObject field=cupForm.getJSONObject(i);
                if("每日战利品".equals(field.getString("key")))throw new AssertionError("Cup exposes the retired daily loot field");
                if("是否领取俱乐部奖励".equals(field.getString("key")))hasClubReward="CheckBox".equals(field.getString("type"));
            }
            if(!hasClubReward||!recreated.defaults("杯赛").getString("是否领取俱乐部奖励").isEmpty())
                throw new AssertionError("Cup club reward checkbox must be available and off by default");
            JSONObject savedConfig=recreated.config;
            recreated.config=new JSONObject().put("projects",new JSONArray().put(new JSONObject(cup.toString())));
            if(!recreated.migrateConfig()||recreated.config.getJSONArray("projects").getJSONObject(0).getJSONObject("data").has("每日战利品"))throw new AssertionError("Cup resume retains daily loot option");
            recreated.config=savedConfig;
            String reason=recreated.dailyTaskPolicy().skipReason();
            if(!reason.isEmpty()&&!reason.equals("dailyQuietHours"))throw new AssertionError("Old completion still blocks task");
            recreated.config=new JSONObject().put("projects",new JSONArray().put(new JSONObject(daily.toString())));
            if(!recreated.migrateConfig()||!LocalStore.javaValue(expectedDaily).equals(
                    LocalStore.javaValue(recreated.config.getJSONArray("projects").getJSONObject(0)))||recreated.migrateConfig())
                throw new AssertionError("Saved session did not migrate idempotently to a cup");
            checkAllDrivingOptions(context);
            report.putString("result","PASS: daily loot merged into cup in saved preferences and session snapshots; task fields and club reward preference preserved; cup checkbox available and off by default; no game input");
        } finally {base.deleteSharedPreferences(preferences+"-local");base.deleteSharedPreferences(preferences+"-daily-task-history");}
    }
    private static void checkAllDrivingOptions(Context context)throws Exception {
        LocalStore store=new LocalStore(context);
        var retired=java.util.List.of("杯赛漂流","杯赛两圈赛制","特殊赛事漂流","特殊赛事两圈赛制",MultiplayerUi.LEGACY_WORLD+"漂流",MultiplayerUi.LEGACY_MASTER+"漂流");
        JSONArray tasks=new JSONArray(),expected=new JSONArray();
        for(String mode:LocalStore.MODES) {
            JSONObject data=store.defaults(mode).put("次数","7");
            JSONObject task=new JSONObject().put("项目",mode).put("taskId",mode).put("enabled",true).put("data",data);
            expected.put(new JSONObject(task.toString()));
            for(String key:retired)data.put(key,"0");tasks.put(task);
            JSONArray form=store.formFields(mode);
            for(int i=0;i<form.length();i++)if(retired.contains(form.getJSONObject(i).getString("key")))throw new AssertionError("Retired driving field visible: "+mode);
        }
        store.config.put("projects",tasks);JSONObject snapshot=new JSONObject(store.config.toString());store.save();
        LocalStore reloaded=new LocalStore(context);
        if(!LocalStore.javaValue(expected).equals(LocalStore.javaValue(reloaded.config.getJSONArray("projects"))))throw new AssertionError("Driving migration must preserve all other task settings");
        reloaded.config=snapshot;
        if(!reloaded.migrateConfig()||!LocalStore.javaValue(expected).equals(LocalStore.javaValue(reloaded.config.getJSONArray("projects"))))throw new AssertionError("Saved session retains retired driving modes");
        if(reloaded.migrateConfig())throw new AssertionError("Driving migration must be idempotent");
    }
}
