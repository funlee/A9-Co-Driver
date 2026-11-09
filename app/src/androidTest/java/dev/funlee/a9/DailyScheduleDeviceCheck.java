package dev.funlee.a9;

import android.app.Instrumentation;
import android.os.Bundle;
import dev.funlee.a9.core.DailyTaskPolicy;
import java.time.LocalDate;
import java.util.UUID;
import org.json.*;

/** Exercises real preference persistence only; never launches or sends input to the game. */
final class DailyScheduleDeviceCheck {
    static void run(Instrumentation instrumentation,Bundle report)throws Exception {
        var context=instrumentation.getTargetContext();
        var history=context.getSharedPreferences("daily-task-history",0);
        String key="test-"+UUID.randomUUID();
        try {
            LocalStore first=new LocalStore(context);
            DailyTaskPolicy policy=first.dailyTaskPolicy();
            policy.completed(key,true,4,10);
            if(history.contains(key))throw new AssertionError("Partial task persisted as complete");
            policy.completed(key,true,10,10);
            String date=LocalDate.now().toString();
            if(!date.equals(history.getString(key,"")))throw new AssertionError("Completion date missing");
            LocalStore recreated=new LocalStore(context);
            String reason=recreated.dailyTaskPolicy().skipReason(key,true);
            if(!reason.equals("dailyAlreadyDone")&&!reason.equals("dailyQuietHours"))throw new AssertionError("Recreated host forgot completion");
            if(!first.config.getJSONArray("projects").getJSONObject(0).getString("taskId")
                    .equals(recreated.config.getJSONArray("projects").getJSONObject(0).getString("taskId")))
                throw new AssertionError("Task identity changed on recreation");
            JSONArray form=recreated.forms.getJSONArray("每日战利品");boolean found=false;
            for(int i=0;i<form.length();i++)if(form.getJSONObject(i).getString("key").equals("每日只执行一次"))found=true;
            if(!found)throw new AssertionError("Daily-once field missing");
            report.putString("result","PASS: completion persists; partial task stays eligible; task identity stable; checkbox available; no game input");
        } finally {history.edit().remove(key).commit();}
    }
}
