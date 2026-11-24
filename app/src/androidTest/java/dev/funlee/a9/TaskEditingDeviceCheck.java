package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Queue lifecycle and real editor actions, using disposable preferences. */
final class TaskEditingDeviceCheck {
    private static final int CUP=1, SPECIAL=2, DAILY=3, MASTER=5, WORLD=6;
    private final Instrumentation test;
    private MainActivity activity;
    private LocalStore store;
    private Context isolated;
    private UiAutomation ui;
    private TaskEditingDeviceCheck(Instrumentation test){this.test=test;}
    static void run(Instrumentation test,Bundle report)throws Exception {new TaskEditingDeviceCheck(test).run(report);}
    private void run(Bundle report)throws Exception {
        Context base=test.getTargetContext();String preferences="task-editing-check-"+UUID.randomUUID();
        isolated=new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(preferences,mode);}
        };
        try {
            store=new LocalStore(isolated);checkDefaults();
            check(store.config.getInt("raceTimeout")==900,"Wrong initial race timeout");
            check(store.config.getJSONObject("global").getBoolean("autoToken")&&store.config.getJSONObject("global").getBoolean("freePkg"),"Wrong initial global task options");
            String initial=tasks().toString();reload();check(initial.equals(tasks().toString()),"Default identities did not persist");
            check(store.config.getJSONObject("global").getBoolean("autoToken")&&store.config.getJSONObject("global").getBoolean("freePkg"),"Default global task options did not persist");
            JSONObject initialConfig=new JSONObject(store.config.toString());
            store.config=new JSONObject().put("global",new JSONObject().put("autoToken",false).put("freePkg",false));store.save();reload();checkDefaults();
            check(!store.config.getJSONObject("global").getBoolean("autoToken")&&!store.config.getJSONObject("global").getBoolean("freePkg"),"Initialization overwrote explicit global options");
            store.config=new JSONObject(initialConfig.toString());store.config.remove("global");store.save();reload();
            check(!store.config.has("global"),"Existing configuration adopted enabled reward defaults");
            store.config=initialConfig;store.save();reload();
            // An untouched legacy queue upgrades; a single edited or enabled slot preserves all 20.
            JSONArray legacy=new JSONArray();
            for(int i=0;i<20;i++)legacy.put(new JSONObject().put("项目","多人上").put("enabled",false).put("data",store.defaults("多人上")));
            store.config.put("projects",legacy);store.config.remove("taskQueueVersion");store.save();reload();checkDefaults();
            legacy.getJSONObject(9).getJSONObject("data").put("次数","7");
            store.config.put("projects",legacy);store.config.remove("taskQueueVersion");store.save();reload();
            check(tasks().length()==20&&tasks().getJSONObject(9).getJSONObject("data").getString("次数").equals("7"),"Customized legacy queue overwritten");
            tasks().getJSONObject(9).getJSONObject("data").put("次数","1");store.save();reload();
            check(tasks().length()==20,"Modern queue was mistaken for legacy placeholders");
            store.resetProjects();
            checkSeriesOptions();
            checkWorldLeagues();
            store.config.put("accuracy",91).put("global",new JSONObject().put("autoToken",false).put("freePkg",false));store.save();
            String route=store.routeKeys().get(0);store.saveRoute(route,"1,31");
            ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            activity=(MainActivity)test.startActivitySync(new Intent(base,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));bind();
            screenshot("default-queue.png");
            checkTaskRoutes();
            clickTask("世界系列赛");
            screenshot("world-editor-open.png");
            check(node("7. 世界系列赛")!=null,"World editor title missing");
            check(node("使用车组")==null,"World editor retained car groups");
            check(node("青铜用车")!=null,"World editor bronze input missing");
            input("青铜用车","2,3");
            for(String league:List.of("白金","翡翠","精英","宗师")) {scrollToInput(league+"用车");input(league+"用车","2,4");}
            screenshot("world-nine-leagues.png");click("保存");reload();bind();
            for(String league:List.of("白金","翡翠","精英","宗师"))check(tasks().getJSONObject(WORLD).getJSONObject("data").getString(league+"用车").equals("2,4"),"New league vehicle settings did not persist: "+league);
            check(tasks().getJSONObject(WORLD).getJSONObject("data").getString("青铜用车").equals("2,3"),"World vehicle settings did not persist");
            clickTask("大师系列赛");
            check(node("6. 大师系列赛")!=null&&node("多人下用车")!=null,"Master editor did not open vehicle settings");
            input("多人下用车","2,4");click("保存");reload();bind();
            check(tasks().getJSONObject(MASTER).getJSONObject("data").getString("多人下用车").equals("2,4"),"Master vehicle settings did not persist");
            String originalTask=tasks().getJSONObject(DAILY).toString();
            clickTask("狼崽");
            check(node("选择项目类型")==null&&node("4. 每日战利品")!=null&&node("保存")!=null,"Existing task did not open its details directly");
            check(node("每日只执行一次")==null&&node("完成本任务后，当天后续循环自动跳过")==null,"Retired daily-once control is visible");
            input("次数","9");click("取消");check(tasks().getJSONObject(DAILY).toString().equals(originalTask),"Cancelling edit changed task");
            clickTask("狼崽");input("次数","4");input("项目名称","test战利品");click("保存");reload();bind();
            test.runOnMainSync(()->{
                View root=activity.getWindow().getDecorView(),title=findText(root,"test战利品");
                check(title!=null&&findText(root,"test战利品 · 每日战利品")==null,"Custom title still includes task type");
                View tag=findText((View)title.getParent(),"战利品");
                check(tag!=null&&tag.getTop()+((View)tag.getParent()).getTop()>=title.getBottom(),"Type tag is not below title");
                for(String label:List.of("世界赛","大师赛","杯赛","特赛"))check(findText(root,label)!=null,"Missing type tag "+label);
            });
            screenshot("custom-task-name.png");
            JSONObject edited=tasks().getJSONObject(DAILY),original=new JSONObject(originalTask);
            check(edited.getJSONObject("data").getString("次数").equals("4")&&edited.getString("项目").equals("每日战利品")
                    &&edited.getString("taskId").equals(original.getString("taskId"))&&edited.getBoolean("enabled")==original.getBoolean("enabled"),"Edit lost fields/type/identity/flag on reload");
            clickMain("新增任务");check(node("选择项目类型")!=null,"New task must choose type first");click("取消");
            clickMain("新增任务");click("取消");check(tasks().length()==7,"Cancelling type picker added a task");
            clickMain("新增任务");click("杯赛");click("取消");check(tasks().length()==7,"Cancelling form added a task");
            clickMain("新增任务");click("杯赛");input("项目名称","新增杯赛测试");input("次数","0");click("保存");
            check(tasks().length()==7&&node("执行次数需为 1–10000")!=null,"Invalid task was saved");click("确定");
            input("次数","3");click("保存");
            check(tasks().length()==8,"UI add failed");
            JSONObject added=tasks().getJSONObject(7);String addedId=added.getString("taskId");
            check(added.getJSONObject("data").getString("次数").equals("3")&&!added.getBoolean("enabled"),"New task fields/flag incorrect");
            check(!addedId.equals(tasks().getJSONObject(CUP).getString("taskId")),"Repeated mode shared identity");
            reload();bind();check(tasks().getJSONObject(7).getString("taskId").equals(addedId),"Added task did not persist");
            String retained=tasks().getJSONObject(1).toString();clickMain("删除");reload();bind();
            check(tasks().length()==7&&tasks().getJSONObject(0).toString().equals(retained),"Deleting first task changed its neighbor");
            for(int i=0;i<7;i++)clickMain("删除");reload();bind();
            check(tasks().length()==0&&((List<?>)store.luaConfig().get("projects")).isEmpty(),"Empty queue resurrected on reload");
            screenshot("empty-queue.png");
            clickMain("新增任务");click("每日战利品");click("保存");check(tasks().length()==1,"Cannot add to empty queue");
            tasks().getJSONObject(0).put("enabled",true).getJSONObject("data").put("次数","12");store.save();bind();
            String beforeReset=tasks().toString();
            clickMain("重置编排");
            check(node("重置将舍弃当前任务编排配置，并恢复默认的 "+store.defaultProjectCount()+" 个任务配置。确认重置吗？")!=null,"Reset warning missing");
            check(tasks().toString().equals(beforeReset),"Reset happened before confirmation");
            screenshot("reset-confirmation.png");click("取消");reload();bind();
            check(tasks().toString().equals(beforeReset),"Cancelling reset changed saved tasks");
            clickMain("重置编排");click("确认重置");reload();bind();checkDefaults();
            check(store.config.getInt("accuracy")==91&&!store.config.getJSONObject("global").getBoolean("autoToken")&&!store.config.getJSONObject("global").getBoolean("freePkg"),"Reset changed preferences");
            check(store.routes.get(route).raw.equals("1,31"),"Reset changed custom route");
            String reset=tasks().toString();reload();check(reset.equals(tasks().toString()),"Reset did not persist");
            screenshot("reset-queue.png");
            report.putString("result","PASS: direct edit details/cancel/save/reload preserves type/identity/flag; only new tasks choose type; seven defaults; legacy upgrade/custom queue preservation; add/cancel/validation; repeated mode identities; delete all and persist empty queue; add to empty queue; reset fields/order/flags; preferences and routes preserved; isolated preferences only");
        } finally {
            if(activity!=null)test.runOnMainSync(()->activity.finish());base.deleteSharedPreferences(preferences);
        }
    }
    private JSONArray tasks()throws Exception{return store.config.getJSONArray("projects");}
    private void checkSeriesOptions()throws Exception {
        List<String> retired=List.of("使用车组","车组选择","车组青铜用车","车组白银用车","车组黄金用车","车组铂金用车","车组钻石用车","车组传奇用车","多人上漂流","多人下漂流","传奇停跑","传奇分数","多人上掉分","掉分段位","掉分段位结束","多人选路");
        JSONObject original=new JSONObject(store.config.toString()),world=tasks().getJSONObject(WORLD),master=tasks().getJSONObject(MASTER);
        for(String key:retired){world.getJSONObject("data").put(key,"0");master.getJSONObject("data").put(key,"0");}
        world.getJSONObject("data").put("青铜用车","2,3").put("次数","7");world.put("enabled",true);
        master.getJSONObject("data").put("多人下用车","2,4").put("次数","9");master.put("enabled",true);
        JSONObject expected=new JSONObject(world.toString());for(String key:retired)expected.getJSONObject("data").remove(key);
        JSONObject expectedMaster=new JSONObject(master.toString());for(String key:retired)expectedMaster.getJSONObject("data").remove(key);
        JSONObject oldSession=new JSONObject(store.config.toString());String cupBefore=tasks().getJSONObject(CUP).toString();store.save();reload();
        check(tasks().getJSONObject(WORLD).toString().equals(expected.toString()),"World migration changed retained configuration or left retired options");
        check(tasks().getJSONObject(MASTER).toString().equals(expectedMaster.toString()),"Master migration changed retained configuration or left retired options");
        check(tasks().getJSONObject(CUP).toString().equals(cupBefore),"Series migration changed cup configuration");
        store.config=oldSession;check(store.migrateConfig(),"Old resume configuration was not migrated");
        check(tasks().getJSONObject(WORLD).toString().equals(expected.toString()),"Resume migration retained world options");
        check(tasks().getJSONObject(MASTER).toString().equals(expectedMaster.toString()),"Resume migration retained master options");
        for(String mode:List.of("多人上","多人下")){
            JSONArray spec=store.forms.getJSONArray(mode);
            for(int i=0;i<spec.length();i++)check(!retired.contains(spec.getJSONObject(i).getString("key")),"Series editor still exposes retired option");
        }
        store.config=original;store.save();
    }
    private void checkWorldLeagues()throws Exception {
        List<String> leagues=List.of("青铜","白银","黄金","白金","翡翠","钻石","精英","宗师","传奇");
        List<String> fields=new ArrayList<>();JSONArray spec=store.forms.getJSONArray("多人上");
        for(int i=0;i<spec.length();i++) {
            String key=spec.getJSONObject(i).getString("key");if(key.endsWith("用车"))fields.add(key.substring(0,key.length()-2));
        }
        check(fields.equals(leagues),"World league fields must match current game order");
        JSONObject original=new JSONObject(store.config.toString()),world=tasks().getJSONObject(WORLD);
        JSONObject cars=world.getJSONObject("data");cars.remove("白金用车");cars.put("铂金用车","2,4");
        for(String added:List.of("翡翠","精英","宗师"))cars.remove(added+"用车");
        cars.put("青铜用车","").put("钻石用车","3,5");world.put("enabled",true);
        String id=world.getString("taskId"),master=tasks().getJSONObject(MASTER).toString();
        JSONObject oldSession=new JSONObject(store.config.toString());store.save();reload();
        JSONObject migrated=tasks().getJSONObject(WORLD).getJSONObject("data");
        check(!migrated.has("铂金用车")&&migrated.getString("白金用车").equals("2,4"),"Old platinum choice lost on upgrade");
        check(migrated.getString("青铜用车").isEmpty()&&migrated.getString("钻石用车").equals("3,5"),"Existing car preferences changed");
        for(String added:List.of("翡翠","精英","宗师"))check(migrated.getString(added+"用车").equals("1,2,3,4,5,6"),"Missing new league defaults");
        check(tasks().getJSONObject(WORLD).getString("taskId").equals(id)&&tasks().getJSONObject(WORLD).getBoolean("enabled"),"League migration changed task identity or flag");
        check(tasks().getJSONObject(MASTER).toString().equals(master),"World migration changed master task");
        String migratedText=migrated.toString();store.config=oldSession;check(store.migrateConfig(),"Resume league config was not upgraded");
        check(tasks().getJSONObject(WORLD).getJSONObject("data").toString().equals(migratedText),"Resume league upgrade differs from saved config");
        cars=tasks().getJSONObject(WORLD).getJSONObject("data");cars.put("白金用车","").put("铂金用车","6");
        store.migrateConfig();check(cars.getString("白金用车").isEmpty()&&!cars.has("铂金用车"),"Explicit current choice must win over legacy choice");
        check(!store.migrateConfig(),"League migration must be idempotent");
        store.config=new JSONObject(original.toString());store.save();
        // A real untouched six-league queue still qualifies for the five-task migration.
        JSONObject oldDefaults=store.defaults("多人上");oldDefaults.put("铂金用车",oldDefaults.remove("白金用车"));
        for(String added:List.of("翡翠","精英","宗师"))oldDefaults.remove(added+"用车");
        JSONArray legacy=new JSONArray();for(int i=0;i<20;i++)legacy.put(new JSONObject().put("项目","多人上").put("enabled",false).put("data",new JSONObject(oldDefaults.toString())));
        store.config.put("projects",legacy);store.config.remove("taskQueueVersion");store.save();reload();checkDefaults();
        store.config=original;store.save();
    }
    private void checkTaskRoutes()throws Exception {
        String map=store.addCustomMap("路线测试赛道","任务测试地图","0,21");
        JSONObject original=new JSONObject(store.config.toString());
        for(int index:new int[]{DAILY,CUP,SPECIAL}) {
            String mode=tasks().getJSONObject(index).getString("项目"),prefix=index==SPECIAL?"特殊赛事":"杯赛";
            check(store.defaults(mode).getString("路线来源").equals("2"),"Incorrect default route source");
            {
                JSONObject data=tasks().getJSONObject(index).getJSONObject("data");
                data.put("路线来源","0").put("已配置地图","").put("跑法代码","");data.remove("路线配置版本");store.save();reload();
                check(tasks().getJSONObject(index).getJSONObject("data").getString("路线来源").equals("2"),"Unconfigured older task did not adopt auto detection");
                data=tasks().getJSONObject(index).getJSONObject("data");data.put("路线来源","0").put("已配置地图",map);data.remove("路线配置版本");store.save();reload();
                check(tasks().getJSONObject(index).getJSONObject("data").getString("路线来源").equals("0"),"Explicit map selection was overwritten");
            }
            for(String legacy:List.of("3","4","1")) {
                JSONObject task=tasks().getJSONObject(index),data=task.getJSONObject("data");
                for(String key:List.of("路线来源","已配置地图","跑法代码"))data.remove(key);
                data.put(prefix+"选路",legacy).put(prefix+"线路",legacy.equals("4")?map:legacy.equals("3")?"10,22":"");
                JSONObject snapshot=new JSONObject(store.config.toString());store.save();reload();
                data=tasks().getJSONObject(index).getJSONObject("data");
                check(!data.has(prefix+"选路")&&!data.has(prefix+"线路"),"Old route fields survived: "+mode);
                check(data.getString("路线来源").equals(legacy.equals("3")?"1":index==SPECIAL&&legacy.equals("4")?"0":"2"),"Wrong migrated route source: "+mode);
                check(data.getString("已配置地图").equals(legacy.equals("4")?map:"")&&data.getString("跑法代码").equals(legacy.equals("3")?"10,22":""),"Lost old route data: "+mode);
                Object expected=LocalStore.javaValue(store.config);store.config=snapshot;
                check(store.migrateConfig()&&expected.equals(LocalStore.javaValue(store.config)),"Resume migration differs: "+mode);
            }
        }
        store.config=new JSONObject(original.toString());store.save();bind();
        for(int index:new int[]{DAILY,CUP,SPECIAL}) {
            openTaskForm(index);
            scrollToControl("路线来源");
            check(node(index==SPECIAL?"选择已配置地图":"自动识别地图")!=null,"Wrong default route source in editor");
            if(index!=SPECIAL){click("路线来源");click("选择已配置地图");}
            scrollToControl("已配置地图");click("已配置地图");
            input("搜索地图","任务测试地图");click(store.matcher.displayName(map));click("保存");reload();bind();
            JSONObject data=tasks().getJSONObject(index).getJSONObject("data");
            check(data.getString("路线来源").equals("0")&&data.getString("已配置地图").equals(map),"Map selection did not persist");
            openTaskForm(index);scrollToControl("路线来源");click("路线来源");click("自定义跑法代码");scrollToInput("跑法代码");
            if(index==SPECIAL){
                input("跑法代码","");click("保存");check(node("确定")!=null,"Empty special event route was accepted");click("确定");
            }
            input("跑法代码","bad");click("保存");check(node("确定")!=null,"Invalid route code was accepted");click("确定");
            input("跑法代码","0,21,20,D:100");screenshot("task-route-"+index+".png");click("保存");reload();bind();
            data=tasks().getJSONObject(index).getJSONObject("data");
            check(data.getString("路线来源").equals("1")&&data.getString("跑法代码").equals("0,21,20,D:100")&&data.getString("已配置地图").equals(map),"Custom code or inactive map choice lost");
            String before=tasks().toString();openTaskForm(index);scrollToInput("跑法代码");input("跑法代码","1,22");click("取消");check(tasks().toString().equals(before),"Cancelling route edit changed configuration");
            openTaskForm(index);scrollToControl("路线来源");click("路线来源");click("自动识别地图");screenshot("auto-route-"+index+".png");click("保存");reload();bind();
            data=tasks().getJSONObject(index).getJSONObject("data");check(data.getString("路线来源").equals("2")&&data.getString("跑法代码").equals("0,21,20,D:100"),"Auto selection did not persist or lost custom draft");
        }
        store.saveRoute(map,"30,31");check(store.routes.get(map).raw.equals("30,31"),"Map route update failed");
        store.config=original;store.save();bind();
    }
    private void openTaskForm(int index)throws Exception {
        JSONObject task=tasks().getJSONObject(index),draft=new JSONObject(task.getJSONObject("data").toString());
        test.runOnMainSync(()->{try {
            Method form=MainActivity.class.getDeclaredMethod("form",int.class,String.class,JSONObject.class);form.setAccessible(true);form.invoke(activity,index,task.getString("项目"),draft);
        }catch(Exception e){throw new AssertionError(e);}});settle();
    }
    private void scrollToControl(String name) {
        for(int i=0;i<12;i++) {
            AccessibilityNodeInfo field=node(name);if(field!=null&&field.isVisibleToUser())return;
            AccessibilityNodeInfo scroll=scrollable(ui.getRootInActiveWindow());
            check(scroll!=null&&scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),"Cannot reach control "+name);settle();
        }
        throw new AssertionError("Cannot reach control "+name);
    }
    private void reload()throws Exception{store=new LocalStore(isolated);}
    private void checkDefaults()throws Exception {
        String[] modes={"车联","杯赛","特殊赛事","每日战利品","特殊赛事","多人下","多人上"};String[] counts={"2","2","2","3","3","3","3"};Set<String> ids=new HashSet<>();
        JSONArray template=new JSONArray(store.asset("data/default-projects.json"));
        check(tasks().length()==7,"Expected seven tasks");
        for(int i=0;i<modes.length;i++) {
            JSONObject task=tasks().getJSONObject(i);
            check(task.getString("项目").equals(modes[i])&&task.getBoolean("enabled"),"Wrong default mode/flag at "+i);
            check(task.getJSONObject("data").getString("次数").equals(counts[i]),"Wrong saved default count");
            check(LocalStore.javaValue(task.getJSONObject("data")).equals(LocalStore.javaValue(template.getJSONObject(i).getJSONObject("data"))),"Default fields not restored");
            if(LocalStore.hasTaskRoute(task.getString("项目")))store.validateTaskRoute(task.getJSONObject("data"));
            check(!task.getString("taskId").isEmpty()&&ids.add(task.getString("taskId")),"Missing/duplicate identity");
        }
    }
    private void bind(){test.runOnMainSync(()->{
        try{Field field=MainActivity.class.getDeclaredField("store");field.setAccessible(true);field.set(activity,store);Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(activity);}
        catch(Exception e){throw new AssertionError(e);}
    });settle();}
    private void clickMain(String text){test.runOnMainSync(()->{View view=findText(activity.getWindow().getDecorView(),text);check(view!=null,"Missing main action "+text);view.performClick();});settle();}
    private void clickTask(String text){test.runOnMainSync(()->{View view=findText(activity.getWindow().getDecorView(),text);check(view!=null,"Missing task "+text);((View)view.getParent()).performClick();});settle();}
    private View findText(View view,String text){
        if(view instanceof TextView&&text.equals(((TextView)view).getText().toString()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;
    }
    private AccessibilityNodeInfo node(String text){return find(ui.getRootInActiveWindow(),text);}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String text){
        if(node==null)return null;if(text.equals(String.valueOf(node.getContentDescription()))||text.equals(String.valueOf(node.getText())))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=find(node.getChild(i),text);if(found!=null)return found;}return null;
    }
    private void click(String text){AccessibilityNodeInfo node=node(text);check(node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Missing dialog action "+text);settle();}
    private AccessibilityNodeInfo editable(AccessibilityNodeInfo node,String name){
        if(node==null)return null;if(node.isEditable()&&name.equals(String.valueOf(node.getContentDescription())))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=editable(node.getChild(i),name);if(found!=null)return found;}return null;
    }
    private void scrollToInput(String name) {
        for(int i=0;i<12;i++) {
            AccessibilityNodeInfo field=editable(ui.getRootInActiveWindow(),name);
            if(field!=null&&field.isVisibleToUser())return;
            AccessibilityNodeInfo scroll=scrollable(ui.getRootInActiveWindow());
            check(scroll!=null&&scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),"Cannot reach input "+name);settle();
        }
        throw new AssertionError("Cannot reach input "+name);
    }
    private AccessibilityNodeInfo scrollable(AccessibilityNodeInfo node) {
        if(node==null)return null;if(node.isScrollable())return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=scrollable(node.getChild(i));if(found!=null)return found;}return null;
    }
    private void input(String name,String value){AccessibilityNodeInfo field=editable(ui.getRootInActiveWindow(),name);check(field!=null,"Missing input "+name);Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Cannot edit "+name);settle();}
    private void screenshot(String name)throws Exception {
        settle();android.graphics.Bitmap image=ui.takeScreenshot();File dir=new File(test.getTargetContext().getExternalFilesDir(null),"task-editing-check");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
    }
    private void settle(){test.waitForIdleSync();SystemClock.sleep(200);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
