package dev.funlee.a9.core;

/** Maps the automation's 720 × 1280 frame to the captured game window on the display. */
public final class GameViewport {
    public final int left, top, width, height, rotation;

    public GameViewport(int left, int top, int width, int height, int rotation) {
        this.left=left;this.top=top;this.width=width;this.height=height;
        this.rotation=Math.floorMod(rotation,360);
    }

    public static boolean supports(int width,int height,int rotation) {
        if(width<=0||height<=0||rotation%90!=0)return false;
        double ratio=rotation%180==0?width/(double)height:height/(double)width;
        // Keep the existing narrow-screen tolerance and accept landscape displays up to 2.5:1.
        return ratio>=.4&&ratio<=720.0/1280+.08;
    }

    public boolean isFullHeightWindow(int displayWidth,int displayHeight) {
        return left>=0&&top>=0&&left+width<=displayWidth&&top+height<=displayHeight&&
                width>=displayWidth*.7&&height>=displayHeight*.9&&supports(width,height,rotation);
    }

    public float[] toScreen(int x,int y) {
        if(x<0||y<0||x>=720||y>=1280)throw new IllegalArgumentException("触控坐标超出画面："+x+","+y);
        float fx=x/719f,fy=y/1279f;
        switch(rotation){
            case 90: return screenPoint(fy,1-fx);
            case 180: return screenPoint(1-fx,1-fy);
            case 270: return screenPoint(1-fy,fx);
            default: return screenPoint(fx,fy);
        }
    }

    private float[] screenPoint(float x,float y) {
        return new float[]{left+x*(width-1),top+y*(height-1)};
    }

    /** Converts display coordinates to the upright 1280 × 720 OCR frame. */
    public float[] toUpright(float x,float y) {
        float fx=(x-left)/Math.max(1,width-1),fy=(y-top)/Math.max(1,height-1);
        switch(rotation){
            case 90: return new float[]{fx*1279,fy*719};
            case 180: return new float[]{(1-fy)*1279,fx*719};
            case 270: return new float[]{(1-fx)*1279,(1-fy)*719};
            default: return new float[]{fy*1279,(1-fx)*719};
        }
    }
}
