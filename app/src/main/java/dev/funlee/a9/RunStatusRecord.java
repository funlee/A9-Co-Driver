package dev.funlee.a9;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps the last lifecycle outcome visible when Android recreates the app process. */
final class RunStatusRecord {
    static final int FAILURE_NOTIFICATION=2;
    private RunStatusRecord(){}

    static void save(Context context,boolean active,String status){
        preferences(context).edit().putBoolean("active",active).putString("status",status).apply();
    }

    static String idleStatus(Context context){
        SharedPreferences saved=preferences(context);
        if(saved.getBoolean("active",false))return "上次任务异常中断，请重新开始或续跑";
        return saved.getString("status","尚未开始任务");
    }

    static void recordFailure(Context context,String game,String message){
        preferences(context).edit().putString("failureGame",game).putString("failureMessage",message).apply();
    }
    static String failureMessage(Context context){return preferences(context).getString("failureMessage","");}
    static String legacyScreenshotReason(Context context){
        String notice=failureMessage(context);
        if(notice.contains("异常界面已截图保存"))return dev.funlee.a9.core.RunIssue.reason(notice);
        return preferences(context).getString("legacyScreenshotReason","");
    }
    static String failureGame(Context context){return preferences(context).getString("failureGame","");}
    static void clearFailure(Context context){
        context.getSystemService(android.app.NotificationManager.class).cancel(FAILURE_NOTIFICATION);
        SharedPreferences prefs=preferences(context);
        SharedPreferences.Editor edit=prefs.edit();
        String reason=legacyScreenshotReason(context);
        if(!reason.isEmpty())edit.putString("legacyScreenshotReason",reason);
        edit.remove("failureGame").remove("failureMessage").apply();
    }

    private static SharedPreferences preferences(Context context){
        return context.getSharedPreferences("run-status",Context.MODE_PRIVATE);
    }
}
