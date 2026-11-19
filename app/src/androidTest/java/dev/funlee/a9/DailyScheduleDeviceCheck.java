package dev.funlee.a9;

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
            daily.put("taskId",key);daily.getJSONObject("data").put("每日只执行一次","0").put("杯赛漂流","0").put("杯赛两圈赛制","0").put("次数","5");
            JSONObject cup=first.config.getJSONArray("projects").getJSONObject(2);
            cup.getJSONObject("data").put("每日战利品","0").put("杯赛漂流","0").put("杯赛两圈赛制","0");JSONObject expectedCup=new JSONObject(cup.toString());expectedCup.getJSONObject("data").remove("每日战利品");expectedCup.getJSONObject("data").remove("杯赛漂流");expectedCup.getJSONObject("data").remove("杯赛两圈赛制");String cupBefore=expectedCup.toString();first.save();
            history.edit().putString(key,LocalDate.now().toString()).commit();
            LocalStore recreated=new LocalStore(context);
            JSONObject migrated=recreated.config.getJSONArray("projects").getJSONObject(0);
            if(migrated.getJSONObject("data").has("每日只执行一次")||!migrated.getJSONObject("data").getString("次数").equals("5"))
                throw new AssertionError("Old daily-once setting was not removed independently");
            checkRemovedDailyOptions(migrated);
            if(!cupBefore.equals(recreated.config.getJSONArray("projects").getJSONObject(2).toString()))throw new AssertionError("Cup cleanup changed retained settings");
            JSONArray cupForm=recreated.forms.getJSONArray("杯赛");
            for(int i=0;i<cupForm.length();i++)if("每日战利品".equals(cupForm.getJSONObject(i).getString("key")))throw new AssertionError("Cup still exposes daily loot option");
            JSONObject savedConfig=recreated.config;
            recreated.config=new JSONObject().put("projects",new JSONArray().put(new JSONObject(cup.toString())));
            if(!recreated.migrateConfig()||recreated.config.getJSONArray("projects").getJSONObject(0).getJSONObject("data").has("每日战利品"))throw new AssertionError("Cup resume retains daily loot option");
            recreated.config=savedConfig;
            String reason=recreated.dailyTaskPolicy().skipReason();
            if(!reason.isEmpty()&&!reason.equals("dailyQuietHours"))throw new AssertionError("Old completion still blocks task");
            recreated.config=new JSONObject().put("projects",new JSONArray().put(new JSONObject(daily.toString())));
            if(!recreated.migrateConfig()||recreated.config.getJSONArray("projects").getJSONObject(0).getJSONObject("data").has("每日只执行一次"))
                throw new AssertionError("Saved session retains retired daily-once setting");
            checkRemovedDailyOptions(recreated.config.getJSONArray("projects").getJSONObject(0));
            if(!first.config.getJSONArray("projects").getJSONObject(0).getString("taskId")
                    .equals(recreated.config.getJSONArray("projects").getJSONObject(0).getString("taskId")))
                throw new AssertionError("Task identity changed on recreation");
            JSONArray form=recreated.forms.getJSONArray("每日战利品");boolean found=false;
            for(int i=0;i<form.length();i++)if(java.util.List.of("每日只执行一次","杯赛漂流","杯赛两圈赛制").contains(form.getJSONObject(i).getString("key")))found=true;
            if(found)throw new AssertionError("Retired daily field still available");
            checkAllDrivingOptions(context);
            report.putString("result","PASS: daily-once/drift/two-lap settings removed from config and resume snapshot; cup driving options removed; old history cannot block daily task; task identity/count preserved; retired fields removed; isolated preferences; no game input");
        } finally {base.deleteSharedPreferences(preferences+"-local");base.deleteSharedPreferences(preferences+"-daily-task-history");}
    }
    private static void checkAllDrivingOptions(Context context)throws Exception {
        LocalStore store=new LocalStore(context);
        var retired=java.util.List.of("杯赛漂流","杯赛两圈赛制","特殊赛事漂流","特殊赛事两圈赛制","多人上漂流","多人下漂流");
        JSONArray tasks=new JSONArray(),expected=new JSONArray();
        for(String mode:LocalStore.MODES) {
            JSONObject data=store.defaults(mode).put("次数","7");
            JSONObject task=new JSONObject().put("项目",mode).put("taskId",mode).put("enabled",true).put("data",data);
            expected.put(new JSONObject(task.toString()));
            for(String key:retired)data.put(key,"0");tasks.put(task);
            JSONArray form=store.forms.getJSONArray(mode);
            for(int i=0;i<form.length();i++)if(retired.contains(form.getJSONObject(i).getString("key")))throw new AssertionError("Retired driving field visible: "+mode);
        }
        store.config.put("projects",tasks);JSONObject snapshot=new JSONObject(store.config.toString());store.save();
        LocalStore reloaded=new LocalStore(context);
        if(!LocalStore.javaValue(expected).equals(LocalStore.javaValue(reloaded.config.getJSONArray("projects"))))throw new AssertionError("Driving migration must preserve all other task settings");
        reloaded.config=snapshot;
        if(!reloaded.migrateConfig()||!LocalStore.javaValue(expected).equals(LocalStore.javaValue(reloaded.config.getJSONArray("projects"))))throw new AssertionError("Saved session retains retired driving modes");
        if(reloaded.migrateConfig())throw new AssertionError("Driving migration must be idempotent");
    }
    private static void checkRemovedDailyOptions(JSONObject task)throws Exception {
        JSONObject data=task.getJSONObject("data");
        if(data.has("杯赛漂流")||data.has("杯赛两圈赛制"))throw new AssertionError("Retired daily driving options survived migration");
    }
}
