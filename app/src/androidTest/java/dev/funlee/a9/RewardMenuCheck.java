package dev.funlee.a9;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.*;
import dev.funlee.a9.core.TaskStats;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.LuaRuntime;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Explicit reward-only acceptance: production Lua + real OCR, never loads race workflows. */
final class RewardMenuCheck {
    static void run(Instrumentation test,Bundle report)throws Exception {
        if(SessionState.running)throw new IllegalStateException("Stop the active task before reward-only acceptance");
        UiAutomation ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        LocalStore store=new LocalStore(test.getTargetContext());
        Intent game=test.getTargetContext().getPackageManager().getLaunchIntentForPackage(store.config.getString("gamePkg"));
        test.getTargetContext().startActivity(game.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(2000);
        TextRecognizer reader=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        List<String> logs=new ArrayList<>();int[] frames={0};
        File evidence=new File(test.getTargetContext().getExternalFilesDir(null),"reward-live-"+System.currentTimeMillis());evidence.mkdirs();report.putString("evidence",evidence.getAbsolutePath());
        AtomicBoolean stopped=new AtomicBoolean(),paused=new AtomicBoolean();AtomicReference<String> hud=new AtomicReference<>("正在准备领奖"),shown=new AtomicReference<>("");
        TaskPanelView[] panel={null};Rect[] overlayBounds={null};WindowManager windows=test.getTargetContext().getSystemService(WindowManager.class);Handler main=new Handler(Looper.getMainLooper());
        Runnable tick=new Runnable(){public void run(){if(panel[0]==null)return;String status=paused.get()?"已暂停":hud.get();panel[0].update(new TaskStats(),status,false,"",false);shown.set(status);int[] xy=new int[2];panel[0].getLocationOnScreen(xy);overlayBounds[0]=new Rect(xy[0],xy[1],xy[0]+panel[0].getWidth(),xy[1]+panel[0].getHeight());main.postDelayed(this,500);}};
        test.runOnMainSync(()->{
            android.content.Context context=new ContextThemeWrapper(test.getTargetContext(),R.style.AppTheme);Button pause=new Button(context),stop=new Button(context);pause.setText("暂停");stop.setText("停止");
            pause.setOnClickListener(v->{paused.set(!paused.get());pause.setText(paused.get()?"继续":"暂停");});stop.setOnClickListener(v->stopped.set(true));
            panel[0]=TaskPanelView.create(context,new TextView(context),pause,stop);panel[0].setCompact(true);
            Point size=new Point();windows.getDefaultDisplay().getRealSize(size);WindowManager.LayoutParams params=new WindowManager.LayoutParams();params.type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;params.format=PixelFormat.TRANSLUCENT;params.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;PanelPlacement.apply(params,size,panel[0].panelHeight());windows.addView(panel[0],params);main.post(tick);
        });
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            try {
                if(stopped.get())throw new IllegalStateException("Reward check manually stopped");
                while(paused.get()&&!stopped.get())Thread.sleep(50);
                if(name.equals("_hud")){hud.set(l.toString(1));android.util.Log.i("A9RewardHud",l.toString(1));return 0;}
                if(name.equals("_rewardUi")){
                    String requestedAtCapture=hud.get(),shownAtCapture=shown.get();Bitmap raw=ui.takeScreenshot();if(raw==null)throw new IOException("No screenshot");
                    Bitmap bitmap=raw.copy(Bitmap.Config.ARGB_8888,false);raw.recycle();
                    try {
                        if(bitmap.getWidth()<bitmap.getHeight())throw new IOException("Game is not landscape");
                        Text result=Tasks.await(reader.process(InputImage.fromBitmap(bitmap,0)),12,TimeUnit.SECONDS);
                        android.util.Log.i("A9RewardUi",result.getText());
                        List<Object> words=new ArrayList<>();StringBuilder recognized=new StringBuilder();
                        for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()){
                            Rect b=line.getBoundingBox();if(b!=null&&(overlayBounds[0]==null||!Rect.intersects(overlayBounds[0],b))){words.add(Map.of("text",line.getText(),"x",b.centerX()*1280/bitmap.getWidth(),"y",b.centerY()*720/bitmap.getHeight()));recognized.append(line.getText()).append('\n');}
                        }
                        try(FileOutputStream out=new FileOutputStream(new File(evidence,String.format(Locale.ROOT,"reward-check-%03d.png",++frames[0])))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
                        try(FileWriter notes=new FileWriter(new File(evidence,"frames.txt"),true)){notes.write(frames[0]+" | requested="+requestedAtCapture+" | displayed="+shownAtCapture+" | "+result.getText().replace('\n',' ')+'\n'+"words="+words+'\n');}
                        android.util.Log.i("A9RewardWords",words.toString());
                        LuaRuntime.push(l,Map.of("text",recognized.toString(),"words",words,"frameId",SystemClock.elapsedRealtime()));return 1;
                    }finally{bitmap.recycle();}
                }
                if(name.equals("tap"))shell(ui,"input tap "+l.toInteger(2)+" "+(719-l.toInteger(1)));
                else if(name.equals("swipe"))shell(ui,"input swipe "+l.toInteger(2)+" "+(719-l.toInteger(1))+" "+l.toInteger(4)+" "+(719-l.toInteger(3))+" "+l.toInteger(5));
                else if(name.equals("keyPress")){
                    if(!"back".equals(l.toString(1)))throw new IllegalArgumentException("Unexpected key");
                    shell(ui,"input keyevent KEYCODE_BACK");
                }else if(name.equals("print")){String line=l.toString(1);logs.add(line);android.util.Log.i("A9RewardCheck",line);}
                return 0;
            }catch(Exception e){throw new IllegalStateException(name+": "+e,e);}
        },List.of("_rewardUi","tap","swipe","keyPress","print","_hud","_racePanel"))){
            Map<String,Object> config=store.luaConfig();
            // This check is limited to the two free reward options; never purchases or ads.
            config.put("freePkg","0");config.put("autoToken","0");lua.set("UIconfig",config);
            lua.load("rewards",store.asset("lua/rewards.lua"));lua.load("check","data={};function check()currentRewards.collect()end");
            lua.start("check");long start=SystemClock.elapsedRealtime();boolean alive=true;
            while(alive&&SystemClock.elapsedRealtime()-start<300000){alive=lua.step(SystemClock.elapsedRealtime()-start);Thread.sleep(25);}
            if(alive)throw new AssertionError("Reward check timed out");
            Thread.sleep(700);Bitmap finalFrame=ui.takeScreenshot();if(finalFrame!=null){try(FileOutputStream out=new FileOutputStream(new File(evidence,"final.png"))){finalFrame.compress(Bitmap.CompressFormat.PNG,100,out);}finally{finalFrame.recycle();}}
            for(String line:logs)if(line.contains("暂未领取")||line.contains("未完成确认")||line.contains("结果未确认")||line.contains("尚未确认全部领取"))throw new AssertionError(String.join("\n",logs));
            report.putString("result","PASS: reward-only workflow completed; inspect verified claim logs; no races launched\n"+String.join("\n",logs));
        }finally{reader.close();test.runOnMainSync(()->{main.removeCallbacks(tick);if(panel[0]!=null){windows.removeViewImmediate(panel[0]);panel[0]=null;}});try(FileWriter out=new FileWriter(new File(evidence,"logs.txt"))){out.write(String.join("\n",logs));}}
    }
    private static void shell(UiAutomation ui,String command)throws IOException {
        android.util.Log.i("A9RewardInput",command);
        try(ParcelFileDescriptor fd=ui.executeShellCommand(command);FileInputStream input=new FileInputStream(fd.getFileDescriptor())){
            byte[] bytes=new byte[1024];while(input.read(bytes)!=-1){}
        }
    }
}
