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
import java.util.*;
import dev.funlee.a9.core.*;

public final class MainActivity extends Activity {
    private LocalStore store;
    private LinearLayout root,body;
    private ScrollView pageScroll;
    private TextView pageTitle,stateBadge,runStatus;
    // Keep page IDs stable; retired tools and advanced pages restore to permissions.
    private static final int RUN=0,ROUTES=1,SETTINGS=3,PREFERENCES=5;
    private final int[] navigationPages={RUN,PREFERENCES,ROUTES,SETTINGS};
    private final Button[] navigation=new Button[4];
    private LinearLayout issueCard;
    private RunIssue displayedIssue;
    private Button issueAction;
    private Toast preferenceToast;
    private Button startControl,pauseControl,stopControl;
    private static final int BG=0xfff5f7f9,SURFACE=0xffffffff,INK=0xff1d2939,MUTED=0xff59697d,ACCENT=0xff087f73,SOFT=0xffe7f4f0,LINE=0xffe4e9ef;
    private static final int PAGE_TITLE=22, DIALOG_TITLE=18, SECTION=16, BODY=14, DETAIL=12;
    private static final int SPACE_SMALL=4, SPACE_MEDIUM=8, SPACE_LARGE=12, SPACE_SECTION=16, DIALOG_GUTTER=24;
    private static final Typeface FONT=Typeface.create("sans-serif",Typeface.NORMAL),FONT_MEDIUM=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    private static final int PAGE_GUTTER=SPACE_SECTION, CARD_GAP=SPACE_LARGE, ROW_GAP=SPACE_MEDIUM, TOUCH_HEIGHT=48;
    private boolean wide;
    private int tab;
    private final Set<String> expandedTracks=new HashSet<>(),collapsedFilteredTracks=new HashSet<>();
    private String routeSelectedTrack="",routeSelectedMap="";
    private boolean routeOnlyWithoutCode;
    private boolean pendingInspect;
    private boolean pendingResume;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(saved!=null){
            tab=supportedPage(saved.getInt("tab",RUN));routeOnlyWithoutCode=saved.getBoolean("routeOnlyWithoutCode",false);
            pendingInspect=saved.getBoolean("pendingInspect");pendingResume=saved.getBoolean("pendingResume");
            routeSelectedTrack=saved.getString("routeSelectedTrack","");routeSelectedMap=saved.getString("routeSelectedMap","");
            expandedTracks.addAll(saved.getStringArrayList("expandedTracks")==null?Collections.emptyList():saved.getStringArrayList("expandedTracks"));
            collapsedFilteredTracks.addAll(saved.getStringArrayList("collapsedFilteredTracks")==null?Collections.emptyList():saved.getStringArrayList("collapsedFilteredTracks"));
        }
        try{store=new LocalStore(this);show();}catch(Exception e){TextView error=new TextView(this);error.setText(getString(R.string.init_error,e.toString()));setContentView(error);}
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable shape(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private void surface(View view,int fill,int stroke,int radius){view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x14087f73),shape(fill,stroke,radius),null));}
    private void typography(TextView view,int size,boolean emphasis){
        view.setTextSize(size);view.setTypeface(emphasis?FONT_MEDIUM:FONT);
        view.setIncludeFontPadding(false);view.setLineSpacing(dp(2),1);
    }
    private TextView label(String text,int size){
        TextView label=new TextView(this);
        label.setText(text);
        typography(label,size,false);
        label.setTextColor(MUTED);
        label.setPadding(0,dp(SPACE_SMALL),0,dp(SPACE_SMALL));
        return label;
    }
    private TextView heading(String text,int size){
        TextView heading=label(text,size);
        heading.setTextColor(INK);
        heading.setTypeface(FONT_MEDIUM);
        return heading;
    }
    private Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);typography(b,BODY,true);b.setMinHeight(dp(TOUCH_HEIGHT));b.setMinimumHeight(dp(TOUCH_HEIGHT));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));b.setElevation(0);b.setStateListAnimator(null);
        boolean primary=text.startsWith("开始")||text.startsWith("保存"),danger=text.equals("停止");
        surface(b,primary?ACCENT:danger?0xfffceeed:SURFACE,primary||danger?0:LINE,10);b.setTextColor(primary?SURFACE:danger?0xffb74d49:INK);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(SPACE_SMALL),0,dp(SPACE_SMALL));b.setLayoutParams(p);
        b.setOnClickListener(v->{try{action.run();}catch(Exception e){message(e.getMessage());}});return b;}
    private Button compactButton(String text,Runnable action){Button b=button(text,action);b.setPadding(dp(SPACE_MEDIUM),dp(SPACE_SMALL),dp(SPACE_MEDIUM),dp(SPACE_SMALL));b.setTextSize(BODY);return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private LinearLayout card(){LinearLayout box=column();box.setPadding(dp(PAGE_GUTTER),dp(CARD_GAP),dp(PAGE_GUTTER),dp(CARD_GAP));box.setBackground(shape(SURFACE,LINE,BODY));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(CARD_GAP));box.setLayoutParams(p);return box;}
    private TextView badge(String text,int color,int fill){TextView b=label(text,DETAIL);b.setTextColor(color);b.setGravity(Gravity.CENTER);b.setPadding(dp(SPACE_MEDIUM),dp(SPACE_SMALL),dp(SPACE_MEDIUM),dp(SPACE_SMALL));b.setBackground(shape(fill,0,7));return b;}
    private ScrollView scroll(View v){ScrollView s=new ScrollView(this);s.setFillViewport(false);s.setClipToPadding(false);s.setVerticalScrollBarEnabled(false);s.addView(v);return s;}
    private EditText edit(String value){EditText e=new EditText(this);e.setText(value);typography(e,BODY,false);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));e.setBackground(shape(BG,LINE,9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(ROW_GAP));e.setLayoutParams(p);e.setMinHeight(dp(TOUCH_HEIGHT));return e;}
    private void weighted(LinearLayout row,View view,int index){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(index==0?0:dp(ROW_GAP),0,0,0);row.addView(view,p);}
    private void brand(LinearLayout target){
        LinearLayout brand=row();brand.setGravity(Gravity.CENTER_VERTICAL);ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.ic_steering_mark);mark.setContentDescription("A9 方向盘标志");
        brand.addView(mark,new LinearLayout.LayoutParams(dp(wide?44:36),dp(wide?44:36)));
        boolean stack=wide||getResources().getConfiguration().screenWidthDp<350
                ||getResources().getConfiguration().fontScale>1.15f;
        LinearLayout names=stack?column():row();
        names.setGravity(Gravity.CENTER_VERTICAL);
        names.setPadding(dp(SPACE_LARGE),0,0,0);
        TextView title=heading(getString(R.string.app_name),SECTION);
        title.setPadding(0,0,0,0);
        names.addView(title);
        TextView caption=label("游戏自动化助手",DETAIL);
        caption.setPadding(dp(stack?0:8),0,0,0);
        names.addView(caption);
        brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        target.addView(brand);
    }
    private void show(){
        wide=getResources().getConfiguration().screenWidthDp>=840||(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE&&getResources().getConfiguration().screenWidthDp>=600);root=wide?row():column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        LinearLayout rail=column();rail.setPadding(dp(wide?24:PAGE_GUTTER),dp(wide?24:8),dp(wide?24:PAGE_GUTTER),dp(wide?16:4));if(wide){rail.setBackgroundColor(SURFACE);root.addView(rail,new LinearLayout.LayoutParams(dp(218),-1));}else root.addView(rail);
        brand(rail);LinearLayout nav=wide?column():row();LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(wide?24:8);rail.addView(nav,np);
        String[] names={"任务编排","任务偏好","地图配置","偏好设置"};for(int i=0;i<names.length;i++){final int n=navigationPages[i];Button b=button(names[i],()->navigate(n));b.setGravity(wide?Gravity.CENTER_VERTICAL|Gravity.START:Gravity.CENTER);navigation[i]=b;if(!wide){b.setTextSize(DETAIL);b.setPadding(dp(2),dp(SPACE_SMALL),dp(2),dp(SPACE_SMALL));}if(wide)nav.addView(b);else weighted(nav,b,i);}
        if(wide){View space=new View(this);rail.addView(space,new LinearLayout.LayoutParams(1,0,1));rail.addView(badge("本地运行 · 无需账号",ACCENT,SOFT));TextView version=label(getString(R.string.app_name)+"  ·  "+version(),DETAIL);version.setPadding(0,dp(SPACE_LARGE),0,0);rail.addView(version);}
        LinearLayout content=column();
        int gutter=wide?24:PAGE_GUTTER;
        content.setPadding(dp(gutter),dp(wide?16:4),dp(gutter),dp(SPACE_MEDIUM));
        root.addView(content,wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout pageHeading=row();
        pageHeading.setGravity(Gravity.CENTER_VERTICAL);
        pageTitle=heading("",PAGE_TITLE);
        pageHeading.addView(pageTitle,new LinearLayout.LayoutParams(0,-2,1));
        stateBadge=badge("",ACCENT,SOFT);
        pageHeading.addView(stateBadge);
        content.addView(pageHeading);
        body=column();
        pageScroll=scroll(body);
        LinearLayout.LayoutParams pageLayout=new LinearLayout.LayoutParams(-1,0,1);
        pageLayout.topMargin=dp(ROW_GAP);
        content.addView(pageScroll,pageLayout);
        setContentView(root);render();
    }
    private static int supportedPage(int page){return page==2||page==4?SETTINGS:page==RUN||page==ROUTES||page==SETTINGS||page==PREFERENCES?page:RUN;}
    private void navigate(int page){if(preferenceToast!=null)preferenceToast.cancel();tab=supportedPage(page);render();pageScroll.scrollTo(0,0);}
    private void render(){
        body.removeAllViews();permissionRefreshers.clear();issueCard=null;displayedIssue=null;issueAction=null;startControl=null;pauseControl=null;stopControl=null;runStatus=null;
        pageTitle.setText(tab==ROUTES?"地图配置":tab==SETTINGS?"偏好设置":tab==PREFERENCES?"任务偏好":"任务编排");
        for(int i=0;i<navigation.length;i++){boolean active=navigationPages[i]==tab;surface(navigation[i],active?SOFT:wide?SURFACE:BG,0,9);navigation[i].setTextColor(active?ACCENT:MUTED);}
        switch(tab){case RUN:projects();break;case ROUTES:routes();break;case PREFERENCES:taskPreferences();break;default:settings();}updateSession();
    }
    private void updateSession(){for(Runnable refresh:permissionRefreshers)refresh.run();updateIssue();if(stateBadge==null)return;String state=SessionState.running?(SessionState.paused?"已暂停":"运行中"):"";stateBadge.setText(state);stateBadge.setVisibility(SessionState.running?View.VISIBLE:View.GONE);if(startControl!=null)startControl.setEnabled(!SessionState.running);if(pauseControl!=null){pauseControl.setText(SessionState.paused?"继续":"暂停");pauseControl.setAlpha(SessionState.running?1:.45f);}if(stopControl!=null)stopControl.setAlpha(SessionState.running?1:.45f);updateRunStatus();}
    private void updateRunStatus(){
        if(runStatus==null)return;
        String phase=SessionState.running?(SessionState.paused?"已暂停":SessionState.hud):RunStatusRecord.idleStatus(this);
        int lineEnd=phase.indexOf('\n');
        if(lineEnd>=0)phase=phase.substring(0,lineEnd);
        if(phase.trim().isEmpty())phase=SessionState.status;
        if(!phase.contentEquals(runStatus.getText()))runStatus.setText(phase);
    }
    private void updateIssue(){
        if(issueCard==null)return;
        RunIssue issue=SessionState.issue;
        if(issue!=null&&issue.action==RunIssue.Action.ACCESSIBILITY&&TouchService.instance!=null){
            SessionState.clearIssue();
            issue=null;
        }
        issueCard.setVisibility(issue==null?View.GONE:View.VISIBLE);
        if(issueAction!=null)issueAction.setEnabled(!SessionState.running);
        if(issue==null||issue==displayedIssue)return;
        displayedIssue=issue;issueCard.removeAllViews();
        TextView title=heading(issue.title,SECTION);title.setTextColor(0xffa15421);issueCard.addView(title);
        issueCard.addView(label(issue.guidance,DETAIL));
        RunIssue shown=issue;
        issueAction=button(shown.actionLabel,()->resolveIssue(shown));issueAction.setEnabled(!SessionState.running);issueCard.addView(issueAction);
    }
    private void resolveIssue(RunIssue issue){
        switch(issue.action){
            case ACCESSIBILITY:startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));break;
            case CAPTURE:requestStart("",SessionState.inspecting,!SessionState.inspecting&&SessionRecord.available(this));break;
            case SETTINGS:navigate(SETTINGS);break;
            case TASK_PREFERENCES:navigate(PREFERENCES);break;
            case GAME:
                Intent game=getPackageManager().getLaunchIntentForPackage(store.config.optString("gamePkg"));
                if(game==null){showRunIssue("未找到游戏");return;}
                startActivity(new Intent().setComponent(game.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));break;
            case QUEUE:SessionState.clearIssue();navigate(RUN);break;
        }
    }
    private void showRunIssue(String detail){
        SessionState.failure(detail);
        if(!SessionState.running)RunStatusRecord.save(this,false,SessionState.status);
        navigate(RUN);
    }
    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "";}}
    private String modeName(String mode){return MultiplayerUi.displayMode(mode);}
    private String taskTypeTag(String mode){
        switch(mode){
            case "每日战利品":return "战利品";
            case "杯赛":return "杯赛";
            case "特殊赛事":return "特赛";
            case "多人下":return "大师赛";
            case "多人上":return "世界赛";
            case "车联":return "车联赛";
            default:return modeName(mode);
        }
    }
    private void projects(){
        LinearLayout controls=card();controls.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));
        LinearLayout actions=row();startControl=compactButton(wide?"开始任务":"开始",()->requestStart("",false));pauseControl=compactButton("暂停",()->control(AutomationService.PAUSE));stopControl=compactButton("停止",()->control(AutomationService.STOP));weighted(actions,startControl,0);weighted(actions,pauseControl,1);weighted(actions,stopControl,2);
        if(SessionRecord.available(this))weighted(actions,compactButton(wide?"继续上次":"续跑",()->requestStart("",false,true)),3);
        controls.addView(actions);
        runStatus=label("",DETAIL);
        runStatus.setTextColor(MUTED);
        runStatus.setPadding(0,dp(SPACE_SMALL),0,0);
        controls.addView(runStatus);
        body.addView(controls);
        issueCard=card();issueCard.setBackground(shape(0xfffff8ef,0xfff0dac3,BODY));issueCard.setVisibility(View.GONE);body.addView(issueCard);
        JSONArray all=store.config.optJSONArray("projects");int count=0;for(int i=0;i<all.length();i++)if(all.optJSONObject(i).optBoolean("enabled"))count++;
        LinearLayout queueHeading=row();queueHeading.setGravity(Gravity.CENTER_VERTICAL);queueHeading.addView(heading("项目队列",SECTION),new LinearLayout.LayoutParams(0,-2,1));queueHeading.addView(badge("已启用 "+count+" 项",ACCENT,SOFT));body.addView(queueHeading);body.addView(label("拖动序号调整执行顺序",DETAIL));
        LinearLayout queueActions=row();weighted(queueActions,compactButton("新增任务",()->projectEditor(-1)),0);
        weighted(queueActions,compactButton("重置编排",this::confirmResetProjects),1);
        LinearLayout.LayoutParams queueActionsLayout=new LinearLayout.LayoutParams(-1,-2);queueActionsLayout.bottomMargin=dp(ROW_GAP);body.addView(queueActions,queueActionsLayout);
        if(all.length()==0)body.addView(label("暂无任务，点击“新增任务”添加，或“重置编排”恢复默认 5 项。",BODY));
        TaskQueueView queue=new TaskQueueView(this,pageScroll,(from,to)->{try{store.moveProject(from,to);render();}catch(Exception e){message(e.getMessage());}});body.addView(queue);
        for(int i=0;i<all.length();i++){
            final int index=i;JSONObject p=all.optJSONObject(i),d=p.optJSONObject("data");boolean active=p.optBoolean("enabled");LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(dp(SPACE_SMALL),dp(SPACE_SMALL),dp(SPACE_LARGE),dp(SPACE_SMALL));surface(line,SURFACE,active?0xffb8dbd1:LINE,9);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(ROW_GAP);line.setLayoutParams(lp);
            TextView order=badge(String.format(Locale.ROOT,"≡ %02d",i+1),active?ACCENT:MUTED,active?SOFT:BG);order.setMinHeight(dp(48));order.setContentDescription("拖动排序任务 "+(i+1));line.addView(order);
            LinearLayout details=column();details.setGravity(Gravity.CENTER_VERTICAL);details.setPadding(dp(SPACE_LARGE),dp(SPACE_SMALL),dp(SPACE_MEDIUM),dp(SPACE_SMALL));
            String mode=p.optString("项目"),name=d.optString("项目名称",mode).trim();if(name.isEmpty()||name.equals(mode))name=modeName(mode);
            TextView title=heading(name,BODY);title.setPadding(0,0,0,0);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);details.addView(title,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout metadata=row();metadata.setGravity(Gravity.CENTER_VERTICAL);metadata.setPadding(0,dp(SPACE_SMALL),0,0);
            TextView type=badge(taskTypeTag(mode),ACCENT,SOFT);type.setTextSize(DETAIL);type.setSingleLine(true);type.setPadding(dp(SPACE_MEDIUM),dp(2),dp(SPACE_MEDIUM),dp(2));metadata.addView(type);
            TextView summary=label(d.optString("次数","1")+" 次"+((mode.equals("杯赛")||mode.equals("每日战利品"))?" · 位置 "+d.optString("杯赛位置","2"):""),DETAIL);summary.setPadding(dp(SPACE_MEDIUM),0,0,0);summary.setMaxLines(getResources().getConfiguration().fontScale>1.15f?2:1);summary.setEllipsize(TextUtils.TruncateAt.END);metadata.addView(summary,new LinearLayout.LayoutParams(0,-2,1));
            details.addView(metadata,new LinearLayout.LayoutParams(-1,-2));line.addView(details,new LinearLayout.LayoutParams(0,-2,1));details.setOnClickListener(v->projectEditor(index));
            Switch enabled=new Switch(this);enabled.setMinHeight(dp(48));enabled.setMinWidth(dp(48));enabled.setContentDescription("启用项目 "+(i+1)+" "+name);enabled.setChecked(active);enabled.setOnCheckedChangeListener((v,on)->{try{p.put("enabled",on);store.save();render();}catch(Exception e){message(e.toString());}});line.addView(enabled);
            Button delete=compactButton("删除",()->{try{store.deleteProject(index);render();}catch(Exception e){message(e.getMessage());}});delete.setContentDescription("删除任务 "+(i+1)+" "+name);delete.setTextColor(0xffb74d49);LinearLayout.LayoutParams deleteLayout=new LinearLayout.LayoutParams(-2,-2);deleteLayout.leftMargin=dp(SPACE_MEDIUM);line.addView(delete,deleteLayout);queue.addTask(line,order,details);
        }
    }
    private void confirmResetProjects(){
        showDialog(new AlertDialog.Builder(this).setTitle("重置任务编排？")
                .setMessage("重置将舍弃当前任务编排配置，并恢复默认的 5 个任务配置。确认重置吗？")
                .setNegativeButton("取消",null)
                .setPositiveButton("确认重置",(dialog,which)->{
                    try{store.resetProjects();render();Toast.makeText(this,"已恢复默认的 5 个任务配置",Toast.LENGTH_SHORT).show();}
                    catch(Exception e){message(e.getMessage());}
                }).create());
    }
    private void projectEditor(int index){
        if(index>=0){
            try{
                JSONObject project=store.config.getJSONArray("projects").getJSONObject(index);
                form(index,project.getString("项目"),new JSONObject(project.getJSONObject("data").toString()));
            }catch(Exception e){message(e.getMessage());}
            return;
        }
        String[] labels=Arrays.stream(LocalStore.MODES).map(MultiplayerUi::displayMode).toArray(String[]::new);
        showDialog(new AlertDialog.Builder(this).setTitle("选择项目类型").setAdapter(listOptions(Arrays.asList(labels)),(dialog,which)->{
            try{
                String mode=LocalStore.MODES[which];form(-1,mode,store.defaults(mode));
            }catch(Exception e){message(e.getMessage());}
        }).setNegativeButton("取消",null).create());
    }
    private void form(int index,String mode,JSONObject data) throws Exception {
        LinearLayout fields=dialogFields();JSONArray spec=store.forms.getJSONArray(mode);Map<String,View> views=new LinkedHashMap<>();
        if(mode.equals("多人上"))fields.addView(label("比赛时自动识别地图并执行“地图配置”中的跑法；识别失败或缺少有效跑法时左划兜底。",DETAIL));
        if(mode.equals("多人上"))fields.addView(label("优先使用当前段位配置的车辆。该段未配置或配置车全部无油时，逐级尝试较低段位的配置。序号按各段车库从左到右、每列从上到下计算，支持每次用一辆。",DETAIL));
        if(mode.equals("多人下"))fields.addView(label("按配置的车辆序号选车。每列先上后下，再从左往右累计，跨屏时核验车辆位置，支持每次用一辆。",DETAIL));
        if(mode.equals("每日战利品"))fields.addView(label("每天 00:00–00:30 自动跳过（设备本地时间，00:30 恢复）。其余时间按配置次数参与每轮任务循环。",DETAIL));
        for(int i=0;i<spec.length();i++){
            JSONObject f=spec.getJSONObject(i);String key=f.getString("key"),type=f.getString("type");JSONArray options=f.getJSONArray("options");
            if(key.equals("称霸位置"))continue; // Rendered alongside the 称霸模式 checkbox.
            if(type.equals("TaskRoute")){taskRouteFields(fields,views,data,mode);}
            else if(key.equals("称霸模式")){
                CheckBox enabled=checkBox("称霸模式");
                enabled.setChecked("0".equals(data.optString(key)));
                fields.addView(enabled);
                views.put(key,enabled);

                LinearLayout chapterFields=column();
                String title="称霸模式章节";
                chapterFields.addView(heading(title,BODY));
                EditText chapter=edit(data.optString("称霸位置","1"));
                chapter.setContentDescription(title);
                chapterFields.addView(chapter);
                fields.addView(chapterFields);
                views.put("称霸位置",chapter);
                chapterFields.setVisibility(enabled.isChecked()?View.VISIBLE:View.GONE);
                enabled.setOnCheckedChangeListener((view,checked)->chapterFields.setVisibility(checked?View.VISIBLE:View.GONE));
            }
            else if(type.equals("EditText")){
                boolean eventPosition=key.equals("杯赛位置");String title=eventPosition?"赛事位置":key;
                fields.addView(heading(title,BODY));
                if(eventPosition)fields.addView(label("正数从左起计数，负数从右起计数；3 表示从左起第 3 个赛事。",DETAIL));
                EditText e=edit(data.optString(key));e.setContentDescription(title);if(eventPosition)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);fields.addView(e);views.put(key,e);
            }
            else if(type.equals("CheckBoxGroup")){
                LinearLayout checks=column();String selected=","+data.optString(key)+",";
                for(int n=0;n<options.length();n++){CheckBox cb=checkBox(options.getString(n));cb.setTag(n);cb.setChecked(selected.contains(","+n+","));checks.addView(cb);}fields.addView(heading(key,BODY));fields.addView(checks);views.put(key,checks);
            }else{
                fields.addView(heading(key,BODY));Spinner spinner=outlinedSpinner();String[] choices=new String[options.length()];for(int j=0;j<choices.length;j++)choices[j]=options.getString(j);
                spinner.setAdapter(spinnerOptions(Arrays.asList(choices)));
                LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,-2);
                spacing.bottomMargin=dp(ROW_GAP);spinner.setLayoutParams(spacing);
                int selection=0;try{selection=Integer.parseInt(data.optString(key,"0"));}catch(Exception ignored){}spinner.setSelection(Math.min(Math.max(0,selection),Math.max(0,choices.length-1)));
                fields.addView(spinner);views.put(key,spinner);
            }
        }
        if(mode.equals("多人上"))worldRewardFields(fields,views,data);
        if(mode.equals("多人下"))fields.addView(label("比赛时自动识别地图并执行“地图配置”中的跑法；识别失败或缺少有效跑法时左划兜底。",DETAIL));
        else if(!mode.equals("多人上")&&!LocalStore.hasTaskRoute(mode)){
            fields.addView(label("选路为“百分比选路”时，线路填写进度指令；“自定义选图”时可通过下方按钮填入本地地图。",DETAIL));
            fields.addView(button("选择本地路线",()->chooseRoute(key->{
                String prefix=mode.equals("车联")?"车手联会":mode.equals("每日战利品")?"杯赛":mode;
                View target=views.get(prefix+"线路");if(target instanceof EditText)((EditText)target).setText(store.routes.get(key).raw);
                View choice=views.get(prefix+"选路");if(choice instanceof Spinner)((Spinner)choice).setSelection(3);
            })));
        }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle((index<0?"新增任务 · ":(index+1)+". ")+MultiplayerUi.displayMode(mode)).setView(scroll(fields)).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{
            try{
                for(Map.Entry<String,View> e:views.entrySet()){
                    String value="";View view=e.getValue();
                    if(view instanceof EditText)value=((EditText)view).getText().toString().trim();
                    else if(view instanceof CheckBox)value=((CheckBox)view).isChecked()?"0":"";
                    else if(view instanceof Button)value=String.valueOf(view.getTag());
                    else if(view instanceof Spinner){int position=((Spinner)view).getSelectedItemPosition();value=e.getKey().equals("路线来源")?((List<String>)view.getTag()).get(position):String.valueOf(position);}
                    else if(view instanceof LinearLayout){List<String> selected=new ArrayList<>();for(int j=0;j<((LinearLayout)view).getChildCount();j++){CheckBox cb=(CheckBox)((LinearLayout)view).getChildAt(j);if(cb.isChecked())selected.add(String.valueOf(cb.getTag()));}value=String.join(",",selected);}
                    data.put(e.getKey(),value);
                }
                int times=Integer.parseInt(data.optString("次数","1"));if(times<1||times>10000)throw new IllegalArgumentException("执行次数需为 1–10000");
                if(data.has("杯赛位置")){try{if(Integer.parseInt(data.getString("杯赛位置"))==0)throw new NumberFormatException();}catch(NumberFormatException e){throw new IllegalArgumentException("赛事位置必须为非零整数");}}
                if(mode.equals("特殊赛事")){
                    if(!data.optString("称霸模式").isEmpty()&&!data.optString("称霸位置").matches("[1-9][0-9]*"))
                        throw new IllegalArgumentException("称霸模式章节请填写游戏显示的正整数");
                    if(!data.optString("特殊赛事阶段").matches("[1-9][0-9]*"))
                        throw new IllegalArgumentException("特殊赛事阶段请填写游戏显示的正整数");
                    if("1".equals(data.optString("路线来源"))&&data.optString("跑法代码").isEmpty())
                        throw new IllegalArgumentException("请填写跑法代码，或选择自动识别地图");
                    if("0".equals(data.optString("路线来源"))&&data.optString("已配置地图").isEmpty())
                        throw new IllegalArgumentException("请选择已配置地图，或选择自动识别地图");
                }
                if(LocalStore.hasTaskRoute(mode))store.validateTaskRoute(data);
                if(mode.equals("多人上")&&"0".equals(data.optString("自选奖励")))DynamicRewards.rules(data.optString("自选奖励规则"));
                if(index<0)store.addProject(mode,data);
                else{JSONObject p=store.config.getJSONArray("projects").getJSONObject(index);p.put("项目",mode);p.put("data",data);store.save();}
                dialog.dismiss();render();
            }catch(Exception e){message(e.getMessage());}
        }));showDialog(dialog);
    }
    private void worldRewardFields(LinearLayout fields,Map<String,View> views,JSONObject data) {
        CheckBox automatic=checkBox("自选奖励");
        automatic.setChecked("0".equals(data.optString("自选奖励")));
        fields.addView(automatic);views.put("自选奖励",automatic);
        fields.addView(label("关闭时，遇到动态奖励会停止任务，等你手动选卡。",DETAIL));
        LinearLayout rulesFields=column();
        EditText rules=edit(data.optString("自选奖励规则"));
        rules.setContentDescription("自选奖励规则");rules.setHint("1000:4,31700:2");
        rulesFields.addView(rules);views.put("自选奖励规则",rules);
        rulesFields.addView(label("留空时选最左边的非红币奖励。也可填写“里程碑分数:从左数第几张”，多档用逗号分隔。卡片或档位无法确认时，请手动选卡。",DETAIL));
        fields.addView(rulesFields);
        rulesFields.setVisibility(automatic.isChecked()?View.VISIBLE:View.GONE);
        automatic.setOnCheckedChangeListener((view,checked)->rulesFields.setVisibility(checked?View.VISIBLE:View.GONE));
    }
    private void taskRouteFields(LinearLayout fields,Map<String,View> views,JSONObject data,String mode) {
        fields.addView(heading("地图路线",BODY));
        Spinner source=outlinedSpinner();source.setContentDescription("路线来源");
        boolean automatic=LocalStore.supportsAutoRoute(mode);
        List<String> values=automatic?List.of("2","0","1"):List.of("0","1");source.setTag(values);
        source.setAdapter(spinnerOptions(automatic?List.of("自动识别地图","选择已配置地图","自定义跑法代码"):List.of("选择已配置地图","自定义跑法代码")));
        fields.addView(source);views.put("路线来源",source);
        LinearLayout mapFields=column(),codeFields=column();
        String selected=data.optString("已配置地图");
        Button map=button(selected.isEmpty()?"选择地图":store.matcher.displayName(selected),()->{});
        map.setContentDescription("已配置地图");map.setTag(selected);
        map.setOnClickListener(v->chooseConfiguredMap(key->{map.setTag(key);map.setText(store.matcher.displayName(key));}));
        mapFields.addView(map);mapFields.addView(label("使用所选地图的最新跑法，可在“地图配置”中修改。",DETAIL));
        views.put("已配置地图",map);fields.addView(mapFields);
        EditText code=edit(data.optString("跑法代码"));code.setContentDescription("跑法代码");
        code.setTypeface(Typeface.MONOSPACE);code.setMinLines(3);code.setGravity(Gravity.TOP|Gravity.START);
        codeFields.addView(code);codeFields.addView(label("填写跑法代码，例如：0,N:0:1:100,20,21。",DETAIL));
        views.put("跑法代码",code);fields.addView(codeFields);
        TextView hint=label("自动识别地图名并使用已配置的跑法；识别失败或暂无有效跑法时左划兜底。",DETAIL);fields.addView(hint);
        Runnable refresh=()->{String value=values.get(source.getSelectedItemPosition());mapFields.setVisibility(value.equals("0")?View.VISIBLE:View.GONE);codeFields.setVisibility(value.equals("1")?View.VISIBLE:View.GONE);
            hint.setVisibility(value.equals("2")?View.VISIBLE:View.GONE);};
        source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){refresh.run();}
            public void onNothingSelected(AdapterView<?> parent){}
        });
        source.setSelection(Math.max(0,values.indexOf(data.optString("路线来源",automatic?"2":"0"))));refresh.run();
    }
    private void chooseConfiguredMap(java.util.function.Consumer<String> chosen) {
        List<String> keys=store.routeKeys();keys.removeIf(key->!store.routes.get(key).valid());
        if(keys.isEmpty()){message("暂无已配置跑法的地图，请先在地图配置中添加或完善跑法");return;}
        LinearLayout content=dialogFields();EditText search=edit("");search.setHint("搜索赛道或地图");search.setContentDescription("搜索地图");content.addView(search);
        ListView list=new ListView(this);content.addView(list,new LinearLayout.LayoutParams(-1,dp(320)));
        List<String> visible=new ArrayList<>();
        Runnable filter=()->{visible.clear();List<String> labels=new ArrayList<>();String query=search.getText().toString().trim();
            for(String key:keys){String name=store.matcher.displayName(key);if(name.contains(query)||key.contains(query)){visible.add(key);labels.add(name);}}
            list.setAdapter(listOptions(labels));};
        AlertDialog picker=new AlertDialog.Builder(this).setTitle("选择已配置地图").setView(content).setNegativeButton("取消",null).create();
        list.setOnItemClickListener((parent,view,position,id)->{chosen.accept(visible.get(position));picker.dismiss();});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter.run();}public void afterTextChanged(Editable e){}});
        filter.run();showDialog(picker);
    }
    private String routeShortName(MapMatcher.Rule rule){return rule.aliases.isEmpty()?rule.display:rule.aliases.get(0);}
    private ArrayAdapter<String> listOptions(List<String> labels){
        return new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){
            @Override public View getView(int position,View recycled,ViewGroup parent){
                TextView text=label(getItem(position),BODY);text.setTextColor(INK);
                text.setGravity(Gravity.CENTER_VERTICAL);text.setMinHeight(dp(TOUCH_HEIGHT));
                text.setPadding(dp(PAGE_GUTTER),dp(ROW_GAP),dp(PAGE_GUTTER),dp(ROW_GAP));return text;
            }
        };
    }
    private void showDialog(AlertDialog dialog){
        dialog.show();
        int titleId=getResources().getIdentifier("alertTitle","id","android");
        TextView title=dialog.findViewById(titleId);
        if(title!=null){typography(title,DIALOG_TITLE,true);title.setTextColor(INK);}
        TextView message=dialog.findViewById(android.R.id.message);
        if(message!=null){typography(message,BODY,false);message.setTextColor(INK);}
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){
            Button action=dialog.getButton(which);if(action==null)continue;
            typography(action,BODY,true);action.setAllCaps(false);action.setMinHeight(dp(TOUCH_HEIGHT));action.setTextColor(ACCENT);
            action.setPadding(dp(SPACE_LARGE),dp(ROW_GAP),dp(SPACE_LARGE),dp(ROW_GAP));
        }
    }
    private ArrayAdapter<String> spinnerOptions(List<String> labels){
        return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){
            @Override public View getView(int position,View recycled,ViewGroup parent){
                TextView text=recycled instanceof TextView?(TextView)recycled:label("",BODY);
                text.setText(getItem(position));
                text.setTextColor(INK);
                text.setPadding(0,0,0,0);
                text.setSingleLine(true);
                text.setEllipsize(TextUtils.TruncateAt.END);
                return text;
            }
            @Override public View getDropDownView(int position,View recycled,ViewGroup parent){
                TextView text=label(getItem(position),BODY);
                text.setTextColor(INK);
                text.setGravity(Gravity.CENTER_VERTICAL);
                text.setMinHeight(dp(TOUCH_HEIGHT));
                text.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));
                return text;
            }
        };
    }
    private Spinner outlinedSpinner(){
        Spinner spinner=new Spinner(this,Spinner.MODE_DROPDOWN);
        Drawable arrow=getDrawable(android.R.drawable.arrow_down_float).mutate();
        arrow.setTint(MUTED);
        LayerDrawable background=new LayerDrawable(new Drawable[]{shape(BG,LINE,9),arrow});
        background.setLayerSize(1,dp(SPACE_LARGE),dp(SPACE_LARGE));
        background.setLayerGravity(1,Gravity.END|Gravity.CENTER_VERTICAL);
        background.setLayerInsetEnd(1,dp(SPACE_LARGE));
        spinner.setBackground(background);
        spinner.setPaddingRelative(dp(SPACE_LARGE),dp(SPACE_SMALL),dp(28),dp(SPACE_SMALL));
        spinner.setMinimumHeight(dp(TOUCH_HEIGHT));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.bottomMargin=dp(ROW_GAP);spinner.setLayoutParams(layout);
        return spinner;
    }
    private CheckBox checkBox(String text){
        CheckBox checkbox=new CheckBox(this);
        checkbox.setText(text);
        typography(checkbox,BODY,false);
        checkbox.setTextColor(INK);
        checkbox.setMinHeight(dp(TOUCH_HEIGHT));
        checkbox.setButtonTintList(ColorStateList.valueOf(ACCENT));
        return checkbox;
    }
    private String routeTrack(MapMatcher.Rule rule){
        if(store.isCustomMap(rule.internal))return store.customMapTrack(rule.internal);
        String shortName=routeShortName(rule);
        String track=rule.display.endsWith(shortName)?rule.display.substring(0,rule.display.length()-shortName.length()):"";
        return track.isEmpty()?"地图配置":track;
    }
    private int routesWithoutCode(List<MapMatcher.Rule> rules){
        int count=0;for(MapMatcher.Rule rule:rules)if(store.routes.get(rule.internal).raw.isEmpty())count++;return count;
    }
    private void routeCards(LinearLayout target,List<MapMatcher.Rule> rules){
        LinearLayout pair=null;int index=0;
        for(MapMatcher.Rule rule:rules){
            String key=rule.internal;Route r=store.routes.get(key);
            LinearLayout item=card();item.setTag("map:"+key);item.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));((LinearLayout.LayoutParams)item.getLayoutParams()).bottomMargin=dp(ROW_GAP);surface(item,SURFACE,LINE,12);
            LinearLayout titleRow=row();titleRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView title=heading(routeShortName(rule),BODY);title.setPadding(0,0,dp(SPACE_MEDIUM),0);titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            boolean custom=store.isCustomMap(key);titleRow.addView(badge(custom?"自定义":"系统内置",custom?ACCENT:MUTED,custom?SOFT:BG));item.addView(titleRow);
            String state=r.raw.isEmpty()?"待补充 · 左划兜底":r.valid()?"已收录 · "+r.events.size()+" 个动作":"跑法需校验";
            TextView caption=label(state,DETAIL);caption.setPadding(0,dp(2),0,0);caption.setTextColor(r.valid()?ACCENT:MUTED);item.addView(caption);item.setFocusable(true);item.setOnClickListener(v->routeEditor(key));
            if(wide){
                if(index%2==0){pair=row();LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,-2);gap.bottomMargin=dp(ROW_GAP);target.addView(pair,gap);}
                weighted(pair,item,index%2);index++;
            }else target.addView(item);
        }
        if(wide&&pair!=null&&index%2==1)weighted(pair,new View(this),1);
    }
    private void routes(){
        Map<String,List<MapMatcher.Rule>> tracks=new LinkedHashMap<>();
        for(MapMatcher.Rule rule:store.matcher.rules)if(store.routes.containsKey(rule.internal)){
            tracks.computeIfAbsent(routeTrack(rule),key->new ArrayList<>()).add(rule);
        }
        body.addView(button("新增自定义地图",this::customMapEditor));
        LinearLayout filtersCard=settingsGroup("地图筛选");
        int mapCount=0,customCount=0;for(List<MapMatcher.Rule> rules:tracks.values())for(MapMatcher.Rule rule:rules){mapCount++;if(store.isCustomMap(rule.internal))customCount++;}
        TextView explanation=label(tracks.size()+" 个赛道 · "+mapCount+" 个地图 · 自定义 "+customCount+" 个\n无路线代码的地图自动左划。",DETAIL);filtersCard.addView(explanation);
        TextView count=label("",DETAIL);
        LinearLayout filters=row();
        List<String> trackNames=new ArrayList<>();trackNames.add("");trackNames.addAll(tracks.keySet());
        if(!trackNames.contains(routeSelectedTrack))routeSelectedTrack="";
        Spinner trackChoice=outlinedSpinner(),mapChoice=outlinedSpinner();
        trackChoice.setContentDescription("选择赛道");mapChoice.setContentDescription("选择地图");
        mapChoice.setDropDownWidth(Math.min(dp(360),getResources().getDisplayMetrics().widthPixels-dp(36)));
        List<String> trackLabels=new ArrayList<>(trackNames);trackLabels.set(0,"全部赛道");
        trackChoice.setAdapter(spinnerOptions(trackLabels));trackChoice.setSelection(trackNames.indexOf(routeSelectedTrack));
        List<String> mapKeys=new ArrayList<>();
        Runnable refreshMaps=()->{
            mapKeys.clear();mapKeys.add("");List<String> mapLabels=new ArrayList<>();mapLabels.add("全部地图");
            for(Map.Entry<String,List<MapMatcher.Rule>> entry:tracks.entrySet()){
                if(!routeSelectedTrack.isEmpty()&&!entry.getKey().equals(routeSelectedTrack))continue;
                for(MapMatcher.Rule rule:entry.getValue()){
                    if(routeOnlyWithoutCode&&!store.routes.get(rule.internal).raw.isEmpty())continue;
                    mapKeys.add(rule.internal);mapLabels.add(routeSelectedTrack.isEmpty()?entry.getKey()+" · "+routeShortName(rule):routeShortName(rule));
                }
            }
            if(!mapKeys.contains(routeSelectedMap))routeSelectedMap="";
            mapChoice.setAdapter(spinnerOptions(mapLabels));mapChoice.setSelection(mapKeys.indexOf(routeSelectedMap));
        };
        refreshMaps.run();
        LinearLayout trackField=column(),mapField=column();TextView trackLabel=heading("赛道",BODY),mapLabel=heading("地图",BODY);
        trackField.addView(trackLabel);trackField.addView(trackChoice);mapField.addView(mapLabel);mapField.addView(mapChoice);
        weighted(filters,trackField,0);weighted(filters,mapField,1);filtersCard.addView(filters);
        Map<String,List<MapMatcher.Rule>> visibleTracks=new LinkedHashMap<>();
        List<java.util.function.Consumer<Boolean>> toggles=new ArrayList<>();
        LinearLayout actions=row();actions.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox onlyWithoutCode=checkBox("只看无路线代码");
        onlyWithoutCode.setChecked(routeOnlyWithoutCode);
        filtersCard.addView(onlyWithoutCode);
        body.addView(filtersCard);

        Button expandAll=compactButton("全部展开",()->{for(java.util.function.Consumer<Boolean> toggle:toggles)toggle.accept(true);});
        Button collapseAll=compactButton("全部折叠",()->{for(java.util.function.Consumer<Boolean> toggle:toggles)toggle.accept(false);});
        weighted(actions,expandAll,0);
        weighted(actions,collapseAll,1);
        body.addView(actions);
        body.addView(count);
        LinearLayout list=column();
        body.addView(list);
        Runnable update=()->{
            list.removeAllViews();visibleTracks.clear();toggles.clear();
            String track=routeSelectedTrack,map=routeSelectedMap;
            boolean filtering=routeOnlyWithoutCode||!track.isEmpty()||!map.isEmpty();int total=0,withoutCode=0;
            for(Map.Entry<String,List<MapMatcher.Rule>> entry:tracks.entrySet()){
                if(!track.isEmpty()&&!entry.getKey().equals(track))continue;
                List<MapMatcher.Rule> matches=new ArrayList<>();
                for(MapMatcher.Rule rule:entry.getValue())if((map.isEmpty()||rule.internal.equals(map))&&(!routeOnlyWithoutCode||store.routes.get(rule.internal).raw.isEmpty()))matches.add(rule);
                if(!matches.isEmpty()){visibleTracks.put(entry.getKey(),matches);total+=matches.size();withoutCode+=routesWithoutCode(matches);}
            }
            count.setText((filtering?visibleTracks.size()+" 个赛道 · "+total+" 个地图 · ":"")+"无路线代码 "+withoutCode+" 个" );
            expandAll.setEnabled(total>0);collapseAll.setEnabled(total>0);
            for(Map.Entry<String,List<MapMatcher.Rule>> entry:visibleTracks.entrySet()){
                String name=entry.getKey();List<MapMatcher.Rule> rules=entry.getValue();int missing=routesWithoutCode(rules);
                LinearLayout maps=column();
                Button heading=button("",()->{});heading.setTextSize(SECTION);heading.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
                heading.setIncludeFontPadding(false);heading.setMinHeight(dp(48));heading.setMinimumHeight(dp(48));heading.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));
                ((LinearLayout.LayoutParams)heading.getLayoutParams()).setMargins(0,0,0,dp(ROW_GAP));
                surface(heading,SOFT,0,10);heading.setTextColor(INK);list.addView(heading);list.addView(maps);
                java.util.function.Consumer<Boolean> toggle=expanded->{
                    if(filtering){if(expanded)collapsedFilteredTracks.remove(name);else collapsedFilteredTracks.add(name);}
                    else {if(expanded)expandedTracks.add(name);else expandedTracks.remove(name);}
                    String title=(expanded?"▾  ":"▸  ")+name;
                    SpannableString headingText=new SpannableString(title+"\n"+rules.size()+" 个地图 · 无路线代码 "+missing+" 个");
                    headingText.setSpan(new android.text.style.RelativeSizeSpan((float)DETAIL/SECTION),title.length()+1,headingText.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    headingText.setSpan(new android.text.style.ForegroundColorSpan(MUTED),title.length()+1,headingText.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    heading.setText(headingText);
                    heading.setContentDescription(name+"，"+rules.size()+" 个地图，无路线代码 "+missing+" 个，"+(expanded?"已展开，点击折叠":"已折叠，点击展开"));
                    if(expanded&&maps.getChildCount()==0)routeCards(maps,rules);
                    maps.setVisibility(expanded?View.VISIBLE:View.GONE);
                };
                heading.setOnClickListener(v->toggle.accept(maps.getVisibility()!=View.VISIBLE));toggles.add(toggle);
                toggle.accept(filtering?!collapsedFilteredTracks.contains(name):expandedTracks.contains(name));
            }
            if(total==0)list.addView(label(routeOnlyWithoutCode?"当前筛选下没有无路线代码的地图":"没有找到匹配的地图",BODY));
        };
        onlyWithoutCode.setOnCheckedChangeListener((view,checked)->{
            routeOnlyWithoutCode=checked;collapsedFilteredTracks.clear();refreshMaps.run();update.run();pageScroll.scrollTo(0,0);
        });
        trackChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                String selected=trackNames.get(position);if(selected.equals(routeSelectedTrack))return;
                routeSelectedTrack=selected;routeSelectedMap="";collapsedFilteredTracks.clear();refreshMaps.run();update.run();pageScroll.scrollTo(0,0);
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
        mapChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                String selected=mapKeys.get(position);if(selected.equals(routeSelectedMap))return;
                routeSelectedMap=selected;collapsedFilteredTracks.clear();update.run();pageScroll.scrollTo(0,0);
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
        update.run();
    }

    private void customMapEditor(){
        LinearLayout box=dialogFields();
        box.addView(label("填写游戏中显示的赛道和地图名称，用于自动识别。路线代码可稍后补充。",DETAIL));
        box.addView(heading("赛道名称",BODY));
        AutoCompleteTextView track=new AutoCompleteTextView(this);typography(track,BODY,false);track.setTextColor(INK);track.setHintTextColor(MUTED);
        track.setPadding(dp(SPACE_LARGE),dp(SPACE_MEDIUM),dp(SPACE_LARGE),dp(SPACE_MEDIUM));track.setBackground(shape(BG,LINE,9));track.setMinHeight(dp(TOUCH_HEIGHT));track.setSingleLine(true);
        track.setHint("选择已有赛道或输入新赛道");track.setContentDescription("赛道名称");track.setThreshold(1);
        Set<String> tracks=new LinkedHashSet<>();for(MapMatcher.Rule rule:store.matcher.rules)tracks.add(routeTrack(rule));
        track.setAdapter(listOptions(new ArrayList<>(tracks)));
        track.setText(routeSelectedTrack);track.setOnClickListener(v->track.showDropDown());box.addView(track,new LinearLayout.LayoutParams(-1,-2));
        box.addView(heading("地图名称",BODY));EditText name=edit("");name.setSingleLine(true);name.setHint("输入游戏中的地图名称");name.setContentDescription("地图名称");box.addView(name);
        box.addView(heading("路线代码（选填）",BODY));EditText raw=edit("");raw.setTypeface(Typeface.MONOSPACE);raw.setMinLines(3);raw.setGravity(Gravity.TOP|Gravity.START);
        raw.setHint("例如：1,31,50,21");raw.setContentDescription("路线代码");box.addView(raw);
        box.addView(label("留空时使用自动左划兜底；填写后会校验路线代码。",DETAIL));
        TextView error=label("",DETAIL);error.setTextColor(0xffb74d49);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);box.addView(error);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("新增自定义地图").setView(scroll(box)).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{
            try{
                String key=store.addCustomMap(track.getText().toString(),name.getText().toString(),raw.getText().toString());
                routeSelectedTrack=store.customMapTrack(key);routeSelectedMap=key;routeOnlyWithoutCode=false;collapsedFilteredTracks.clear();
                dialog.dismiss();render();Toast.makeText(this,"已新增自定义地图",Toast.LENGTH_SHORT).show();
            }catch(Exception e){error.setText(e.getMessage());}
        }));showDialog(dialog);
    }
    private void routeEditor(String key){
        boolean custom=store.isCustomMap(key);
        Route route=store.routes.get(key);LinearLayout box=dialogFields();box.addView(badge(custom?"自定义":"系统内置",custom?ACCENT:MUTED,custom?SOFT:BG));
        box.addView(label(route.events.size()+" 个动作"+(route.valid()?"":"\n"+String.join("\n",route.errors)),DETAIL));
        if(route.raw.isEmpty())box.addView(label("暂未收录这张地图的跑法。多人赛识别到该地图后会自动左划兜底；补充有效跑法并保存后即可使用。",DETAIL));
        EditText raw=edit(route.raw);raw.setTypeface(Typeface.MONOSPACE);raw.setMinLines(5);raw.setGravity(Gravity.TOP|Gravity.START);box.addView(raw);
        box.addView(label("N:a:n:b 氮气；D:t 漂移；21 / 31 等为路线按钮；360 为双击漂移后补氮气。",DETAIL));
        box.addView(label(custom?"路线代码可留空，留空时自动左划兜底。":"恢复默认会舍弃此地图的自定义修改，恢复内置跑法。",DETAIL));
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(store.matcher.displayName(key)).setView(scroll(box)).setNegativeButton("关闭",null).setPositiveButton("保存",null);
        builder.setNeutralButton(custom?"删除地图":"恢复默认",null);
        AlertDialog dialog=builder.create();
        dialog.setOnShowListener(v->{
            dialog.getButton(-1).setOnClickListener(b->{try{store.saveRoute(key,raw.getText().toString());dialog.dismiss();render();}catch(Exception e){message(e.getMessage());}});
            if(custom)dialog.getButton(-3).setOnClickListener(b->confirmDeleteCustomMap(key,dialog));
            if(!custom)dialog.getButton(-3).setOnClickListener(b->{try{store.resetRoute(key);dialog.dismiss();render();routeEditor(key);Toast.makeText(this,"已恢复内置跑法",Toast.LENGTH_SHORT).show();}catch(Exception e){message(e.getMessage());}});
        });showDialog(dialog);if(custom)dialog.getButton(-3).setTextColor(0xffb74d49);
    }
    private void confirmDeleteCustomMap(String key,AlertDialog editor){
        AlertDialog confirmation=new AlertDialog.Builder(this).setTitle("删除自定义地图？")
                .setMessage("将删除“"+store.matcher.displayName(key)+"”及其路线代码，删除后无法恢复。确认删除吗？")
                .setNegativeButton("取消",null).setPositiveButton("确认删除",(dialog,which)->{
                    try{
                        store.deleteCustomMap(key);editor.dismiss();
                        if(key.equals(routeSelectedMap))routeSelectedMap="";
                        render();Toast.makeText(this,"已删除自定义地图",Toast.LENGTH_SHORT).show();
                    }catch(Exception e){message(e.getMessage());}
                }).create();
        showDialog(confirmation);confirmation.getButton(-1).setTextColor(0xffb74d49);
    }
    private void chooseRoute(java.util.function.Consumer<String> chosen){List<String> keys=store.routeKeys();keys.removeIf(key->store.routes.get(key).raw.isEmpty());String[] labels=new String[keys.size()];for(int i=0;i<labels.length;i++)labels[i]=store.matcher.displayName(keys.get(i));showDialog(new AlertDialog.Builder(this).setTitle("选择已收录的跑法").setAdapter(listOptions(Arrays.asList(labels)),(d,w)->chosen.accept(keys.get(w))).setNegativeButton("取消",null).create());}
    private final List<Runnable> permissionRefreshers=new ArrayList<>();
    private LinearLayout settingsGroup(String title){
        LinearLayout group=card();group.setPadding(dp(PAGE_GUTTER),dp(SPACE_MEDIUM),dp(PAGE_GUTTER),dp(SPACE_MEDIUM));
        TextView heading=heading(title,SECTION);heading.setTextColor(ACCENT);group.addView(heading);return group;
    }
    private TextView settingRow(LinearLayout group,String title,String detail,String value,Runnable action){
        if(group.getChildCount()>1){View divider=new View(this);divider.setBackgroundColor(LINE);group.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
        LinearLayout item=row();item.setGravity(Gravity.CENTER_VERTICAL);item.setMinimumHeight(dp(56));item.setPadding(0,dp(SPACE_MEDIUM),0,dp(SPACE_MEDIUM));
        LinearLayout copy=column();TextView name=heading(title,BODY);name.setPadding(0,0,0,0);copy.addView(name);
        if(!detail.isEmpty()){TextView caption=label(detail,DETAIL);caption.setPadding(0,dp(2),dp(SPACE_MEDIUM),0);copy.addView(caption);}
        item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        TextView current=label(value,DETAIL);current.setGravity(Gravity.END);current.setPadding(dp(SPACE_MEDIUM),0,dp(SPACE_MEDIUM),0);current.setMaxWidth(dp(96));item.addView(current);
        TextView arrow=label("›",DIALOG_TITLE);arrow.setTextColor(MUTED);item.addView(arrow);
        surface(item,SURFACE,0,7);item.setFocusable(true);item.setContentDescription(title+"，"+detail+"，"+value);item.setTag(title);
        item.setOnClickListener(v->action.run());group.addView(item);return current;
    }
    private void permissionRow(LinearLayout group,String title,String detail,java.util.function.BooleanSupplier allowed,Runnable action){
        permissionStatusRow(group,title,detail,()->allowed.getAsBoolean()?"已开启":"去开启",action);
    }
    private void permissionStatusRow(LinearLayout group,String title,String detail,java.util.function.Supplier<String> state,Runnable action){
        TextView status=settingRow(group,title,detail,"",action);
        Runnable refresh=()->{String value=state.get();status.setText(value);status.setTextColor(value.equals("已开启")||value.equals("已连接")?ACCENT:0xffa15421);((View)status.getParent()).setContentDescription(title+"，"+detail+"，"+value);};
        permissionRefreshers.add(refresh);refresh.run();
    }
    private String touchPermissionStatus(){
        if(TouchService.instance!=null)return "已连接";
        return TouchService.enabledInSettings(this)?"未连接":"去开启";
    }
    private JSONObject globalConfig(){JSONObject global=store.config.optJSONObject("global");return global==null?new JSONObject():global;}
    private String durationLabel(int seconds){return seconds%60==0?seconds/60+" 分钟":seconds+" 秒";}
    private void settings(){
        LinearLayout permissions=settingsGroup("运行权限");
        permissionStatusRow(permissions,"自动操作","必需 · 允许助手点击和滑动",this::touchPermissionStatus,()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        permissionRow(permissions,"比赛统计悬浮窗","在游戏中显示比赛数据",()->Settings.canDrawOverlays(this),()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        permissionRow(permissions,"通知控制","也可从通知栏控制任务",()->getSystemService(NotificationManager.class).areNotificationsEnabled(),()->startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName())));
        body.addView(permissions);
        LinearLayout diagnostics=settingsGroup("问题排查");
        settingRow(diagnostics,"查看异常截图","查看最近保存的异常画面","",this::showFailureScreenshot);
        body.addView(diagnostics);
        TextView about=label(getString(R.string.app_name)+"  "+version()+" · 本地运行",DETAIL);about.setGravity(Gravity.CENTER);body.addView(about);
    }
    private void showFailureScreenshot() {
        FailureScreenshot.load(getApplicationContext(), (bitmap, savedAt) -> {
            if (isFinishing() || isDestroyed() || tab != SETTINGS) {
                if (bitmap != null) bitmap.recycle();
                return;
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                    .setTitle("异常截图").setPositiveButton("关闭", null);
            if (bitmap == null) {
                showDialog(dialog.setMessage(savedAt == 0 ? "暂无异常截图" : "最近一次异常没有可查看的画面").create());
                return;
            }
            LinearLayout preview = column();
            preview.setPadding(dp(PAGE_GUTTER), dp(ROW_GAP), dp(PAGE_GUTTER), 0);
            String time = java.text.DateFormat.getDateTimeInstance().format(new Date(savedAt));
            preview.addView(label("保存于 " + time, DETAIL));
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setContentDescription("任务异常时的截屏");
            int height = Math.min(dp(480), getResources().getDisplayMetrics().heightPixels / 2);
            preview.addView(image, new LinearLayout.LayoutParams(-1, height));
            AlertDialog screenshot = dialog.setView(preview).create();
            screenshot.setOnDismissListener(ignored -> image.setImageDrawable(null));
            showDialog(screenshot);
        });
    }
    private void taskPreferences(){
        LinearLayout preferences=settingsGroup("运行规则");
        gameSelector(preferences);
        settingRow(preferences,"单局时间上限","超过时限后停止当前任务",durationLabel(store.config.optInt("raceTimeout",900)),()->numberSetting("单局时间上限","raceTimeout",60,1800,"秒",900));
        body.addView(preferences);
        LinearLayout rewards=settingsGroup("奖励领取");
        rewards.addView(label("每轮任务编排开始前检查一次。\n所有启用的任务执行一遍后，下轮再检查。",DETAIL));
        rewardSwitch(rewards,"freePkg","领取免费包");
        rewardSwitch(rewards,"autoToken","领取每日目标奖励");
        body.addView(rewards);
        body.addView(label("更改用于新开始的任务；续跑沿用原任务设置。",DETAIL));
    }
    private LinearLayout dialogFields(){if(preferenceToast!=null)preferenceToast.cancel();LinearLayout fields=column();fields.setPadding(dp(DIALOG_GUTTER),dp(ROW_GAP),dp(DIALOG_GUTTER),dp(ROW_GAP));fields.setFocusableInTouchMode(true);fields.requestFocus();return fields;}
    private void saveSetting(String key,Object value)throws JSONException{
        JSONObject next=new JSONObject(store.config.toString());next.put(key,value);store.config=next;store.save();
        int position=pageScroll.getScrollY();render();pageScroll.post(()->pageScroll.scrollTo(0,position));
        if(preferenceToast!=null)preferenceToast.cancel();preferenceToast=Toast.makeText(this,"已保存",Toast.LENGTH_SHORT);preferenceToast.show();
    }
    private Integer validNumber(EditText input,int min,int max,String unit){
        try{int value=Integer.parseInt(input.getText().toString().trim());if(value>=min&&value<=max){input.setError(null);return value;}}catch(NumberFormatException ignored){}
        input.setError("请输入 "+min+"–"+max+" "+unit);input.requestFocus();return null;
    }
    private EditText numberInput(int value){EditText input=edit(String.valueOf(value));input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);return input;}
    private void numberSetting(String title,String key,int min,int max,String unit,int fallback){
        LinearLayout fields=dialogFields();fields.addView(label("范围："+min+"–"+max+" "+unit,DETAIL));EditText input=numberInput(store.config.optInt(key,fallback));input.setContentDescription(title+"（"+unit+"）");fields.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title+"（"+unit+"）").setView(scroll(fields)).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{Integer value=validNumber(input,min,max,unit);if(value==null)return;try{saveSetting(key,value);dialog.dismiss();}catch(Exception e){input.setError("保存失败，请重试");}}));showDialog(dialog);
    }
    private void gameSelector(LinearLayout group){
        String current=store.config.optString("gamePkg");
        List<InstalledGames.Game> games=InstalledGames.find(getPackageManager(),current);
        List<String> labels=new ArrayList<>();labels.add(games.isEmpty()?"未找到已安装的游戏":"请选择目标游戏");labels.addAll(InstalledGames.labels(games));
        LinearLayout field=column();field.setPadding(0,dp(SPACE_MEDIUM),0,dp(SPACE_MEDIUM));field.addView(heading("目标游戏",BODY));
        Spinner choice=outlinedSpinner();choice.setTag("目标游戏");choice.setContentDescription("选择目标游戏");
        choice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){
            @Override public boolean isEnabled(int position){return position>0;}
            @Override public boolean areAllItemsEnabled(){return false;}
            private View option(int position,boolean dropdown){
                TextView text=label(getItem(position),BODY);text.setTextColor(position==0?MUTED:INK);
                text.setGravity(Gravity.CENTER_VERTICAL);text.setMinHeight(dp(TOUCH_HEIGHT));
                text.setPadding(dropdown?dp(SPACE_LARGE):0,dp(SPACE_SMALL),dropdown?dp(SPACE_LARGE):0,dp(SPACE_SMALL));
                text.setMaxLines(3);text.setEllipsize(TextUtils.TruncateAt.END);return text;
            }
            @Override public View getView(int position,View recycled,ViewGroup parent){return option(position,false);}
            @Override public View getDropDownView(int position,View recycled,ViewGroup parent){return option(position,true);}
        });
        int selected=0;for(int i=0;i<games.size();i++)if(games.get(i).packageName.equals(current))selected=i+1;
        choice.setSelection(selected);choice.setEnabled(!games.isEmpty());field.addView(choice,new LinearLayout.LayoutParams(-1,-2));
        if(games.isEmpty())field.addView(label("请先安装游戏，返回此页面后会自动刷新。",DETAIL));
        else if(selected==0)field.addView(label("原目标游戏不可用，请从已安装的游戏中重新选择。",DETAIL));
        group.addView(field);
        choice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(position<=0)return;String pkg=games.get(position-1).packageName;
                if(pkg.equals(store.config.optString("gamePkg")))return;
                if(getPackageManager().getLaunchIntentForPackage(pkg)==null){render();message("该游戏已不可用，请重新选择。");return;}
                try{saveSetting("gamePkg",pkg);}catch(Exception e){render();message("保存失败，请重试");}
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
    }
    private void rewardSwitch(LinearLayout group,String key,String title){
        if(group.getChildCount()>1){
            View divider=new View(this);
            divider.setBackgroundColor(LINE);
            group.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        Switch toggle=new Switch(this);
        toggle.setText(title);
        typography(toggle,BODY,false);
        toggle.setTextColor(INK);
        toggle.setMinHeight(dp(56));
        toggle.setTag(title);
        toggle.setChecked(globalConfig().optBoolean(key));
        toggle.setOnCheckedChangeListener((view,enabled)->{
            try{
                JSONObject global=new JSONObject(globalConfig().toString());
                global.put(key,enabled);
                store.config.put("global",global);
                store.save();
            }catch(JSONException e){
                render();
                message("保存失败，请重试");
            }
        });
        group.addView(toggle,new LinearLayout.LayoutParams(-1,-2));
    }
    private void requestStart(String route,boolean inspect){
        requestStart(route,inspect,false);
    }
    private void requestStart(String route,boolean inspect,boolean resume){
        if(SessionState.running){message("请先停止当前任务");return;}
        SessionState.inspecting=inspect;
        if(inspect&&!Settings.canDrawOverlays(this)&&!getSystemService(NotificationManager.class).areNotificationsEnabled()){showRunIssue("请先开启悬浮窗或通知，以便在游戏中点击取样");return;}
        if(!inspect&&TouchService.instance==null){showRunIssue(TouchService.unavailableReason(this));return;}
        try{
            if(!inspect&&route.isEmpty()&&!resume){
                boolean enabled=false;JSONArray p=store.config.getJSONArray("projects");for(int i=0;i<p.length();i++)enabled|=p.getJSONObject(i).optBoolean("enabled");if(!enabled)throw new IllegalArgumentException("请至少启用一个项目");
            }
            store.config.put("routeOnly",route);store.save();pendingInspect=inspect;pendingResume=resume;
            MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);Intent consent;
            if(Build.VERSION.SDK_INT>=34)consent=manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay());else consent=manager.createScreenCaptureIntent();
            startActivityForResult(consent,101);
        }catch(Exception e){showRunIssue(e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=101)return;
        if(result!=RESULT_OK||data==null){showRunIssue("未授权截屏，任务没有启动");return;}
        Intent launch=getPackageManager().getLaunchIntentForPackage(store.config.optString("gamePkg"));
        if(launch==null){showRunIssue("未找到游戏，请核对包名和安装状态");return;}
        Intent service=new Intent(this,AutomationService.class).setAction(AutomationService.START).putExtra("result",result).putExtra("consent",data).putExtra("inspect",pendingInspect).putExtra("resume",pendingResume);
        try{
            startForegroundService(service);
            // Match an explicit activity resume, keeping launcher-specific reinitialization out of this transition.
            startActivity(new Intent().setComponent(launch.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch(Exception e){showRunIssue(e.getMessage());}
    }
    private void control(String action){if(!SessionState.running)return;if(action.equals(AutomationService.STOP))SessionState.status("已手动停止");startService(new Intent(this,AutomationService.class).setAction(action));}
    private void message(String text){showDialog(new AlertDialog.Builder(this).setMessage(text==null?"操作失败":text).setPositiveButton("确定",null).create());}
    @Override protected void onResume(){super.onResume();if(store!=null&&body!=null&&tab==PREFERENCES)render();SessionState.listener=this::updateSession;SessionState.changed();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("tab",tab);out.putBoolean("routeOnlyWithoutCode",routeOnlyWithoutCode);out.putBoolean("pendingInspect",pendingInspect);out.putBoolean("pendingResume",pendingResume);out.putString("routeSelectedTrack",routeSelectedTrack);out.putString("routeSelectedMap",routeSelectedMap);out.putStringArrayList("expandedTracks",new ArrayList<>(expandedTracks));out.putStringArrayList("collapsedFilteredTracks",new ArrayList<>(collapsedFilteredTracks));super.onSaveInstanceState(out);}
    @Override protected void onPause(){SessionState.listener=null;super.onPause();}
}
