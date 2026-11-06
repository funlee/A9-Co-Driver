package dev.funlee.a9.core;

public final class ColorPattern {
    /** The recovered automation API uses BBGGRR; Android pixels use AARRGGBB. */
    public static int swapRedBlue(int c){return (c&0xff00ff00)|((c&255)<<16)|((c>>>16)&255);}
    public static final class Test {
        private final int desired; private final int[] tolerance=new int[3];
        public Test(String spec,double similarity) {
            String[] v=spec.replace("#", "").replace("0x", "").split("-");
            desired=swapRedBlue((int)Long.parseLong(v[0],16));int base=v.length>1?swapRedBlue(Integer.parseInt(v[1],16)):0;
            int extra=(int)Math.round((1-Math.max(0,Math.min(1,similarity)))*255);
            for(int i=0;i<3;i++)tolerance[i]=Math.min(255,((base>>(i*8))&255)+extra);
        }
        public boolean matches(int actual) {
            for(int i=0;i<3;i++)if(Math.abs(((actual>>(i*8))&255)-((desired>>(i*8))&255))>tolerance[i])return false;
            return true;
        }
    }
    public static boolean color(int actual,String spec,double similarity) {
        return new Test(spec,similarity).matches(actual);
    }
    public static boolean matches(PixelFrame f,String spec,double similarity) {
        for(String item:spec.split(",")) {
            String[] a=item.trim().split("\\|"); if(a.length!=3) throw new IllegalArgumentException("Invalid color point: "+item);
            int x=Integer.parseInt(a[0]), y=Integer.parseInt(a[1]);
            if(!f.inBounds(x,y) || !color(f.pixel(x,y),a[2],similarity)) return false;
        }
        return true;
    }
}
