package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.io.*;
import java.util.*;
import dev.funlee.a9.core.*;

public final class MainActivity extends Activity {
    private LocalStore store;
    private LinearLayout root,body;
    private TextView status,log;
    private int tab;
    private boolean pendingInspect;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(!BuildConfig.PRIVATE_ASSETS_ENABLED){
            LinearLayout intro=column();intro.setPadding(dp(24),dp(24),dp(24),dp(24));
            intro.addView(label("A9 Copilot",25));
            intro.addView(label("公开源码版",18));
            intro.addView(label("此构建不包含从其他 APK 提取的脚本、地图路线和识别字库，因此不能启动自动比赛。完整本地功能需要你自行提供获授权的私有资源，并按项目文档启用本地构建。",16));
            setContentView(intro);return;
        }
        try{store=new LocalStore(this);show();}catch(Exception e){TextView error=new TextView(this);error.setText(getString(R.string.init_error,e.toString()));setContentView(error);}
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(0xff203b38);t.setPadding(0,dp(5),0,dp(5));return t;}
    private Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->{try{action.run();}catch(Exception e){message(e.getMessage());}});return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private ScrollView scroll(View v){ScrollView s=new ScrollView(this);s.addView(v);return s;}
    private EditText edit(String value){EditText e=new EditText(this);e.setText(value);e.setTextSize(14);return e;}
    private void show(){
        root=column();root.setBackgroundColor(0xfff3f5f4);root.setPadding(dp(16),dp(12),dp(16),dp(8));root.setFitsSystemWindows(true);
        root.addView(label("A9 本地助手",25));root.addView(label("本地配置 · 无需登录",12));status=label(SessionState.status,13);root.addView(status);
        LinearLayout nav=row();String[] names={"运行","路线","诊断","设置"};for(int i=0;i<names.length;i++){final int n=i;nav.addView(button(names[i],()->{tab=n;render();}),new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);
        body=column();root.addView(scroll(body),new LinearLayout.LayoutParams(-1,0,1));setContentView(root);render();
    }
    private void render(){body.removeAllViews();log=null;switch(tab){case 0:projects();break;case 1:routes();break;case 2:diagnostics();break;default:settings();}}
    private void projects(){
        body.addView(label("项目队列",20));body.addView(label("按顺序执行启用的项目；设置会保存在本机。首次验收建议先用单条路线手动跑图。",13));
        LinearLayout actions=row();actions.addView(button("开始任务",()->requestStart("",false)));actions.addView(button("暂停 / 继续",()->control(AutomationService.PAUSE)));actions.addView(button("停止",()->control(AutomationService.STOP)));body.addView(actions);
        String recent=store.config.optString("lastRoute",store.config.optString("routeOnly",""));
        if(store.routes.containsKey(recent))body.addView(button("手动跑图 · "+store.matcher.displayName(recent),()->requestStart(recent,false)));
        CheckBox repeat=new CheckBox(this);repeat.setText("循环执行项目队列");repeat.setChecked(store.config.optBoolean("repeat",false));repeat.setOnCheckedChangeListener((v,b)->{try{store.config.put("repeat",b);store.save();}catch(Exception e){message(e.toString());}});body.addView(repeat);
        JSONArray all=store.config.optJSONArray("projects");
        for(int i=0;i<all.length();i++){
            final int index=i;JSONObject p=all.optJSONObject(i);LinearLayout line=row();CheckBox enabled=new CheckBox(this);enabled.setChecked(p.optBoolean("enabled"));
            enabled.setOnCheckedChangeListener((v,b)->{try{p.put("enabled",b);store.save();}catch(Exception e){message(e.toString());}});
            line.addView(enabled);JSONObject d=p.optJSONObject("data");String name=d.optString("项目名称",p.optString("项目"));
            String mode=p.optString("项目");name=name.equals(mode)?MultiplayerUi.displayMode(mode):name+" · "+MultiplayerUi.displayMode(mode);
            line.addView(button((i+1)+". "+name+" · "+d.optString("次数","1")+" 次",()->projectEditor(index)),new LinearLayout.LayoutParams(0,-2,1));body.addView(line);
        }
    }
    private void projectEditor(int index){
        JSONObject project=store.config.optJSONArray("projects").optJSONObject(index);
        String[] labels=Arrays.stream(LocalStore.MODES).map(MultiplayerUi::displayMode).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("选择项目类型").setItems(labels,(dialog,which)->{
            try{
                String mode=LocalStore.MODES[which];JSONObject data=project.optString("项目").equals(mode)?new JSONObject(project.getJSONObject("data").toString()):store.defaults(mode);
                form(index,mode,data);
            }catch(Exception e){message(e.getMessage());}
        }).setNegativeButton("取消",null).show();
    }
    private void form(int index,String mode,JSONObject data) throws Exception {
        LinearLayout fields=column();fields.setPadding(dp(16),0,dp(16),0);JSONArray spec=store.forms.getJSONArray(mode);Map<String,View> views=new LinkedHashMap<>();
        if(mode.equals("多人上")||mode.equals("多人下"))fields.addView(label("多人上对应世界系列赛，多人下对应大师系列赛。新版菜单目前只选首屏序号 1–4 中有油的车辆，支持每次用一辆；车组、广告修车、自动掉分和到段位停跑尚未适配。",13));
        for(int i=0;i<spec.length();i++){
            JSONObject f=spec.getJSONObject(i);String key=f.getString("key"),type=f.getString("type");JSONArray options=f.getJSONArray("options");
            if(type.equals("EditText")){fields.addView(label(key,13));EditText e=edit(data.optString(key));fields.addView(e);views.put(key,e);}
            else if(type.equals("CheckBoxGroup")){
                LinearLayout checks=column();String selected=","+data.optString(key)+",";
                for(int n=0;n<options.length();n++){CheckBox cb=new CheckBox(this);cb.setText(options.getString(n));cb.setTag(n);cb.setChecked(selected.contains(","+n+","));checks.addView(cb);}fields.addView(label(key,13));fields.addView(checks);views.put(key,checks);
            }else{
                fields.addView(label(key,13));Spinner spinner=new Spinner(this);String[] choices=new String[options.length()];for(int j=0;j<choices.length;j++)choices[j]=options.getString(j);
                spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));
                int selection=0;try{selection=Integer.parseInt(data.optString(key,"0"));}catch(Exception ignored){}spinner.setSelection(Math.min(Math.max(0,selection),Math.max(0,choices.length-1)));
                fields.addView(spinner);views.put(key,spinner);
            }
        }
        fields.addView(label("选路为“百分比选路”时，线路填写进度指令；“自定义选图”时可通过下方按钮填入本地地图。",12));
        fields.addView(button("选择本地路线",()->chooseRoute(key->{
            String prefix=mode.equals("车联")?"车手联会":mode.equals("每日战利品")?"杯赛":mode;
            View target=views.get(prefix+"线路");if(target instanceof EditText)((EditText)target).setText(store.routes.get(key).raw);
            View choice=views.get(prefix+"选路");if(choice instanceof Spinner)((Spinner)choice).setSelection(3);
        })));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle((index+1)+". "+MultiplayerUi.displayMode(mode)).setView(scroll(fields)).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{
            try{
                for(Map.Entry<String,View> e:views.entrySet()){
                    String value="";View view=e.getValue();
                    if(view instanceof EditText)value=((EditText)view).getText().toString().trim();
                    else if(view instanceof Spinner)value=String.valueOf(((Spinner)view).getSelectedItemPosition());
                    else if(view instanceof LinearLayout){List<String> selected=new ArrayList<>();for(int j=0;j<((LinearLayout)view).getChildCount();j++){CheckBox cb=(CheckBox)((LinearLayout)view).getChildAt(j);if(cb.isChecked())selected.add(String.valueOf(cb.getTag()));}value=String.join(",",selected);}
                    data.put(e.getKey(),value);
                }
                int times=Integer.parseInt(data.optString("次数","1"));if(times<1||times>10000)throw new IllegalArgumentException("执行次数需为 1–10000");
                JSONObject p=store.config.getJSONArray("projects").getJSONObject(index);p.put("项目",mode);p.put("data",data);store.save();dialog.dismiss();render();
            }catch(Exception e){message(e.getMessage());}
        }));dialog.show();
    }
    private void routes(){
        body.addView(label("本地路线库",20));body.addView(label("显示国服名称，保留原始配置索引。可查看、编辑并单独试跑。",13));
        EditText query=edit("");query.setHint("搜索地图名 / 内部索引");body.addView(query);LinearLayout list=column();body.addView(list);
        Runnable update=()->{list.removeAllViews();String q=query.getText().toString();for(String key:store.routeKeys()){
            String display=store.matcher.displayName(key);if(!display.contains(q)&&!key.contains(q))continue;Route r=store.routes.get(key);
            list.addView(button(display+(r.valid()?"":" ⚠"),()->routeEditor(key)));
        }};
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){update.run();}public void afterTextChanged(Editable s){}});update.run();
    }
    private void routeEditor(String key){
        Route route=store.routes.get(key);LinearLayout box=column();box.setPadding(dp(16),0,dp(16),0);box.addView(label("内部索引："+key+"\n"+route.events.size()+" 个动作"+(route.valid()?"":"\n"+String.join("\n",route.errors)),13));
        EditText raw=edit(route.raw);raw.setMinLines(5);box.addView(raw);
        box.addView(label("N:a:n:b 氮气；D:t 漂移；21 / 31 等为路线按钮；360 为双击漂移后补氮气。",12));
        box.addView(button("保存并手动跑图",()->{try{store.saveRoute(key,raw.getText().toString());requestStart(key,false);}catch(Exception e){message(e.getMessage());}}));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(store.matcher.displayName(key)).setView(scroll(box)).setNegativeButton("关闭",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{store.saveRoute(key,raw.getText().toString());dialog.dismiss();render();}catch(Exception e){message(e.getMessage());}}));dialog.show();
    }
    private void chooseRoute(java.util.function.Consumer<String> chosen){List<String> keys=store.routeKeys();String[] labels=new String[keys.size()];for(int i=0;i<labels.length;i++)labels[i]=store.matcher.displayName(keys.get(i));new AlertDialog.Builder(this).setTitle("选择路线").setItems(labels,(d,w)->chosen.accept(keys.get(w))).setNegativeButton("取消",null).show();}
    private void diagnostics(){
        body.addView(label("识别与运行诊断",20));body.addView(label("开始诊断后，在游戏的地图介绍或比赛画面点击悬浮窗或通知中的“取样”。识别结果显示在悬浮窗和日志中。",13));
        body.addView(button("开始屏幕诊断",()->requestStart("",true)));
        body.addView(button("查看最近诊断截图",this::preview));
        EditText text=edit("");text.setHint("输入画面上的地图名，测试匹配");body.addView(text);
        body.addView(button("测试名称匹配",()->{MapMatcher.Match m=store.matcher.match(text.getText().toString());List<String> c=new ArrayList<>();for(MapMatcher.Rule r:m.candidates)c.add(r.display+" → "+r.internal);message(c.isEmpty()?"没有匹配":String.join("\n",c));}));
        log=label(SessionState.logs(),12);log.setTextIsSelectable(true);body.addView(log);
    }
    private void preview(){File f=new File(getFilesDir(),"runtime/diagnostic.png");Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());if(b==null){message("还没有诊断截图");return;}ImageView image=new ImageView(this);image.setImageBitmap(b);image.setAdjustViewBounds(true);new AlertDialog.Builder(this).setTitle("归一化画面 · 720×1280").setView(scroll(image)).setPositiveButton("关闭",(d,w)->b.recycle()).show();}
    private void settings(){
        body.addView(label("设备与本地设置",20));body.addView(button("开启无障碍触控",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        body.addView(button("开启悬浮控制",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())))));
        body.addView(button("允许通知控制",()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},102);else message("当前系统可直接显示任务通知");}));
        body.addView(button("领取奖励与广告选项",this::globalSettings));
        body.addView(label("游戏包名",13));EditText pkg=edit(store.config.optString("gamePkg"));body.addView(pkg);
        body.addView(label("将截屏顺时针旋转至 720×1280 基准方向",13));Spinner rotation=new Spinner(this);rotation.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"不旋转","顺时针 90°","180°","顺时针 270°"}));int[] angles={0,90,180,270};for(int i=0;i<4;i++)if(angles[i]==store.config.optInt("rotation",90))rotation.setSelection(i);body.addView(rotation);
        body.addView(label("字库匹配阈值（50–100）",13));EditText accuracy=edit(String.valueOf(store.config.optInt("accuracy",80)));body.addView(accuracy);
        body.addView(label("动作延迟（0–5000ms）",13));EditText delay=edit(String.valueOf(store.config.optInt("delay",0)));body.addView(delay);
        body.addView(label("单局最长运行时间（60–1800 秒）",13));EditText raceTimeout=edit(String.valueOf(store.config.optInt("raceTimeout",900)));body.addView(raceTimeout);
        body.addView(label("路线执行方式",13));Spinner driver=new Spinner(this);driver.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ZX 队列时序","XC 起始延时"}));driver.setSelection(store.config.optInt("driverMode",1)-1);body.addView(driver);
        body.addView(button("保存设置",()->{try{
            int a=Integer.parseInt(accuracy.getText().toString()),d=Integer.parseInt(delay.getText().toString()),seconds=Integer.parseInt(raceTimeout.getText().toString());if(a<50||a>100||d<0||d>5000||seconds<60||seconds>1800)throw new IllegalArgumentException("请检查识别阈值、动作延迟和单局时间范围");
            String p=pkg.getText().toString().trim();if(!p.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IllegalArgumentException("包名格式错误");
            store.config.put("gamePkg",p).put("rotation",angles[rotation.getSelectedItemPosition()]).put("accuracy",a).put("delay",d).put("raceTimeout",seconds).put("driverMode",driver.getSelectedItemPosition()+1);store.save();message("设置已保存，下次启动时生效");
        }catch(Exception e){message(e.getMessage());}}));
        body.addView(label("当前版本先按 16:9 游戏画面校准。进度 OCR、菜单颜色特征与触控位置仍需在目标游戏版本上实测。",12));
        body.addView(button("第三方运行库许可",()->{try{message(store.asset("licenses/luajava.txt")+"\n\n"+store.asset("licenses/jnigen.txt"));}catch(Exception e){message(e.getMessage());}}));
    }
    private void globalSettings(){
        JSONObject saved=store.config.optJSONObject("global");if(saved==null)saved=new JSONObject();final JSONObject global=saved;
        String[][] options={{"freePkg","领取免费包"},{"coinPkg","购买金币零件包"},{"autoToken","领取任务代币"},{"Coupons","领取点券"},{"clearPrize","杯赛奖励与扫荡"},{"spPrize","领取大奖赛包"},{"coinAD","多人赛后金币广告"},{"cupTicketAD","杯赛看广告回票"},{"spCupTicketAD","特殊赛事看广告回票"},{"dsTicketAD","车联看广告回票"},{"onceAD","单次广告修车"},{"checkAuto","检查游戏触屏操作设置"}};
        LinearLayout box=column();box.setPadding(dp(16),0,dp(16),0);Map<String,CheckBox> toggles=new LinkedHashMap<>();
        for(String[] o:options){CheckBox cb=new CheckBox(this);cb.setText(o[1]);cb.setChecked(global.optBoolean(o[0],false));box.addView(cb);toggles.put(o[0],cb);}
        box.addView(label("广告等待时间（10–120 秒）",13));EditText seconds=edit(String.valueOf(global.optInt("AdTime",40)));box.addView(seconds);
        box.addView(label("多人金币广告名次（1–8）",13));EditText rank=edit(String.valueOf(global.optInt("coinADNum",1)));box.addView(rank);
        box.addView(label("大奖赛位置（序号，用逗号分隔）",13));EditText positions=edit(global.optString("spPos","1"));box.addView(positions);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("领取奖励与广告选项").setView(scroll(box)).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
            int s=Integer.parseInt(seconds.getText().toString()),r=Integer.parseInt(rank.getText().toString());if(s<10||s>120||r<1||r>8)throw new IllegalArgumentException("请检查广告时间和名次范围");
            String p=positions.getText().toString().replace('，',',').replace(" ","");if(!p.matches("[1-9][0-9]?(,[1-9][0-9]?)*"))throw new IllegalArgumentException("大奖赛位置应填写正整数序号");
            for(Map.Entry<String,CheckBox> e:toggles.entrySet())global.put(e.getKey(),e.getValue().isChecked());
            global.put("AdTime",s).put("coinADNum",r).put("spPos",p);store.config.put("global",global);store.save();dialog.dismiss();
        }catch(Exception e){message(e.getMessage());}}));dialog.show();
    }
    private void requestStart(String route,boolean inspect){
        if(SessionState.running){message("请先停止当前任务");return;}
        if(inspect&&!Settings.canDrawOverlays(this)&&!getSystemService(NotificationManager.class).areNotificationsEnabled()){tab=3;render();message("请先开启悬浮窗或通知，以便在游戏中点击取样");return;}
        if(!inspect&&TouchService.instance==null){tab=3;render();message("请先在设置中开启无障碍服务");return;}
        try{
            if(!inspect&&route.isEmpty()){
                boolean enabled=false;JSONArray p=store.config.getJSONArray("projects");for(int i=0;i<p.length();i++)enabled|=p.getJSONObject(i).optBoolean("enabled");if(!enabled)throw new IllegalArgumentException("请至少启用一个项目，或从路线页选择手动跑图");
            }
            store.config.put("routeOnly",route);if(!route.isEmpty())store.config.put("lastRoute",route);store.save();pendingInspect=inspect;
            MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);Intent consent;
            if(Build.VERSION.SDK_INT>=34)consent=manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay());else consent=manager.createScreenCaptureIntent();
            startActivityForResult(consent,101);
        }catch(Exception e){message(e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=101)return;
        if(result!=RESULT_OK||data==null){message("未授权截屏，任务没有启动");return;}
        Intent launch=getPackageManager().getLaunchIntentForPackage(store.config.optString("gamePkg"));
        if(launch==null){message("未找到游戏，请核对包名和安装状态");return;}
        Intent service=new Intent(this,AutomationService.class).setAction(AutomationService.START).putExtra("result",result).putExtra("consent",data).putExtra("inspect",pendingInspect);
        startForegroundService(service);
        // Match an explicit activity resume, keeping launcher-specific reinitialization out of this transition.
        startActivity(new Intent().setComponent(launch.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void control(String action){if(!SessionState.running)return;if(action.equals(AutomationService.STOP))SessionState.status("已手动停止");startService(new Intent(this,AutomationService.class).setAction(action));}
    private void message(String text){new AlertDialog.Builder(this).setMessage(text==null?"操作失败":text).setPositiveButton("确定",null).show();}
    @Override protected void onResume(){super.onResume();SessionState.listener=()->{if(status!=null)status.setText(getString(R.string.session_status,SessionState.status,SessionState.panelText().isEmpty()?"":"\n"+SessionState.panelText()));if(log!=null)log.setText(SessionState.logs());};SessionState.changed();}
    @Override protected void onPause(){SessionState.listener=null;super.onPause();}
}
