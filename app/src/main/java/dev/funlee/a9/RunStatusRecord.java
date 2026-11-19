package dev.funlee.a9;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps the last lifecycle outcome visible when Android recreates the app process. */
final class RunStatusRecord {
    private RunStatusRecord(){}

    static void save(Context context,boolean active,String status){
        preferences(context).edit().putBoolean("active",active).putString("status",status).apply();
    }

    static String idleStatus(Context context){
        SharedPreferences saved=preferences(context);
        if(saved.getBoolean("active",false))return "上次任务异常中断，请重新开始或续跑";
        return saved.getString("status","尚未开始任务");
    }

    private static SharedPreferences preferences(Context context){
        return context.getSharedPreferences("run-status",Context.MODE_PRIVATE);
    }
}
