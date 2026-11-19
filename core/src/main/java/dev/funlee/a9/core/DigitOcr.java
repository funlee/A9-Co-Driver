package dev.funlee.a9.core;

import java.io.*;
import java.util.*;

/** Recognizes digits locally using sideways templates stored in x-major order. */
public final class DigitOcr {
    public static final class Glyph {
        public final int digit,w,h; public final String bits;
        Glyph(int digit,int w,int h,String bits) { this.digit=digit; this.w=w; this.h=h; this.bits=bits; }
    }
    public static final class Result {
        public final int digit; public final double confidence;
        Result(int d,double c) { digit=d; confidence=c; }
        public boolean valid() { return digit>=0; }
    }
    public final List<Glyph> templates=new ArrayList<>();
    public DigitOcr(Reader input) throws IOException {
        BufferedReader r=new BufferedReader(input); String line;
        while((line=r.readLine())!=null) {
            if(line.trim().isEmpty()) continue;
            String[] a=line.split("\\|"); String[] d=a[1].split(",");
            Glyph g=new Glyph(Integer.parseInt(a[0]),Integer.parseInt(d[0]),Integer.parseInt(d[1]),a[2].trim());
            if(g.bits.length()!=g.w*g.h || !g.bits.matches("[01]+")) throw new IOException("Invalid digit template");
            templates.add(g);
        }
    }
    public Result read(PixelFrame f,int x1,int y1,int x2,int y2,double threshold) {
        return read(f,x1,y1,x2,y2,threshold,1.0);
    }
    private Result read(PixelFrame f,int x1,int y1,int x2,int y2,double threshold,double widthScale) {
        if(!f.inBounds(x1,y1)||!f.inBounds(x2,y2))return new Result(-1,0);
        int left=x2+1,right=-1,top=y2+1,bottom=-1;
        for(int x=x1;x<=x2;x++) for(int y=y1;y<=y2;y++) if(white(f.pixel(x,y))) {
            left=Math.min(left,x); right=Math.max(right,x); top=Math.min(top,y); bottom=Math.max(bottom,y);
        }
        if(right<left || !f.inBounds(x1,y1) || !f.inBounds(x2,y2)) return new Result(-1,0);
        int sourceWidth=right-left+1;
        int w=Math.max(1,(int)Math.round(sourceWidth*widthScale)),h=bottom-top+1,digit=-1;
        double bestScore=-1,confidence=0;
        for(Glyph g:templates) {
            if(Math.abs(w-g.w)>(w+g.w)/2.0 || Math.abs(h-g.h)>(h+g.h)/2.0) continue;
            int equal=0;
            for(int x=0;x<Math.min(w,g.w);x++) for(int y=0;y<Math.min(h,g.h);y++) {
                int sample=left+(int)((x+.5)*sourceWidth/w);
                if(white(f.pixel(sample,top+y))==(g.bits.charAt(x*g.h+y)=='1')) equal++;
            }
            double a=equal/(double)(w*h), b=equal/(double)(g.w*g.h), score=equal+a+b;
            if(a>=threshold && b>=threshold && score>bestScore) { bestScore=score; digit=g.digit; confidence=Math.min(a,b); }
        }
        return new Result(digit,confidence);
    }
    /** The live HUD omits a leading zero below 10%; an unreadable tens glyph is not a blank. */
    public int progress(PixelFrame f,double threshold) {
        return progress(f,threshold,false);
    }
    public int progress(PixelFrame f,double threshold,boolean wide) {
        return progress(f,threshold,wide?20.0/9:16.0/9);
    }
    public int progress(PixelFrame f,double threshold,double aspect) {
        boolean wide=aspect>=2.0;
        if(!f.inBounds(719,1279)||!RaceHud.visible(f,wide))return -1;
        boolean shifted=RaceHud.widePause(f,wide);
        int left=634,right=660,tensStart=190,onesStart=209,onesEnd=224;
        double widthScale=1.0;
        if(shifted&&aspect<=2.07){
            // 18:9 moves the HUD inward, with the percent sign starting at x=244.
            left=631;right=655;tensStart=208;onesStart=228;onesEnd=243;widthScale=.9;
        }else if(shifted){
            left=622;right=647;tensStart=214;onesStart=230;onesEnd=242;widthScale=18.0/22;
        }
        Result tens=read(f,left,tensStart,right,onesStart-1,threshold,widthScale);
        Result ones=read(f,left,onesStart,shifted?right:662,onesEnd,threshold,widthScale);
        if(!ones.valid())return -1;
        if(tens.valid())return tens.digit*10+ones.digit;
        for(int x=left;x<=right;x++)for(int y=tensStart;y<onesStart;y++)if(white(f.pixel(x,y)))return -1;
        return ones.digit;
    }
    private boolean white(int c) { return (((c>>16)&255)+((c>>8)&255)+(c&255))/3>200; }
}
