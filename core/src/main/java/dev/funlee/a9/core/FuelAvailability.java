package dev.funlee.a9.core;

import java.util.*;

/** Empty-fuel badges in the aligned first six garage slots; never authorizes a tap. */
public final class FuelAvailability {
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
