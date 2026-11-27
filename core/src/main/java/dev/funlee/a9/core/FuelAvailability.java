package dev.funlee.a9.core;

import java.util.*;
import java.util.regex.Pattern;

/** Empty-fuel badges in the aligned first six garage slots; never authorizes a tap. */
public final class FuelAvailability {
    // Either "10" may be split by OCR. Keep spaces between separate counters
    // intact: in "1/1 0/3", the zero belongs to the second counter.
    private static final Pattern DETAIL_COUNTER=Pattern.compile(
            "(?<![0-9])(1\\s*0|[0-9])\\s*[/／]\\s*(1\\s*0(?!\\s*[/／])|[1-9])(?![0-9])");

    /** Upright vehicle-detail fuel counter, excluding tickets, blueprints and performance. */
    public static int detail(List<?> words) {
        Set<Integer> values=new HashSet<>();
        for(Object o:words){
            Map<?,?> w=(Map<?,?>)o;
            int x=((Number)w.get("x")).intValue(),y=((Number)w.get("y")).intValue();
            if(x<470||x>710||y<640||y>705)continue;
            addCounters(w.get("text").toString(),values);
        }
        return values.size()==1?values.iterator().next():-1;
    }
    /** Read an enlarged crop containing only the lower fuel counter. */
    public static int focusedDetail(String text){
        Set<Integer> values=new HashSet<>();
        addCounters(text,values);
        return values.size()==1?values.iterator().next():-1;
    }
    private static void addCounters(String text,Set<Integer> values){
        var m=DETAIL_COUNTER.matcher(text);
        while(m.find()){
            int fuel=Integer.parseInt(m.group(1).replaceAll("\\s",""));
            int capacity=Integer.parseInt(m.group(2).replaceAll("\\s",""));
            if(fuel<=capacity)values.add(fuel);
        }
    }
    public static List<Integer> emptySlots(PixelFrame f){
        List<Integer> slots=new ArrayList<>();if(f.width!=720||f.height!=1280)return slots;
        for(int i=1;i<=6;i++){
            int left=219+((i-1)/2)*464,top=384+((i-1)%2)*237,pink=0,white=0;
            for(int x=left;x<left+46;x++)for(int y=top;y<top+26;y++){
                int c=f.pixel(719-y,x),r=(c>>16)&255,g=(c>>8)&255,b=c&255;
                if(r>=220&&g<=65&&b>=65&&b<=190)pink++;
                if(r>=225&&g>=225&&b>=225)white++;
            }
            if(pink>=400&&white>=25)slots.add(i);
        }
        return slots;
    }
}
