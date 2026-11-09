package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
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
    private LinearLayout root,body,contentPanel,sessionCard;
    private ScrollView pageScroll;
    private TextView status,log;
    private TextView pageTitle,pageSubtitle,sessionMeta,stateBadge;
    private final Button[] navigation=new Button[4];
    private Button startControl,pauseControl,stopControl;
    private static final int BG=0xfff5f7f9,SURFACE=0xffffffff,INK=0xff1d2939,MUTED=0xff778396,ACCENT=0xff087f73,SOFT=0xffe7f4f0,LINE=0xffe4e9ef;
    private boolean wide;
    private int tab;
    private boolean pendingInspect;
    private boolean pendingResume;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(saved!=null)tab=saved.getInt("tab",0);
        try{store=new LocalStore(this);show();}catch(Exception e){TextView error=new TextView(this);error.setText(getString(R.string.init_error,e.toString()));setContentView(error);}
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable shape(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private void surface(View view,int fill,int stroke,int radius){view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x14087f73),shape(fill,stroke,radius),null));}
    private TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(size>=17?INK:MUTED);t.setPadding(0,dp(5),0,dp(5));t.setLineSpacing(dp(3),1);if(size>=17)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    private Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(46));b.setMinimumHeight(dp(46));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(14),dp(9),dp(14),dp(9));b.setElevation(0);b.setStateListAnimator(null);
        boolean primary=text.startsWith("开始")||text.startsWith("保存"),danger=text.equals("停止");
        surface(b,primary?ACCENT:danger?0xfffceeed:SURFACE,primary||danger?0:LINE,10);b.setTextColor(primary?SURFACE:danger?0xffb74d49:INK);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(p);
        b.setOnClickListener(v->{try{action.run();}catch(Exception e){message(e.getMessage());}});return b;}
    private Button compactButton(String text,Runnable action){Button b=button(text,action);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(8),dp(4),dp(8),dp(4));b.setTextSize(13);return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private LinearLayout card(){LinearLayout box=column();box.setPadding(dp(18),dp(13),dp(18),dp(13));box.setBackground(shape(SURFACE,LINE,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));box.setLayoutParams(p);return box;}
    private TextView badge(String text,int color,int fill){TextView b=label(text,11);b.setTextColor(color);b.setGravity(Gravity.CENTER);b.setPadding(dp(10),dp(5),dp(10),dp(5));b.setBackground(shape(fill,0,7));return b;}
    private ScrollView scroll(View v){ScrollView s=new ScrollView(this);s.setFillViewport(false);s.setClipToPadding(false);s.setVerticalScrollBarEnabled(false);s.addView(v);return s;}
    private EditText edit(String value){EditText e=new EditText(this);e.setText(value);e.setTextSize(14);e.setTextColor(INK);e.setHintTextColor(0xff9aa6b5);e.setPadding(dp(13),dp(12),dp(13),dp(12));e.setBackground(shape(BG,LINE,9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(3),0,dp(8));e.setLayoutParams(p);e.setMinHeight(dp(46));return e;}
    private void weighted(LinearLayout row,View view,int index){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(index==0?0:dp(8),0,0,0);row.addView(view,p);}
    private void brand(LinearLayout target){LinearLayout brand=row();brand.setGravity(Gravity.CENTER_VERTICAL);TextView mark=badge("A9",SURFACE,ACCENT);mark.setTextSize(16);mark.setTypeface(Typeface.DEFAULT_BOLD);mark.setPadding(dp(10),dp(10),dp(10),dp(10));brand.addView(mark);LinearLayout names=column();names.setPadding(dp(12),0,0,0);names.addView(label("Co-Driver",18));TextView caption=label("本地自动驾驶助手",11);caption.setPadding(0,0,0,0);names.addView(caption);brand.addView(names);target.addView(brand);}
    private void show(){
        wide=getResources().getConfiguration().screenWidthDp>=840||(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE&&getResources().getConfiguration().screenWidthDp>=600);root=wide?row():column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        LinearLayout rail=column();rail.setPadding(dp(wide?22:18),dp(22),dp(wide?22:18),dp(16));if(wide){rail.setBackgroundColor(SURFACE);root.addView(rail,new LinearLayout.LayoutParams(dp(218),-1));}else root.addView(rail);
        brand(rail);LinearLayout nav=wide?column():row();LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(wide?34:18);rail.addView(nav,np);
        String[] names={"运行中心","地图路线","运行诊断","偏好设置"};for(int i=0;i<names.length;i++){final int n=i;Button b=button(names[i],()->{tab=n;render();pageScroll.scrollTo(0,0);});b.setGravity(wide?Gravity.CENTER_VERTICAL|Gravity.START:Gravity.CENTER);navigation[i]=b;if(!wide){b.setTextSize(12);b.setPadding(dp(4),dp(8),dp(4),dp(8));}if(wide)nav.addView(b);else weighted(nav,b,i);}
        if(wide){View space=new View(this);rail.addView(space,new LinearLayout.LayoutParams(1,0,1));rail.addView(badge("本地运行 · 无需账号",ACCENT,SOFT));TextView version=label("A9 Co-Driver  ·  "+version(),11);version.setPadding(0,dp(12),0,0);rail.addView(version);}
        LinearLayout content=column();contentPanel=content;content.setPadding(dp(wide?28:18),dp(wide?22:4),dp(wide?28:18),dp(12));root.addView(content,wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout heading=row();heading.setGravity(Gravity.CENTER_VERTICAL);LinearLayout titles=column();pageTitle=label("",25);pageSubtitle=label("",12);titles.addView(pageTitle);titles.addView(pageSubtitle);heading.addView(titles,new LinearLayout.LayoutParams(0,-2,1));stateBadge=badge("待命",ACCENT,SOFT);heading.addView(stateBadge);content.addView(heading);
        LinearLayout session=card();sessionCard=session;LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(14),0,dp(16));session.setLayoutParams(sp);status=label("",14);status.setTextColor(INK);status.setMaxLines(2);status.setEllipsize(TextUtils.TruncateAt.END);sessionMeta=label("",11);session.addView(status);session.addView(sessionMeta);session.setOnClickListener(v->message(SessionState.panelText().isEmpty()?SessionState.status:SessionState.panelText()));content.addView(session);
        body=column();pageScroll=scroll(body);content.addView(pageScroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);render();
    }
    private void render(){body.removeAllViews();log=null;startControl=null;pauseControl=null;stopControl=null;String[] titles={"运行中心","地图路线","运行诊断","偏好设置"},subtitles={"配置任务，按顺序自动执行","国服地图与本地跑法","查看识别结果与运行记录","设备权限与运行参数"};pageTitle.setText(titles[tab]);pageSubtitle.setText(subtitles[tab]);compactRunHeader();for(int i=0;i<navigation.length;i++){surface(navigation[i],i==tab?SOFT:wide?SURFACE:BG,0,9);navigation[i].setTextColor(i==tab?ACCENT:MUTED);}switch(tab){case 0:projects();break;case 1:routes();break;case 2:diagnostics();break;default:settings();}updateSession();}
    private void compactRunHeader(){
        boolean compact=tab==0,inline=compact&&wide;
        contentPanel.setPadding(dp(wide?28:18),dp(compact?10:wide?22:4),dp(wide?28:18),dp(8));
        pageTitle.setTextSize(compact?22:25);pageTitle.setPadding(0,dp(compact?2:5),0,dp(compact?2:5));pageSubtitle.setVisibility(compact?View.GONE:View.VISIBLE);
        sessionCard.setOrientation(inline?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);sessionCard.setGravity(Gravity.CENTER_VERTICAL);sessionCard.setPadding(dp(compact?12:18),dp(compact?4:13),dp(compact?12:18),dp(compact?4:13));
        LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)sessionCard.getLayoutParams();p.setMargins(0,dp(compact?6:14),0,dp(compact?8:16));sessionCard.setLayoutParams(p);
        status.setMaxLines(compact?1:2);status.setLayoutParams(inline?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2));status.setPadding(0,dp(compact?2:5),dp(inline?12:0),dp(compact?2:5));
        sessionMeta.setMaxLines(compact?1:Integer.MAX_VALUE);sessionMeta.setEllipsize(TextUtils.TruncateAt.END);sessionMeta.setLayoutParams(new LinearLayout.LayoutParams(inline?-2:-1,-2));sessionMeta.setPadding(0,dp(compact?2:5),0,dp(compact?2:5));
    }
    private void updateSession(){if(status==null)return;status.setText(SessionState.status);String state=SessionState.running?(SessionState.paused?"已暂停":"运行中"):"待命";stateBadge.setText(state);Map<String,Object> stats=SessionState.stats.snapshot();sessionMeta.setText("已完赛 "+SessionState.stats.finished()+" 场  ·  "+(SessionState.running?"第 "+stats.get("cycle")+" 轮":"等待开始")+"  ·  点击查看运行详情");if(startControl!=null)startControl.setEnabled(!SessionState.running);if(pauseControl!=null){pauseControl.setText(SessionState.paused?"继续":"暂停");pauseControl.setAlpha(SessionState.running?1:.45f);}if(stopControl!=null)stopControl.setAlpha(SessionState.running?1:.45f);}
    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "";}}
    private String modeName(String mode){return MultiplayerUi.displayMode(mode).replace("（原多人上）","").replace("（原多人下）","");}
    private void projects(){
        LinearLayout controls=card();controls.setPadding(dp(12),dp(6),dp(12),dp(6));LinearLayout.LayoutParams cp=(LinearLayout.LayoutParams)controls.getLayoutParams();cp.bottomMargin=dp(8);
        LinearLayout toolbar=wide?row():column(),actions=row();toolbar.setGravity(Gravity.CENTER_VERTICAL);startControl=compactButton(wide?"开始任务":"开始",()->requestStart("",false));pauseControl=compactButton("暂停",()->control(AutomationService.PAUSE));stopControl=compactButton("停止",()->control(AutomationService.STOP));weighted(actions,startControl,0);weighted(actions,pauseControl,1);weighted(actions,stopControl,2);
        if(SessionRecord.available(this))weighted(actions,compactButton(wide?"继续上次":"续跑",()->requestStart("",false,true)),3);
        if(wide)weighted(toolbar,actions,0);else toolbar.addView(actions);
        LinearLayout limits=row();
        int cycles=store.config.optInt("maxCycles",0);
        weighted(limits,compactButton("循环轮数："+(cycles>0?cycles+" 轮":"不限"),()->{
            EditText value=edit(String.valueOf(store.config.optInt("maxCycles",0)));value.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("0 表示不限；无油跳过也计入本轮").setView(value).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
            dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
                int n=Integer.parseInt(value.getText().toString().trim());if(n<0||n>100000)throw new IllegalArgumentException("请输入 0–100000");
                store.config.put("maxCycles",n);store.save();dialog.dismiss();render();
            }catch(Exception e){message(e.getMessage());}}));dialog.show();
        }),0);
        int limit=store.config.optInt("maxRaces",0);
        weighted(limits,compactButton("完赛上限："+(limit>0?limit+" 局":"不限"),()->{
            EditText value=edit(String.valueOf(store.config.optInt("maxRaces",0)));value.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("本次任务总完赛上限，0 表示不限").setView(value).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
            dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
                int n=Integer.parseInt(value.getText().toString().trim());if(n<0||n>100000)throw new IllegalArgumentException("请输入 0–100000");
                store.config.put("maxRaces",n);store.save();dialog.dismiss();render();
            }catch(Exception e){message(e.getMessage());}}));dialog.show();
        }),1);
        if(wide)weighted(toolbar,limits,1);else toolbar.addView(limits);
        controls.addView(toolbar);
        body.addView(controls);
        JSONArray all=store.config.optJSONArray("projects");int count=0;for(int i=0;i<all.length();i++)if(all.optJSONObject(i).optBoolean("enabled"))count++;
        LinearLayout queueHeading=row();queueHeading.setGravity(Gravity.CENTER_VERTICAL);queueHeading.addView(label("项目队列 · 点击编辑",15),new LinearLayout.LayoutParams(0,-2,1));queueHeading.addView(badge("已启用 "+count+" 项",ACCENT,SOFT));body.addView(queueHeading);
        for(int i=0;i<all.length();i++){
            final int index=i;JSONObject p=all.optJSONObject(i),d=p.optJSONObject("data");boolean active=p.optBoolean("enabled");LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(dp(10),dp(2),dp(10),dp(2));surface(line,SURFACE,active?0xffb8dbd1:LINE,9);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);line.setLayoutParams(lp);
            TextView order=badge(String.format(Locale.ROOT,"%02d",i+1),active?ACCENT:MUTED,active?SOFT:BG);line.addView(order);
            LinearLayout details=wide?row():column();details.setGravity(Gravity.CENTER_VERTICAL);details.setPadding(dp(10),0,dp(8),0);String mode=p.optString("项目"),name=d.optString("项目名称",mode);name=name.equals(mode)?modeName(mode):name+" · "+modeName(mode);TextView title=label(name,14);title.setTextColor(INK);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setPadding(0,0,0,0);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);if(wide)details.addView(title,new LinearLayout.LayoutParams(0,-2,1));else details.addView(title);
            TextView summary=label(d.optString("次数","1")+" 次"+(mode.equals("每日战利品")&&"0".equals(d.optString("每日只执行一次"))?"  ·  每日一次":"")+((mode.equals("杯赛")||mode.equals("每日战利品"))?"  ·  位置 "+d.optString("杯赛位置","2"):""),11);summary.setPadding(0,0,dp(wide?16:0),0);details.addView(summary);line.addView(details,new LinearLayout.LayoutParams(0,-2,1));details.setOnClickListener(v->projectEditor(index));order.setOnClickListener(v->projectEditor(index));
            Switch enabled=new Switch(this);enabled.setMinHeight(dp(48));enabled.setMinWidth(dp(48));enabled.setContentDescription("启用项目 "+(i+1)+" "+name);enabled.setChecked(active);enabled.setOnCheckedChangeListener((v,on)->{try{p.put("enabled",on);store.save();render();}catch(Exception e){message(e.toString());}});line.addView(enabled);body.addView(line);
        }
        String recent=store.config.optString("lastRoute",store.config.optString("routeOnly",""));if(store.routes.containsKey(recent))body.addView(button("再次手动跑图 · "+store.matcher.displayName(recent),()->requestStart(recent,false)));
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
        if(mode.equals("多人上")||mode.equals("多人下"))fields.addView(label("多人上对应世界系列赛，多人下对应大师系列赛。新版菜单目前只选首屏序号 1–6 中有油的车辆，支持每次用一辆；车组、广告修车、自动掉分和到段位停跑尚未适配。",13));
        if(mode.equals("杯赛")||mode.equals("每日战利品"))fields.addView(label("杯赛位置：正数从左起计数，负数从右起计数；3 表示从左起第 3 个赛事。",13));
        if(mode.equals("每日战利品"))fields.addView(label("每天 00:00–00:30 自动跳过（设备本地时间，00:30 恢复）。每日一次指完成配置的全部场次；缺油、缺票或中断不算完成。",12));
        for(int i=0;i<spec.length();i++){
            JSONObject f=spec.getJSONObject(i);String key=f.getString("key"),type=f.getString("type");JSONArray options=f.getJSONArray("options");
            if(type.equals("EditText")){fields.addView(label(key,13));EditText e=edit(data.optString(key));if(key.equals("杯赛位置"))e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);fields.addView(e);views.put(key,e);}
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
                if(data.has("杯赛位置")){try{if(Integer.parseInt(data.getString("杯赛位置"))==0)throw new NumberFormatException();}catch(NumberFormatException e){throw new IllegalArgumentException("杯赛位置必须为非零整数");}}
                JSONObject p=store.config.getJSONArray("projects").getJSONObject(index);p.put("项目",mode);p.put("data",data);store.save();dialog.dismiss();render();
            }catch(Exception e){message(e.getMessage());}
        }));dialog.show();
    }
    private void routes(){
        LinearLayout summary=card();summary.addView(label("21 个地图   /   251 个赛段",18));summary.addView(label("保留原版跑法；待补充的赛段在游戏中自动左划。",12));body.addView(summary);
        EditText query=edit("");query.setSingleLine(true);query.setHint("搜索地图名称或内部索引");body.addView(query);LinearLayout list=column();body.addView(list);
        Runnable update=()->{list.removeAllViews();String q=query.getText().toString().trim().toLowerCase(Locale.ROOT);String group="";LinearLayout pair=null;int index=0,total=0;
            for(MapMatcher.Rule rule:store.matcher.rules){String key=rule.internal,display=rule.display;if(!display.toLowerCase(Locale.ROOT).contains(q)&&!key.toLowerCase(Locale.ROOT).contains(q))continue;Route r=store.routes.get(key);if(r==null)continue;
                String shortName=rule.aliases.isEmpty()?display:rule.aliases.get(0);String region=display.endsWith(shortName)?display.substring(0,display.length()-shortName.length()):"地图路线";if(region.isEmpty())region="地图路线";
                if(!group.equals(region)){if(wide&&pair!=null&&index%2==1)weighted(pair,new View(this),1);list.addView(label(region,18));group=region;index=0;pair=null;}
                LinearLayout item=card();item.setPadding(dp(16),dp(10),dp(16),dp(10));surface(item,SURFACE,LINE,12);TextView title=label(shortName,15);title.setTextColor(INK);item.addView(title);String state=r.raw.isEmpty()?"待补充 · 左划兜底":r.valid()?"已收录 · "+r.events.size()+" 个动作":"跑法需校验";TextView caption=label(state,11);caption.setTextColor(r.valid()?ACCENT:MUTED);item.addView(caption);item.setOnClickListener(v->routeEditor(key));
                if(wide){if(index%2==0){pair=row();LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,-2);gap.bottomMargin=dp(10);list.addView(pair,gap);}weighted(pair,item,index%2);index++;}else list.addView(item);total++;
            }
            if(wide&&pair!=null&&index%2==1)weighted(pair,new View(this),1);if(total==0)list.addView(label("没有找到匹配的地图",14));
        };
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){update.run();}public void afterTextChanged(Editable s){}});update.run();
    }
    private void routeEditor(String key){
        Route route=store.routes.get(key);LinearLayout box=column();box.setPadding(dp(16),0,dp(16),0);box.addView(label("内部索引："+key+"\n"+route.events.size()+" 个动作"+(route.valid()?"":"\n"+String.join("\n",route.errors)),13));
        if(route.raw.isEmpty())box.addView(label("暂未收录这张地图的跑法。多人赛识别到该地图后会自动左划兜底；补充有效跑法并保存后即可使用。",13));
        EditText raw=edit(route.raw);raw.setMinLines(5);box.addView(raw);
        box.addView(label("N:a:n:b 氮气；D:t 漂移；21 / 31 等为路线按钮；360 为双击漂移后补氮气。",12));
        box.addView(button("保存并手动跑图",()->{try{store.saveRoute(key,raw.getText().toString());requestStart(key,false);}catch(Exception e){message(e.getMessage());}}));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(store.matcher.displayName(key)).setView(scroll(box)).setNegativeButton("关闭",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{store.saveRoute(key,raw.getText().toString());dialog.dismiss();render();}catch(Exception e){message(e.getMessage());}}));dialog.show();
    }
    private void chooseRoute(java.util.function.Consumer<String> chosen){List<String> keys=store.routeKeys();keys.removeIf(key->store.routes.get(key).raw.isEmpty());String[] labels=new String[keys.size()];for(int i=0;i<labels.length;i++)labels[i]=store.matcher.displayName(keys.get(i));new AlertDialog.Builder(this).setTitle("选择已收录的跑法").setItems(labels,(d,w)->chosen.accept(keys.get(w))).setNegativeButton("取消",null).show();}
    private void diagnostics(){
        LinearLayout screen=card();screen.addView(label("屏幕识别",18));screen.addView(label("启动后，在游戏地图介绍或比赛画面点击悬浮窗或通知中的“取样”。",13));screen.addView(button("开始屏幕诊断",()->requestStart("",true)));screen.addView(button("查看最近截图",this::preview));body.addView(screen);
        LinearLayout matching=card();matching.addView(label("地图名称匹配",18));EditText text=edit("");text.setHint("输入画面上的地图名");matching.addView(text);
        matching.addView(button("测试名称匹配",()->{MapMatcher.Match m=store.matcher.match(text.getText().toString());List<String> c=new ArrayList<>();for(MapMatcher.Rule r:m.candidates)c.add(r.display+" → "+r.internal);message(c.isEmpty()?"没有匹配":String.join("\n",c));}));body.addView(matching);
        LinearLayout records=card();records.addView(label("运行记录",18));log=label(SessionState.logs().isEmpty()?"暂无运行记录":SessionState.logs(),12);log.setTypeface(Typeface.MONOSPACE);log.setTextIsSelectable(true);records.addView(log);body.addView(records);
    }
    private void preview(){File f=new File(getFilesDir(),"runtime/diagnostic.png");Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());if(b==null){message("还没有诊断截图");return;}ImageView image=new ImageView(this);image.setImageBitmap(b);image.setAdjustViewBounds(true);new AlertDialog.Builder(this).setTitle("归一化画面 · 720×1280").setView(scroll(image)).setPositiveButton("关闭",(d,w)->b.recycle()).show();}
    private void settings(){
        LinearLayout permissions=card();permissions.addView(label("设备权限",18));permissions.addView(label("授予触控、悬浮窗和通知权限后，即可在游戏中控制任务。",12));permissions.addView(button("开启无障碍触控",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        permissions.addView(button("开启悬浮控制",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())))));
        permissions.addView(button("允许通知控制",()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},102);else message("当前系统可直接显示任务通知");}));
        permissions.addView(button("领取奖励与广告选项",this::globalSettings));
        body.addView(permissions);LinearLayout parameters=card();parameters.addView(label("运行参数",18));
        parameters.addView(label("游戏包名",13));EditText pkg=edit(store.config.optString("gamePkg"));parameters.addView(pkg);
        parameters.addView(label("将截屏顺时针旋转至 720×1280 基准方向",13));Spinner rotation=new Spinner(this);rotation.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"不旋转","顺时针 90°","180°","顺时针 270°"}));int[] angles={0,90,180,270};for(int i=0;i<4;i++)if(angles[i]==store.config.optInt("rotation",90))rotation.setSelection(i);parameters.addView(rotation);
        parameters.addView(label("字库匹配阈值（50–100）",13));EditText accuracy=edit(String.valueOf(store.config.optInt("accuracy",80)));parameters.addView(accuracy);
        parameters.addView(label("动作延迟（0–5000ms）",13));EditText delay=edit(String.valueOf(store.config.optInt("delay",0)));parameters.addView(delay);
        parameters.addView(label("单局最长运行时间（60–1800 秒）",13));EditText raceTimeout=edit(String.valueOf(store.config.optInt("raceTimeout",900)));parameters.addView(raceTimeout);
        parameters.addView(label("路线执行方式",13));Spinner driver=new Spinner(this);driver.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ZX 队列时序","XC 起始延时"}));driver.setSelection(store.config.optInt("driverMode",1)-1);parameters.addView(driver);
        parameters.addView(button("保存设置",()->{try{
            int a=Integer.parseInt(accuracy.getText().toString()),d=Integer.parseInt(delay.getText().toString()),seconds=Integer.parseInt(raceTimeout.getText().toString());if(a<50||a>100||d<0||d>5000||seconds<60||seconds>1800)throw new IllegalArgumentException("请检查识别阈值、动作延迟和单局时间范围");
            String p=pkg.getText().toString().trim();if(!p.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IllegalArgumentException("包名格式错误");
            store.config.put("gamePkg",p).put("rotation",angles[rotation.getSelectedItemPosition()]).put("accuracy",a).put("delay",d).put("raceTimeout",seconds).put("driverMode",driver.getSelectedItemPosition()+1);store.save();message("设置已保存，下次启动时生效");
        }catch(Exception e){message(e.getMessage());}}));
        body.addView(parameters);
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
        requestStart(route,inspect,false);
    }
    private void requestStart(String route,boolean inspect,boolean resume){
        if(SessionState.running){message("请先停止当前任务");return;}
        if(inspect&&!Settings.canDrawOverlays(this)&&!getSystemService(NotificationManager.class).areNotificationsEnabled()){tab=3;render();message("请先开启悬浮窗或通知，以便在游戏中点击取样");return;}
        if(!inspect&&TouchService.instance==null){tab=3;render();message("请先在设置中开启无障碍服务");return;}
        try{
            if(!inspect&&route.isEmpty()&&!resume){
                boolean enabled=false;JSONArray p=store.config.getJSONArray("projects");for(int i=0;i<p.length();i++)enabled|=p.getJSONObject(i).optBoolean("enabled");if(!enabled)throw new IllegalArgumentException("请至少启用一个项目，或从路线页选择手动跑图");
            }
            store.config.put("routeOnly",route);if(!route.isEmpty())store.config.put("lastRoute",route);store.save();pendingInspect=inspect;pendingResume=resume;
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
        Intent service=new Intent(this,AutomationService.class).setAction(AutomationService.START).putExtra("result",result).putExtra("consent",data).putExtra("inspect",pendingInspect).putExtra("resume",pendingResume);
        startForegroundService(service);
        // Match an explicit activity resume, keeping launcher-specific reinitialization out of this transition.
        startActivity(new Intent().setComponent(launch.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void control(String action){if(!SessionState.running)return;if(action.equals(AutomationService.STOP))SessionState.status("已手动停止");startService(new Intent(this,AutomationService.class).setAction(action));}
    private void message(String text){new AlertDialog.Builder(this).setMessage(text==null?"操作失败":text).setPositiveButton("确定",null).show();}
    @Override protected void onResume(){super.onResume();SessionState.listener=()->{updateSession();if(log!=null)log.setText(SessionState.logs());};SessionState.changed();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("tab",tab);super.onSaveInstanceState(out);}
    @Override protected void onPause(){SessionState.listener=null;super.onPause();}
}
