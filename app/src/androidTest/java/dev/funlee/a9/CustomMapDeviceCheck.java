package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import dev.funlee.a9.core.MapMatcher;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Real save/reload and UI checks using an isolated catalog; never starts automation. */
final class CustomMapDeviceCheck {
    private final Instrumentation test;
    private MainActivity activity;
    private UiAutomation ui;
    private Context isolated;
    private CustomMapDeviceCheck(Instrumentation test){this.test=test;}
    static void run(Instrumentation test,Bundle report)throws Exception {new CustomMapDeviceCheck(test).run(report);}
    private void run(Bundle report)throws Exception {
        Context base=test.getTargetContext();String preferences="custom-map-check-"+UUID.randomUUID();
        isolated=new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(preferences,mode);}
        };
        try {
            LocalStore store=new LocalStore(isolated);int original=store.routeKeys().size();
            reject(store,"","新图","");reject(store,"新赛道","  ","");reject(store,"新赛道","新图","bad");
            MapMatcher.Rule builtIn=store.matcher.rules.get(0);String shortName=builtIn.aliases.get(0);
            String track=builtIn.display.substring(0,builtIn.display.length()-shortName.length());
            reject(store,track,shortName,"");
            String key=store.addCustomMap("测试海岸","灯塔环线","");
            require(store.isCustomMap(key)&&!store.isCustomMap(builtIn.internal),"Wrong provenance");
            reject(store," 测试海岸 "," 灯塔环线 ","");
            require(store.routeKeys().size()==original+1,"Invalid map changed catalog");
            store=new LocalStore(isolated);
            require(store.routeKeys().contains(key)&&store.routes.get(key).raw.isEmpty(),"Empty route lost on reload");
            require(store.matcher.match("地图 测试海岸灯塔环线 1圈").unique(),"Full custom name not recognized");
            require(store.matcher.match("灯塔环线 1圈").candidates.get(0).internal.equals(key),"Short custom name not recognized");
            store.saveRoute(key,"1,31,50,21");store.saveRoute(builtIn.internal,"2,21");store=new LocalStore(isolated);
            require(store.routes.get(key).valid()&&!store.isCustomMap(builtIn.internal),"Route edit changed provenance");
            store.resetRoute(builtIn.internal);
            store.saveRoute(key,"");require(new LocalStore(isolated).routes.get(key).raw.isEmpty(),"Custom route cannot return to fallback");
            store.addCustomMap("另一海岸","灯塔环线","1,31");
            require(!store.matcher.match("灯塔环线 1圈").unique(),"Ambiguous custom names were guessed");
            require(store.matcher.match("测试海岸灯塔环线 1圈").unique(),"Full custom name is ambiguous");
            String disposable=store.addCustomMap("删除测试赛道","删除测试地图","1,31");
            store.config.getJSONArray("projects").getJSONObject(0).getJSONObject("data").put("已配置地图",disposable);store.save();
            try{store.deleteCustomMap(disposable);throw new AssertionError("Referenced map deleted");}
            catch(IllegalArgumentException expected){require(expected.getMessage().contains("仍引用此地图"),"Missing dependency guidance");}
            require(new LocalStore(isolated).isCustomMap(disposable),"Rejected deletion changed saved catalog");
            store.config.getJSONArray("projects").getJSONObject(0).getJSONObject("data").put("已配置地图","");store.save();
            store.deleteCustomMap(disposable);
            require(!store.routeKeys().contains(disposable)&&!store.routes.containsKey(disposable)&&store.matcher.match("删除测试赛道删除测试地图").candidates.isEmpty(),"Deleted map remains in memory");
            store=new LocalStore(isolated);
            require(!store.isCustomMap(disposable)&&!store.routes.containsKey(disposable)&&!store.routeKeys().contains(disposable),"Deleted map returned after reload");
            String recreated=store.addCustomMap("删除测试赛道","删除测试地图","");store.deleteCustomMap(recreated);
            try{store.deleteCustomMap(builtIn.internal);throw new AssertionError("Built-in map deleted");}catch(IllegalArgumentException expected){}
            ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            activity=(MainActivity)test.startActivitySync(new Intent(base,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            test.runOnMainSync(()->{
                try{Field f=MainActivity.class.getDeclaredField("store");f.setAccessible(true);f.set(activity,new LocalStore(isolated));}
                catch(Exception e){throw new AssertionError(e);}
                mainClick("地图配置");
            });settle();screenshot("map-config-overview.png");
            clickMain("新增自定义地图");screenshot("custom-map-form.png");
            click("保存");require(node("请填写赛道名称")!=null,"Empty form dismissed or accepted");
            input("赛道名称",track);input("地图名称",shortName);click("保存");
            require(node("该赛道下已存在同名地图，请直接编辑已有地图")!=null,"Duplicate built-in accepted");
            input("地图名称","晨曦海湾");input("路线代码","bad");click("保存");
            require(node("保存")!=null&&new LocalStore(isolated).routeKeys().size()==original+2,"Invalid route saved");
            input("路线代码","");click("保存");
            require(new LocalStore(isolated).routeKeys().size()==original+3,"UI save did not persist");
            test.runOnMainSync(()->{
                View root=activity.getWindow().getDecorView();
                require(findText(root,"自定义")!=null&&findText(root,"晨曦海湾")!=null,"Saved map/tag not shown");
                try {
                    LocalStore current=new LocalStore(isolated);String savedKey=null;
                    for(MapMatcher.Rule rule:current.matcher.rules)if(rule.display.equals(track+"晨曦海湾"))savedKey=rule.internal;
                    View card=root.findViewWithTag("map:"+savedKey);require(card!=null,"Missing saved map card");card.performClick();
                }catch(Exception e){throw new AssertionError(e);}
            });settle();
            require(node("恢复默认")==null,"Custom map offers built-in reset");click("关闭");
            scrollDown();screenshot("custom-map-created.png");
            test.runOnMainSync(()->{
                try{
                    set("routeSelectedTrack",track);set("routeSelectedMap","");
                    Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(activity);
                    View root=activity.getWindow().getDecorView();require(findText(root,"系统内置")!=null&&findText(root,"自定义")!=null,"Mixed catalog tags missing");
                }catch(Exception e){throw new AssertionError(e);}
            });settle();scrollDown();screenshot("map-source-tags.png");
            String savedKey=null;for(MapMatcher.Rule rule:new LocalStore(isolated).matcher.rules)if(rule.display.equals(track+"晨曦海湾"))savedKey=rule.internal;
            require(savedKey!=null,"Missing map before deletion");openRoute(savedKey);
            require(node("删除地图")!=null,"Custom map delete action missing");click("删除地图");screenshot("custom-map-delete-confirmation.png");
            require(new LocalStore(isolated).isCustomMap(savedKey),"Map deleted before confirmation");
            click("取消");require(node("保存")!=null&&new LocalStore(isolated).isCustomMap(savedKey),"Cancel did not preserve editor/map");
            click("删除地图");click("确认删除");
            require(node("保存")==null&&new LocalStore(isolated).routeKeys().size()==original+2,"UI deletion did not persist/close editor");
            String deletedKey=savedKey;
            test.runOnMainSync(()->require(activity.getWindow().getDecorView().findViewWithTag("map:"+deletedKey)==null,"Deleted map card remains visible"));
            screenshot("custom-map-deleted.png");openRoute(builtIn.internal);
            require(node("删除地图")==null&&node("恢复默认")!=null,"Built-in actions changed");click("关闭");
            report.putString("result","PASS: custom map validation/save/edit; deletion confirmation/cancel/persistence; route and recognition cleanup; re-add same name; referenced-map and built-in deletion protection; isolated preferences only");
        } finally {
            if(activity!=null)test.runOnMainSync(()->activity.finish());base.deleteSharedPreferences(preferences);
        }
    }
    private void openRoute(String key){test.runOnMainSync(()->{
        try{Method editor=MainActivity.class.getDeclaredMethod("routeEditor",String.class);editor.setAccessible(true);editor.invoke(activity,key);}
        catch(Exception e){throw new AssertionError(e);}
    });settle();}
    private void scrollDown(){test.runOnMainSync(()->{try{Field f=MainActivity.class.getDeclaredField("pageScroll");f.setAccessible(true);((ScrollView)f.get(activity)).fullScroll(View.FOCUS_DOWN);}catch(Exception e){throw new AssertionError(e);}});settle();}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    private void reject(LocalStore store,String track,String name,String raw)throws Exception{
        try{store.addCustomMap(track,name,raw);throw new AssertionError("Accepted invalid map: "+track+name);}catch(IllegalArgumentException expected){}
    }
    private void clickMain(String text){test.runOnMainSync(()->mainClick(text));settle();}
    private void mainClick(String text){View view=findText(activity.getWindow().getDecorView(),text);require(view!=null,"Missing main action "+text);view.performClick();}
    private View findText(View view,String text){
        if(view instanceof TextView&&text.equals(((TextView)view).getText().toString()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;
    }
    private AccessibilityNodeInfo node(String text){return find(ui.getRootInActiveWindow(),text);}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String text){
        if(node==null)return null;if(text.equals(String.valueOf(node.getText()))||text.equals(String.valueOf(node.getContentDescription())))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=find(node.getChild(i),text);if(found!=null)return found;}return null;
    }
    private void click(String text){AccessibilityNodeInfo node=node(text);require(node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Missing dialog action "+text);settle();}
    private void input(String name,String value){AccessibilityNodeInfo input=node(name);require(input!=null,"Missing input "+name);if(!input.isEditable())input=findInput(ui.getRootInActiveWindow(),name);require(input!=null,"Missing editable "+name);Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);require(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Input failed "+name);settle();}
    private AccessibilityNodeInfo findInput(AccessibilityNodeInfo node,String name){if(node==null)return null;if(node.isEditable()&&name.equals(String.valueOf(node.getContentDescription())))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=findInput(node.getChild(i),name);if(found!=null)return found;}return null;}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(200);}
    private void screenshot(String name)throws Exception{
        settle();android.graphics.Bitmap bitmap=ui.takeScreenshot();File dir=new File(test.getTargetContext().getExternalFilesDir(null),"custom-map-check");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
