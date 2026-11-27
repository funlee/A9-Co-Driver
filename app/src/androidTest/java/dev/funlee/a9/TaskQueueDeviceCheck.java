package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;

/** Real drag gestures against the production UI, backed by disposable task preferences. */
final class TaskQueueDeviceCheck {
    private final Instrumentation test;
    private MainActivity activity;
    private LocalStore store;
    private long down;
    private float x,y;
    private TaskQueueDeviceCheck(Instrumentation test){this.test=test;}
    static void run(Instrumentation test,Bundle report)throws Exception {new TaskQueueDeviceCheck(test).run(report);}
    private void run(Bundle report)throws Exception {
        Context base=test.getTargetContext();String preferences="task-queue-check-"+UUID.randomUUID();
        Context isolated=new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(preferences,mode);}
        };
        try {
            store=new LocalStore(isolated);JSONArray tasks=store.config.getJSONArray("projects");
            while(tasks.length()<20)store.addProject("世界系列赛",store.defaults("世界系列赛"));
            List<String> originals=new ArrayList<>();
            for(int i=0;i<20;i++){
                JSONObject task=tasks.getJSONObject(i);task.put("enabled",i%3==0);
                task.getJSONObject("data").put("项目名称","排序测试 "+(i+1)).put("次数",String.valueOf(i+1));
                originals.add(task.toString());
            }
            store.save();
            Intent launch=new Intent(base,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity=(MainActivity)test.startActivitySync(launch);
            test.runOnMainSync(()->{
                try {
                    Field field=MainActivity.class.getDeclaredField("store");field.setAccessible(true);field.set(activity,store);
                    Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(activity);
                }catch(Exception e){throw new RuntimeException(e);}
            });test.waitForIdleSync();
            // The drag handle must never open the task editor, even on a short click.
            begin(0,false);release();
            checkEditor(false,"Clicking the drag handle opened the editor");
            // A short downward move changes order and preserves every field and enabled flag.
            begin(0,false);moveToRow(1,false);release();
            checkEditor(false,"Quick drag opened the editor");
            check(tasks.getJSONObject(1).toString().equals(originals.get(0)),"Downward move did not preserve task: "+tasks.getJSONObject(0).getJSONObject("data").optString("项目名称"));
            check(tasks.getJSONObject(0).toString().equals(originals.get(1)),"Other task shifted incorrectly");
            begin(1);moveToRow(0,true);release();
            check(tasks.getJSONObject(0).toString().equals(originals.get(0)),"Upward move failed");
            // Cancel outside the list: no saved order changes.
            String before=tasks.toString();begin(0);move(x,400);release();check(tasks.toString().equals(before),"Cancelled drag changed order");
            // Keep the pointer at the viewport edge while the production list scrolls itself.
            begin(0);edge(false);moveToRow(19,false);release();
            check(tasks.getJSONObject(19).toString().equals(originals.get(0)),"Could not drag first task to last: "+taskOrder(tasks));
            LocalStore reloaded=new LocalStore(isolated);
            check(reloaded.config.getJSONArray("projects").toString().equals(tasks.toString()),"Reordered tasks did not persist");
            begin(19);edge(true);moveToRow(0,true);release();
            for(int i=0;i<20;i++)check(tasks.getJSONObject(i).toString().equals(originals.get(i)),"Round trip changed task "+i);
            List<?> runtime=(List<?>)new LocalStore(isolated).luaConfig().get("projects");
            for(int i=0;i<20;i++)check(((Map<?,?>)runtime.get(i)).get("taskId").equals(tasks.getJSONObject(i).getString("taskId")),"Runtime queue order mismatch");
            // Editing remains available on the name, but is separate from the handle.
            float[] detail=new float[2];test.runOnMainSync(()->{
                View view=((ViewGroup)queue(activity.getWindow().getDecorView()).getChildAt(0)).getChildAt(1);
                int[] pos=new int[2];view.getLocationOnScreen(pos);detail[0]=pos[0]+view.getWidth()/2f;detail[1]=pos[1]+view.getHeight()/2f;
            });x=detail[0];y=detail[1];down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,x,y);SystemClock.sleep(30);release();
            checkEditor(true,"Clicking the task name no longer opens the editor");
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);test.waitForIdleSync();
            report.putString("result","PASS: handle click does not open editor; immediate drag without long press; name click edits; upward/cancelled drags; automatic edge scrolling across all 20 tasks; configuration and persisted order preserved; isolated preferences only");
        }finally{
            if(down!=0)release();
            if(activity!=null)test.runOnMainSync(()->activity.finish());
            base.deleteSharedPreferences(preferences);
        }
    }
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private String taskOrder(JSONArray tasks)throws Exception {
        List<String> names=new ArrayList<>();for(int i=0;i<tasks.length();i++)names.add(tasks.getJSONObject(i).getJSONObject("data").getString("项目名称"));return names.toString();
    }
    private TaskQueueView queue(View view){
        if(view instanceof TaskQueueView)return (TaskQueueView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TaskQueueView q=queue(((ViewGroup)view).getChildAt(i));if(q!=null)return q;}
        return null;
    }
    private float[] row(int index,boolean top){
        float[] point=new float[2];test.runOnMainSync(()->{
            View task=queue(activity.getWindow().getDecorView()).getChildAt(index);int[] pos=new int[2];task.getLocationOnScreen(pos);
            point[0]=pos[0]+40;point[1]=pos[1]+(top?4:task.getHeight()-4);
        });return point;
    }
    private void begin(int index){
        begin(index,true);
    }
    private void begin(int index,boolean hold){
        float[] top=row(index,true),bottom=row(index,false);x=top[0];y=(top[1]+bottom[1])/2;
        down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,x,y);SystemClock.sleep(hold?ViewConfiguration.getLongPressTimeout()+250:30);
    }
    private void checkEditor(boolean visible,String message){
        boolean[] focused={false};test.runOnMainSync(()->focused[0]=activity.hasWindowFocus());
        check(focused[0]!=visible,message);
    }
    private void moveToRow(int index,boolean top){float[] point=row(index,top);move(point[0],point[1]);}
    private void move(float toX,float toY){
        float fromX=x,fromY=y;
        for(int i=1;i<=12;i++){x=fromX+(toX-fromX)*i/12;y=fromY+(toY-fromY)*i/12;event(MotionEvent.ACTION_MOVE,x,y);SystemClock.sleep(20);}
    }
    private void release(){
        event(MotionEvent.ACTION_UP,x,y);down=0;test.waitForIdleSync();
        // Android animates a cancelled drag back to its source outside the app's
        // main looper; do not send the next DOWN while that input channel owns it.
        SystemClock.sleep(600);
    }
    private void edge(boolean top){
        float[] point=new float[2];test.runOnMainSync(()->{
            View view=queue(activity.getWindow().getDecorView());while(!(view instanceof ScrollView))view=(View)view.getParent();
            int[] pos=new int[2];view.getLocationOnScreen(pos);point[0]=pos[0]+view.getWidth()/2f;point[1]=pos[1]+(top?12:view.getHeight()-12);
        });move(point[0],point[1]);
        // Software-rendered emulators do not have a fixed frame rate. Wait for
        // the actual boundary instead of dropping at a still offscreen row.
        long deadline=SystemClock.uptimeMillis()+20000;boolean[] remaining={true};
        while(remaining[0]&&SystemClock.uptimeMillis()<deadline){
            SystemClock.sleep(150);test.runOnMainSync(()->{
                View view=queue(activity.getWindow().getDecorView());while(!(view instanceof ScrollView))view=(View)view.getParent();
                remaining[0]=view.canScrollVertically(top?-1:1);
            });
        }
        if(remaining[0]){
            String[] diagnostic={""};test.runOnMainSync(()->{
                try {
                    TaskQueueView q=queue(activity.getWindow().getDecorView());View s=q;while(!(s instanceof ScrollView))s=(View)s.getParent();
                    int[] pos=new int[2];q.getLocationOnScreen(pos);
                    diagnostic[0]="scroll="+s.getScrollY()+", list="+pos[1]+".."+(pos[1]+q.getHeight())+", pointer="+x+","+y;
                    for(String key:List.of("direction","viewportY","slot","dragged")){Field f=TaskQueueView.class.getDeclaredField(key);f.setAccessible(true);diagnostic[0]+=", "+key+"="+f.get(q);}
                }catch(Exception e){diagnostic[0]=e.toString();}
            });
            throw new AssertionError("Automatic edge scrolling did not reach the "+(top?"top":"bottom")+": "+diagnostic[0]);
        }
    }
    private void event(int action,float x,float y){
        MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{check(test.getUiAutomation().injectInputEvent(event,true),"Pointer injection failed");}finally{event.recycle();}
    }
}
