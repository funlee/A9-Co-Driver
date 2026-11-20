package dev.funlee.a9.core;

import java.util.List;
import java.util.Map;

/** Touch gate for vehicle unlock and star-up prompts. Coordinates are upright 1280x720. */
public final class VehicleUnlockGuard {
    private boolean active;
    private int safeFrames;
    private int[] later;
    public static String normalize(String text){return MapMatcher.normalize(text).replace("解鎖","解锁").replace("車輛","车辆").replace("新車","新车").replace("稍後","稍后").replace("就緒","就绪").replace('級','级').replace('試','试').replace('駕','驾').replace('費','费');}
    public static boolean prompt(String text){
        String t=normalize(text);
        // Garage cards also say "升星就绪"; the dialog has its own explanatory text.
        boolean starUp=(t.contains("提升车库")||t.contains("新车辆性能分上限"))&&
                (t.contains("升星就绪")||(t.contains("升星")&&t.contains("恭喜")));
        // A card ribbon or a vehicle detail action may have the same label as a modal.
        if(!t.contains("稍后")&&!t.contains("恭喜")&&!t.contains("解锁新车")&&
                (t.contains("选择参与")||vehicleStats(t)))return false;
        return t.contains("解锁车辆")||t.contains("解锁新车")||t.contains("解锁就绪")||
                (t.contains("钥匙")&&t.contains("恭喜")&&t.contains("解锁"))||starUp;
    }
    public static boolean laterLabel(String text,int left,int top,int right,int bottom){
        return normalize(text).replace('査','查').equals("稍后查看")&&left>=150&&right<=640&&top>=500&&bottom<=720;
    }
    private static boolean vehicleStats(String t){
        return t.contains("操控性")&&(t.contains("加速")||t.contains("氮气"));
    }
    public static boolean lockedDetail(String text){
        String t=normalize(text).replace('圖','图').replace('紙','纸').replace('鎖','锁').replace('獲','获');
        if(vehicleStats(t))for(String line:text.split("\\R"))
            if(normalize(line).equals("解锁"))return true;
        return (t.contains("获取图纸")&&t.contains("图纸以解锁"))||
                (t.contains("性能分")&&((t.contains("取图纸")&&(t.contains("立刻获取")||t.contains("图纸以解锁")))||
                        (t.contains("获取钥匙")&&t.contains("车钥匙以解锁"))))||
                (vehicleStats(t)&&(t.contains("解锁就绪")||t.contains("解锁车辆")||t.contains("立即解锁")||t.contains("已可解锁")));
    }
    /** Require the trial offer and the race action on the same detail frame. */
    public static boolean trialStart(String text,List<?> words){
        if(!normalize(text).contains("免费试驾"))return false;
        for(Object item:words){
            Map<?,?> word=(Map<?,?>)item;
            if(normalize((String)word.get("text")).startsWith("开始")&&
                    (int)word.get("x")>950&&(int)word.get("y")>590)return true;
        }
        return false;
    }
    public static boolean pendingDetail(String text){
        String t=normalize(text);
        if(vehicleStats(t)&&t.contains("提升车辆星级")&&t.contains("解锁更多"))return true;
        if(!t.contains("性能分")||t.contains("车辆选择")||
                !(t.toUpperCase(java.util.Locale.ROOT).contains("TOUCHDRIVE")||
                        vehicleStats(t)))return false;
        if(t.contains("升星就绪")||t.contains("升星后才"))return true;
        for(String line:text.split("\\R"))if(normalize(line).equals("升星"))return true;
        return false;
    }
    public synchronized void observe(boolean prompt,int[] bounds,boolean knownSafeScene){
        if(prompt){active=true;safeFrames=0;}
        else if(active&&knownSafeScene){if(++safeFrames>=2)active=false;}
        else safeFrames=0;
        later=active&&bounds!=null?bounds.clone():null;
    }
    public synchronized boolean active(){return active;}
    public synchronized int[] laterCenter(){return later==null?null:new int[]{(later[0]+later[2])/2,(later[1]+later[3])/2};}
    public synchronized void requireTap(int x,int y){
        if(active&&(later==null||x<later[0]||x>later[2]||y<later[1]||y>later[3]))
            throw new IllegalStateException("车辆提示保护：只允许点击稍后查看");
    }
    public synchronized void requireOtherGesture(){if(active)throw new IllegalStateException("车辆提示保护：禁止其他手势和返回键");}
}
