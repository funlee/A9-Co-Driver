package dev.funlee.a9.core;

/** Fail-closed touch gate for new-car unlock prompts. Coordinates are upright 1280x720. */
public final class VehicleUnlockGuard {
    private boolean active;
    private int safeFrames;
    private int[] later;
    public static String normalize(String text){return MapMatcher.normalize(text).replace("解鎖","解锁").replace("車輛","车辆").replace("新車","新车").replace("稍後","稍后").replace("就緒","就绪");}
    public static boolean prompt(String text){
        String t=normalize(text);
        return t.contains("解锁车辆")||t.contains("解锁新车")||t.contains("解锁就绪")||
                (t.contains("钥匙")&&t.contains("恭喜")&&t.contains("解锁"));
    }
    public static boolean laterLabel(String text,int left,int top,int right,int bottom){
        return normalize(text).equals("稍后查看")&&left>=150&&right<=640&&top>=500&&bottom<=720;
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
            throw new IllegalStateException("新车解锁保护：只允许点击稍后查看，请由玩家手动解锁车辆");
    }
    public synchronized void requireOtherGesture(){if(active)throw new IllegalStateException("新车解锁保护：禁止其他手势和返回键");}
}
