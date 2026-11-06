package dev.funlee.a9.core;

import java.io.*;
import java.util.*;

/** Matches the recovered, sideways, x-major digit dictionary without online OCR. */
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
        if(!f.inBounds(x1,y1)||!f.inBounds(x2,y2))return new Result(-1,0);
        int left=x2+1,right=-1,top=y2+1,bottom=-1;
        for(int x=x1;x<=x2;x++) for(int y=y1;y<=y2;y++) if(white(f.pixel(x,y))) {
            left=Math.min(left,x); right=Math.max(right,x); top=Math.min(top,y); bottom=Math.max(bottom,y);
        }
        if(right<left || !f.inBounds(x1,y1) || !f.inBounds(x2,y2)) return new Result(-1,0);
        int w=right-left+1,h=bottom-top+1,digit=-1; double bestScore=-1,confidence=0;
        for(Glyph g:templates) {
            if(Math.abs(w-g.w)>(w+g.w)/2.0 || Math.abs(h-g.h)>(h+g.h)/2.0) continue;
            int equal=0;
            for(int x=0;x<Math.min(w,g.w);x++) for(int y=0;y<Math.min(h,g.h);y++) {
                if(white(f.pixel(left+x,top+y))==(g.bits.charAt(x*g.h+y)=='1')) equal++;
            }
            double a=equal/(double)(w*h), b=equal/(double)(g.w*g.h), score=equal+a+b;
            if(a>=threshold && b>=threshold && score>bestScore) { bestScore=score; digit=g.digit; confidence=Math.min(a,b); }
        }
        return new Result(digit,confidence);
    }
    /** The live HUD omits a leading zero below 10%; an unreadable tens glyph is not a blank. */
    public int progress(PixelFrame f,double threshold) {
        if(!f.inBounds(719,1279)||!ColorPattern.matches(f,"680|50|FFFFFF-101010,660|70|FFFFFF-101010",1))return -1;
        Result tens=read(f,634,190,660,208,threshold),ones=read(f,634,209,662,224,threshold);
        if(!ones.valid())return -1;
        if(tens.valid())return tens.digit*10+ones.digit;
        for(int x=634;x<=660;x++)for(int y=190;y<=208;y++)if(white(f.pixel(x,y)))return -1;
        return ones.digit;
    }
    private boolean white(int c) { return (((c>>16)&255)+((c>>8)&255)+(c&255))/3>200; }
}
