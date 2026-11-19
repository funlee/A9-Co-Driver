package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** UI acceptance with temporary preferences, restored verbatim even on failure. No game input. */
public final class SettingsDeviceCheck extends Instrumentation {
    private MainActivity activity;
    private UiAutomation ui;
    private SharedPreferences prefs;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=0;String original=null;boolean touched=false;
        try{
            if(SessionState.running)throw new AssertionError("A task is active");
            prefs=getTargetContext().getSharedPreferences("local",0);original=prefs.getString("config",null);
            JSONObject seed=new JSONObject(original==null?"{}":original);
            JSONObject global=seed.optJSONObject("global");if(global==null)global=new JSONObject();
            global.put("coinAD",true).put("cupTicketAD",true).put("AdTime",42)
                    .put("coinPkg",true).put("Coupons",true).put("spPrize",true)
                    .put("freePkg",true).put("autoToken",false).put("checkAuto",true)
                    .put("settingsTestUnknown","preserve");
            JSONObject taskData=new JSONObject().put("广告修车","0").put("代币买票","0")
                    .put("买票次数","30").put("次数","3");
            seed.put("projects",new JSONArray().put(new JSONObject().put("项目","特殊赛事")
                    .put("enabled",true).put("taskId","settings-fixture").put("data",taskData)));
            seed.put("global",global).put("raceTimeout",900).put("gamePkg","dev.funlee.missing.game");touched=true;
            require(prefs.edit().putString("config",seed.toString()).commit(),"Seed failed");
            ui=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            require(getTargetContext().getPackageManager().getApplicationLabel(getTargetContext().getApplicationInfo()).toString().equals("A9辅助驾驶"),"Launcher name not updated");
            mainClick("偏好设置");screenshot("settings-overview.png");
            runOnMainSync(()->require(activity.getWindow().getDecorView().findViewWithTag("目标游戏")==null,"Task preferences still embedded in settings"));
            mainClick("任务偏好");screenshot("task-preferences.png");
            require(node("每轮任务编排开始前检查一次。\n所有启用的任务执行一遍后，下轮再检查。")!=null,
                    "Reward timing is missing from task preferences");
            recreateActivity();
            runOnMainSync(()->require(activity.getWindow().getDecorView().findViewWithTag("目标游戏")!=null,"Task preferences page not restored"));
            checkGameSelection();
            mainClick("单局时间上限");setInput("0");dialogClick("保存");
            require(config().optInt("raceTimeout")==900&&node("保存")!=null,"Invalid value saved or dialog dismissed");
            screenshot("number-validation.png");setInput("120");dialogClick("保存");require(config().optInt("raceTimeout")==120,"Valid number not persisted");
            mainClick("单局时间上限");setInput("180");dialogClick("取消");require(config().optInt("raceTimeout")==120,"Cancel saved a draft");
            require(node("广告与补给")==null&&node("领取点券")==null&&node("购买金币零件包（消耗金币）")==null,
                    "Retired options are still present");
            require(node("领取免费包").isChecked()&&!node("领取每日目标奖励").isChecked(),
                    "Saved reward values did not reach the inline switches");
            mainClick("领取免费包");
            mainClick("领取每日目标奖励");
            JSONObject after=config().getJSONObject("global");
            require(!after.getBoolean("freePkg")&&after.getBoolean("autoToken")
                    &&after.getBoolean("checkAuto")&&after.getString("settingsTestUnknown").equals("preserve"),
                    "Switches did not save independently");
            require(!after.has("coinAD")&&!after.has("AdTime")&&!after.has("coinPkg")&&!after.has("Coupons")&&!after.has("spPrize"),
                    "Retired global options survived migration");
            JSONObject migratedTask=config().getJSONArray("projects").getJSONObject(0);
            JSONObject migratedData=migratedTask.getJSONObject("data");
            require(migratedTask.getBoolean("enabled")&&migratedTask.getString("taskId").equals("settings-fixture")
                    &&migratedData.getString("次数").equals("3")&&!migratedData.has("广告修车")
                    &&!migratedData.has("代币买票")&&!migratedData.has("买票次数"),"Task migration changed the queue or retained retired options");
            recreateActivity();
            require(!node("领取免费包").isChecked()&&node("领取每日目标奖励").isChecked(),
                    "Reward switches did not retain saved state after recreation");
            mainClick("偏好设置");recreateActivity();
            require(node("运行权限")!=null&&node("高级与帮助")==null&&node("识别与操作参数")==null&&node("开发者工具")==null&&node("第三方运行库许可")==null,"Settings scope changed incorrectly");
            permissionLink("自动操作",android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            permissionLink("悬浮控制",android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            permissionLink("通知控制",android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            screenshot("settings-permissions-only.png");
            result.putString("result","PASS: four primary modules; task preferences isolated and restored; app name; installed game dropdown filters/deduplicates, saves and reloads, dismissed selection unchanged; permission links; numeric validation; save/cancel persistence; inline reward switches persist independently; retired options migrated; advanced/help removed; permission page survives recreation. Original preferences restored.");code=-1;
        }catch(Throwable e){result.putString("error",e.toString());}
        finally{
            if(touched){SharedPreferences.Editor editor=prefs.edit();if(original==null)editor.remove("config");else editor.putString("config",original);if(!editor.commit()){result.putString("restore","FAILED");code=0;}}
        }
        finish(code,result);
    }
    private void checkGameSelection()throws Exception{
        List<InstalledGames.Game> synthetic=Arrays.asList(
                new InstalledGames.Game("test.channel.one","狂野飙车9：竞速传奇"),
                new InstalledGames.Game("test.channel.two","狂野飙车9：竞速传奇"),
                new InstalledGames.Game("test.channel.one","狂野飙车9：竞速传奇"),
                new InstalledGames.Game("test.global","Asphalt Legends"),
                new InstalledGames.Game("test.other","Other app"));
        List<InstalledGames.Game> filtered=InstalledGames.select(synthetic,"");
        require(filtered.size()==3,"Game filtering or package deduplication failed");
        List<String> labels=InstalledGames.labels(filtered);
        require(new HashSet<>(labels).size()==3,"Same-name channels cannot be distinguished");
        require(InstalledGames.select(Collections.singletonList(synthetic.get(4)),"").isEmpty(),"Non-game app included");
        require(InstalledGames.select(Collections.singletonList(synthetic.get(4)),"test.other").size()==1,"Existing launchable custom channel lost");
        require(config().getString("gamePkg").equals("dev.funlee.missing.game"),"Opening selector silently changed the stored package");
        runOnMainSync(()->{
            View field=activity.getWindow().getDecorView().findViewWithTag("目标游戏");
            require(field instanceof Spinner,"Target game is not a dropdown");
            require(((Spinner)field).getSelectedItemPosition()==0,"Missing installation was shown as selected");
        });
        List<InstalledGames.Game> installed=InstalledGames.find(getTargetContext().getPackageManager(),"dev.funlee.missing.game");
        require(!installed.isEmpty(),"This device check requires an installed A9 game");
        mainClick("目标游戏");screenshot("target-game-dropdown.png");
        dialogClick(InstalledGames.labels(installed).get(0));
        String selected=installed.get(0).packageName;
        require(config().getString("gamePkg").equals(selected),"Selected game was not saved");
        Intent launch=getTargetContext().getPackageManager().getLaunchIntentForPackage(selected);
        require(launch!=null&&launch.getComponent().getPackageName().equals(selected),"Saved game does not resolve to its launch activity");
        recreateActivity();
        runOnMainSync(()->{
            Spinner choice=(Spinner)activity.getWindow().getDecorView().findViewWithTag("目标游戏");
            require(choice.getSelectedItemPosition()>0,"Saved selection lost after recreation");
        });
        mainClick("目标游戏");require(ui.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"Could not dismiss dropdown");settle();
        require(config().getString("gamePkg").equals(selected),"Dismissing dropdown changed target");
        screenshot("target-game-selected.png");
    }
    private void permissionLink(String title,String action){
        IntentFilter filter=new IntentFilter(action);
        if(action.equals(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION))filter.addDataScheme("package");
        ActivityMonitor monitor=addMonitor(filter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
        try{mainClick(title);require(monitor.getHits()==1,"Permission row has wrong destination: "+title);}finally{removeMonitor(monitor);}
    }
    private void recreateActivity(){ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(()->activity.recreate());Activity restored=waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);require(restored instanceof MainActivity,"Recreation failed");activity=(MainActivity)restored;waitForIdleSync();}
    private JSONObject config()throws Exception{return new JSONObject(prefs.getString("config","{}"));}
    private void mainClick(String title){runOnMainSync(()->{View view=activity.getWindow().getDecorView().findViewWithTag(title);if(view==null)view=findButton(activity.getWindow().getDecorView(),title);require(view!=null,"Missing main action "+title);view.performClick();});settle();}
    private View findButton(View v,String title){if(v instanceof Button&&((Button)v).getText().toString().equals(title))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findButton(((ViewGroup)v).getChildAt(i),title);if(found!=null)return found;}return null;}
    private void settle(){waitForIdleSync();try{Thread.sleep(200);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private AccessibilityNodeInfo node(String text){AccessibilityNodeInfo root=ui.getRootInActiveWindow();if(root==null)return null;return find(root,text,false);}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String text,boolean input){
        if(input?node.isEditable():text.equals(String.valueOf(node.getText())))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child==null)continue;AccessibilityNodeInfo found=find(child,text,input);if(found!=null)return found;}return null;
    }
    private void dialogClick(String text){AccessibilityNodeInfo found=node(text);require(found!=null,"Missing dialog action "+text);require(found.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Click failed "+text);settle();}
    private void setInput(String value){AccessibilityNodeInfo root=ui.getRootInActiveWindow();AccessibilityNodeInfo input=find(root,"",true);require(input!=null,"Missing input");Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);require(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Set text failed");settle();}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void screenshot(String name)throws Exception{
        settle();android.graphics.Bitmap bitmap=ui.takeScreenshot();File folder=new File(getTargetContext().getExternalFilesDir(null),"settings-check");folder.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(folder,name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
}
