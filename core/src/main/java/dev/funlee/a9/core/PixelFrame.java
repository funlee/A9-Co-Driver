package dev.funlee.a9.core;

public final class PixelFrame {
    public final int width,height;
    private final int[] pixels;
    public PixelFrame(int w,int h,int[] data) {
        if(w<=0 || h<=0 || data.length!=w*h) throw new IllegalArgumentException("Image dimensions");
        width=w; height=h; pixels=data.clone();
    }
    public int pixel(int x,int y) { if(x<0||y<0||x>=width||y>=height) return 0; return pixels[y*width+x]; }
    public boolean inBounds(int x,int y) { return x>=0&&y>=0&&x<width&&y<height; }
    public int[] copyPixels() { return pixels.clone(); }
}
