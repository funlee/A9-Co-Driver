package dev.funlee.a9;

import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Read-only regression over retained device frames, using the production OCR and garage detector. */
public final class GarageScreenshotCheck extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args){arguments=args;super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle report=new Bundle();TextRecognizer ocr=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            if(arguments.containsKey("cupListFrame")){
                File file=new File(getTargetContext().getFilesDir(),"runtime/"+arguments.getString("cupListFrame"));
                Bitmap image=BitmapFactory.decodeFile(file.getPath());
                if(image==null)throw new AssertionError("Missing event-list frame");
                if(image.getWidth()==1280&&image.getHeight()==720){
                    Matrix rotation=new Matrix();rotation.postRotate(90);
                    Bitmap side=Bitmap.createBitmap(image,0,0,1280,720,rotation,false);image.recycle();image=side;
                }
                if(image.getWidth()!=720||image.getHeight()!=1280)throw new AssertionError("Unsupported event-list frame");
                int[] pixels=new int[720*1280];image.getPixels(pixels,0,720,0,0,720,1280);image.recycle();
                PixelFrame frame=new PixelFrame(720,1280,pixels);
                report.putString("cards",CupCarousel.cards(frame).toString());
                report.putBoolean("claimSpotlight",CupCarousel.claimSpotlight(frame));
                if(!CupCarousel.listVisible(frame)&&!CupCarousel.claimSpotlight(frame))throw new AssertionError("Standard event list not confirmed; Back="+RaceHud.backWedge(frame));
                report.putString("result","PASS: retained standard event list recognized without wide viewport or input");
                finish(0,report);return;
            }
            if(arguments.containsKey("emptyGarageFrame")){
                File file=new File(getTargetContext().getFilesDir(),"runtime/"+arguments.getString("emptyGarageFrame"));
                Bitmap image=BitmapFactory.decodeFile(file.getPath());
                if(image==null)throw new AssertionError("Missing empty garage frame");
                if(image.getWidth()==1280&&image.getHeight()==720){
                    Matrix rotation=new Matrix();rotation.postRotate(90);
                    Bitmap side=Bitmap.createBitmap(image,0,0,1280,720,rotation,false);image.recycle();image=side;
                }
                if(image.getWidth()!=720||image.getHeight()!=1280)throw new AssertionError("Unsupported empty garage frame");
                int[] pixels=new int[720*1280];image.getPixels(pixels,0,720,0,0,720,1280);
                Matrix rotation=new Matrix();rotation.postRotate(270);
                Bitmap upright=Bitmap.createBitmap(image,0,0,720,1280,rotation,false);
                Text text=Tasks.await(ocr.process(InputImage.fromBitmap(upright,0)),15,TimeUnit.SECONDS);
                List<Object> words=new ArrayList<>();
                for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
                    Rect b=line.getBoundingBox();if(b!=null)words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
                }
                PixelFrame frame=new PixelFrame(720,1280,pixels);
                report.putString("ocr",text.getText());
                if(ModalPopup.locate(frame)!=null||Boolean.TRUE.equals(GarageFilter.read(frame,16.0/9).get("present"))||
                        !DailyEventUi.classifySolo(text.getText()).equals("cars")||
                        !GarageOcr.soloEmpty(ocr,frame,words,16.0/9))throw new AssertionError("Empty garage not confirmed: "+words);
                if(!GarageOcr.soloEmpty(ocr,frame,List.of(),16.0/9))throw new AssertionError("Focused OCR did not recover the empty heading");
                var garage=MultiplayerGarage.soloSlots(frame,words,16.0/9);
                try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                    LocalStore store=new LocalStore(getTargetContext());lua.set("garage",garage);
                    lua.load("utilities",store.asset("lua/utilities.lua"));
                    lua.load("grid",store.asset("lua/multiplayer_current.lua"));
                    lua.load("replay","data={['项目']='杯赛',['用车次数']={}};run={dailyLoot=true,data={['选车筛选']='0,1,2'}};local frame=0;"+
                            "utils.configureGarageFilter=function()return true end;function print()end;"+
                            "function _soloGarageUi(fast)assert(not fast);frame=frame+1;return {scene='unknown',focusedGarage=true,frameId=frame,garageEmpty=true,garage=garage}end;"+
                            "function test()assert(utils.chooseCarCup({1})=='garageEmpty' and not next(data['用车次数']));done=true end");
                    lua.start("test");for(int i=0;i<40&&lua.step(i*1000L);i++){};lua.load("result","assert(done)");
                }
                upright.recycle();image.recycle();
                report.putString("result","PASS: retained empty garage recognized and skipped without any touch or car-use count");
                finish(0,report);return;
            }
            if(arguments.containsKey("dailyHeadingFrame")){
                File file=new File(getTargetContext().getFilesDir(),"runtime/"+arguments.getString("dailyHeadingFrame"));
                Bitmap image=BitmapFactory.decodeFile(file.getPath());
                if(image==null||image.getWidth()!=720||image.getHeight()!=1280)throw new AssertionError("Missing normalized garage frame");
                int[] pixels=new int[720*1280];image.getPixels(pixels,0,720,0,0,720,1280);image.recycle();
                PixelFrame frame=new PixelFrame(720,1280,pixels);
                var grid=MultiplayerGarage.soloSlots(frame,List.of(),16.0/9);
                if(!RaceHud.backWedge(frame)||ModalPopup.locate(frame)!=null||!GarageOcr.soloHeading(ocr,frame)
                        ||((List<?>)grid.get("cards")).isEmpty())throw new AssertionError("Focused garage evidence missing");
                report.putString("result","PASS: focused ML Kit heading and garage grid on supplied frame");
                finish(0,report);return;
            }
            boolean solo=arguments.containsKey("soloPendingIndex");
            Map<String,Object> garage=null,detail=null;
            for(String scene:List.of("cars","car")){
                String name=scene.equals("cars")?arguments.getString("carsFrame","multiplayer-cars.png"):"multiplayer-car.png";
                Bitmap side;
                if(solo&&scene.equals("car")){
                    List<FailureScreenshot.Entry> entries=FailureScreenshot.readHistory(getTargetContext());
                    side=entries.isEmpty()?null:FailureScreenshot.readImage(entries.get(0));
                }else side=BitmapFactory.decodeFile(new File(getTargetContext().getFilesDir(),"runtime/"+name).getPath());
                if(side==null)throw new AssertionError("Missing retained "+scene+" frame");
                if(side.getWidth()==1280){Matrix m=new Matrix();m.postRotate(90);Bitmap normalized=Bitmap.createBitmap(side,0,0,side.getWidth(),side.getHeight(),m,false);side.recycle();side=normalized;}
                Matrix rotation=new Matrix();rotation.postRotate(270);Bitmap image=Bitmap.createBitmap(side,0,0,720,1280,rotation,false);
                Text text=Tasks.await(ocr.process(InputImage.fromBitmap(image,0)),15,TimeUnit.SECONDS);
                List<Object> words=new ArrayList<>();
                for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
                    Rect b=line.getBoundingBox();if(b!=null)words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
                }
                if(solo){
                    if(scene.equals("cars")){
                        int[] pixels=new int[720*1280];side.getPixels(pixels,0,720,0,0,720,1280);
                        garage=MultiplayerGarage.soloSlots(new PixelFrame(720,1280,pixels),words,16.0/9);
                        if(!DailyEventUi.classifySolo(text.getText()).equals("cars"))throw new AssertionError("Not a vehicle list");
                    }else if(!DailyEventUi.classifySolo(text.getText()).equals("car")||
                            !VehicleUnlockGuard.pendingDetail(text.getText())||VehicleUnlockGuard.trialStart(text.getText(),words)){
                        throw new AssertionError("Pending detail not recognized: "+text.getText());
                    }
                }else if(scene.equals("cars")){
                    Bitmap crop=Bitmap.createBitmap(side,250,90,86,111);Bitmap rot=Bitmap.createBitmap(crop,0,0,86,111,rotation,true);Bitmap scaled=Bitmap.createScaledBitmap(rot,rot.getWidth()*3,rot.getHeight()*3,true);
                    String rank=Tasks.await(ocr.process(InputImage.fromBitmap(scaled,0)),15,TimeUnit.SECONDS).getText();
                    int[] pixels=new int[720*1280];side.getPixels(pixels,0,720,0,0,720,1280);
                    // Reproduce the observed full-frame OCR omission; production crop must recover it.
                    words.removeIf(value->{Map<?,?> w=(Map<?,?>)value;int x=((Number)w.get("x")).intValue(),y=((Number)w.get("y")).intValue();return x>210&&x<340&&y>365&&y<420;});
                    garage=GarageOcr.read(ocr,new PixelFrame(720,1280,pixels),words,rank,false);
                    report.putString("garage",new JSONObject(garage).toString());report.putString("rank",rank);
                    scaled.recycle();rot.recycle();crop.recycle();
                    if(!Boolean.TRUE.equals(garage.get("start")))throw new AssertionError("Retained first-page screenshot was not confirmed: "+garage+" OCR="+words);
                }else{detail=MultiplayerGarage.detail(words);report.putString("detail",new JSONObject(detail).toString());}
                image.recycle();side.recycle();
            }
            if(solo){
                int index=Integer.parseInt(arguments.getString("soloPendingIndex"));
                if(!Boolean.TRUE.equals(garage.get("start")))throw new AssertionError("Expected the first garage page");
                try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                    LocalStore store=new LocalStore(getTargetContext());
                    lua.load("utilities",store.asset("lua/utilities.lua"));
                    lua.load("grid",store.asset("lua/multiplayer_current.lua"));
                    lua.set("garage",garage);lua.set("index",index);
                    lua.load("replay","data={['项目']='杯赛',['用车次数']={}};run={dailyLoot=true,data={}};local frame=0;"+
                            "function _soloGarageUi(fast)frame=frame+1;return {scene='cars',frameId=frame,gridOnly=fast,garage=garage}end;"+
                            "function test()assert(utils.chooseCarCup({index})=='fuelEmpty');done=true end");
                    lua.start("test");for(int i=0;i<100&&lua.step(i*1000L);i++){}
                    lua.load("result","assert(done)");
                }
                report.putString("result","PASS: production selection skipped pending car "+index+" without any touch; OCR recognized the star-up detail");
                finish(-1,report);return;
            }
            Map<?,?> first=(Map<?,?>)((List<?>)garage.get("cards")).get(0);
            if(((Number)first.get("fuel")).intValue()<=0)throw new AssertionError("Retained first car fuel not recognized: "+first);
            if(!first.get("performance").equals(detail.get("performance")))throw new AssertionError("Performance mismatch: "+first+" vs "+detail);
            boolean match=false;for(Object token:(List<?>)first.get("tokens"))if(detail.get("name").toString().contains(token.toString()))match=true;
            if(!match)throw new AssertionError("Vehicle model mismatch");
            report.putString("result","PASS: origin, first card and selected vehicle agree; no input dispatched");finish(0,report);
        }catch(Throwable e){report.putString("result","FAIL: "+e);finish(1,report);}finally{ocr.close();}
    }
}
