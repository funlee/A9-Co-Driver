package dev.funlee.a9.core;

import java.util.*;
import java.util.regex.*;

/** Evidence from the current CN garage. A completed swipe alone is never a list origin. */
public final class MultiplayerGarage {
    private static final Pattern PAIR=Pattern.compile("(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)");
    private static final List<String> LEAGUES=List.of("青铜","白银","黄金","白金","翡翠","钻石","精英","宗师","传奇");
    private static final int[] BADGE_COLORS={0xd88560,0x96b2d4,0xf0ca30,0xb18eff,0x65c6a5,0x21a9ff,0xc0569f,0xc85059,0xf5e2a4};

    /** Locate league shortcuts by their shield fill. A pink underline marks the selected shortcut. */
    public static Map<String,Object> leagueTabs(PixelFrame frame){
        Map<String,Object> tabs=new HashMap<>();
        String selected="";
        for(int i=0;i<LEAGUES.size();i++){
            int rgb=BADGE_COLORS[i],sumX=0,sumY=0,count=0;
            for(int y=86;y<=174;y+=2)for(int x=546+52*i;x<=579+55*i;x+=2){
                int c=frame.pixel(719-y,x);
                if(Math.abs(((c>>16)&255)-(rgb>>16))>16 || Math.abs(((c>>8)&255)-((rgb>>8)&255))>16 ||
                        Math.abs((c&255)-(rgb&255))>16)continue;
                sumX+=x;sumY+=y;count++;
            }
            if(count<8)continue;
            int x=sumX/count,y=sumY/count;
            String league=LEAGUES.get(i);
            tabs.put(league,Map.of("x",x,"y",y));
            for(int row=y+15;row<=y+40;row++){
                boolean line=true;
                for(int dx=-12;dx<=12;dx+=6){
                    int c=frame.pixel(719-row,x+dx);
                    if(((c>>16)&255)<210 || ((c>>8)&255)>55 || (c&255)<45){line=false;break;}
                }
                if(line){selected=selected.isEmpty()?league:"?";break;}
            }
        }
        if(tabs.size()<3)return Map.of();
        if(LEAGUES.contains(selected))tabs.put("selected",selected);
        return tabs;
    }

    public static String compact(String s){return s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");}
    private static int pos(Map<?,?> w,String key){return ((Number)w.get(key)).intValue();}
    private static String pair(String s){Matcher m=PAIR.matcher(s);return m.find()?m.group(1).replace(",","")+"/"+m.group(2).replace(",",""):"";}
    private static boolean edge(PixelFrame f,int x,int top){
        int hits=0;
        for(int y=top+12;y<top+205;y++){
            int c=f.pixel(719-y,x),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            if(((g>140&&b>110&&r<100)||(g>100&&b>180&&r<30))||(r>150&&g>180&&b<160)||(r>180&&g<100&&b>50))hits++;
        }
        return hits>=150;
    }
    public static Map<String,Object> read(PixelFrame f,List<?> words,String rank){
        return read(f,words,rank,false);
    }
    public static Map<String,Object> read(PixelFrame f,List<?> words,String rank,boolean wide){
        return read(f,words,rank,wide?20.0/9:16.0/9);
    }
    public static int cardCenterOffset(double aspect){
        return aspect>2.07?150:aspect>=2.0?175:200;
    }
    public static Map<String,Object> read(PixelFrame f,List<?> words,String rank,double aspect){
        boolean wide=aspect>2.07,layout18By9=aspect>=2.0&&!wide;
        StringBuilder rail=new StringBuilder(rank);
        for(Object o:words){Map<?,?> w=(Map<?,?>)o;if(pos(w,"x")>=90&&pos(w,"x")<=180&&pos(w,"y")>=350&&pos(w,"y")<=480)rail.append(w.get("text"));}
        // Observed OCR substitution on the small bronze shield, only inside the origin rail.
        rank=rail.toString().replace("青洞","青铜").replace("青銅","青铜").replace("吉铜","青铜").replace("铂金","白金");
        String league="";for(String s:LEAGUES)if(rank.contains(s))league=s;
        if((wide||layout18By9)&&league.isEmpty()&&bronzeShield(f,layout18By9))league="青铜";
        int left=-1;
        int top=wide?215:layout18By9?206:195;
        int rowPitch=wide?231:layout18By9?233:237;
        int columnPitch=wide?359:layout18By9?405:464;
        int cardWidth=wide?348:layout18By9?393:450;
        int nameStart=wide?235:layout18By9?258:290;
        int centerOffset=cardCenterOffset(aspect);
        for(int x=wide?215:202;x<=(wide?233:220);x++)if(edge(f,x,top)&&edge(f,x,top+rowPitch)){left=x;break;}
        List<Object> cards=new ArrayList<>();
        if(left>=0)for(int i=1;i<=6;i++){
            int x=left+((i-1)/2)*columnPitch,y=top+((i-1)%2)*rowPitch;
            // A clipped card cannot supply identity and does not authorize a tap.
            if(x+cardWidth>(wide?1260:1280))continue;
            String performance="";int fuel=-1,capacity=0;Set<String> tokens=new TreeSet<>();
            for(Object o:words){Map<?,?> w=(Map<?,?>)o;int wx=pos(w,"x"),wy=pos(w,"y");String t=w.get("text").toString();
                if(wx>x&&wx<x+150&&wy>y&&wy<y+70)performance=pair(t);
                if(wx>x&&wx<x+130&&wy>y+170&&wy<y+225){
                    String p=pair(t);if(!p.isEmpty()){String[] a=p.split("/");try{int n=Integer.parseInt(a[0]),d=Integer.parseInt(a[1]);if(d>=1&&d<=9&&n>=0&&n<=d){fuel=n;capacity=d;}}catch(NumberFormatException ignored){}}
                }
                if(wx>x+nameStart&&wx<x+cardWidth&&wy>y+90&&wy<y+185){
                    for(String token:t.toUpperCase(Locale.ROOT).split("[^A-Z0-9]+"))if(token.length()>=4&&token.matches(".*[A-Z].*"))tokens.add(token);
                    String model=compact(t);
                    if(layout18By9&&model.length()>=6&&model.matches("(?=.*[A-Z])(?=.*[0-9]).*"))tokens.add(model);
                }
            }
            if(!wide&&!layout18By9&&FuelAvailability.emptySlots(f).contains(i)){fuel=0;capacity=6;}
            if(!performance.isEmpty()&&!tokens.isEmpty())cards.add(Map.of("index",i,"x",x+centerOffset,"y",y+110,"performance",performance,"tokens",new ArrayList<>(tokens),"fuel",fuel,"capacity",capacity));
        }
        Map<?,?> first=null;for(Object c:cards)if(((Number)((Map<?,?>)c).get("index")).intValue()==1)first=(Map<?,?>)c;
        boolean start=!league.isEmpty()&&left>=0&&first!=null;
        return Map.of("left",left,"start",start,"rank",league,"key",start?league+":"+first.get("performance"):"","cards",cards);
    }
    private static boolean bronzeShield(PixelFrame frame,boolean layout18By9){
        int bronze=0;
        int left=layout18By9?114:128,right=layout18By9?160:182;
        int top=layout18By9?420:425,bottom=layout18By9?440:443;
        for(int y=top;y<=bottom;y+=3)for(int x=left;x<=right;x+=3){
            int c=frame.pixel(719-y,x),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            if(r>=165&&g>=40&&g<=195&&b<95&&r>g+30)bronze++;
        }
        return bronze>=(layout18By9?58:65);
    }
    public static Map<String,Object> detail(List<?> words){
        StringBuilder name=new StringBuilder();Set<String> performance=new HashSet<>();
        for(Object o:words){Map<?,?> w=(Map<?,?>)o;int x=pos(w,"x"),y=pos(w,"y");String t=w.get("text").toString();
            if(x<700&&y>60&&y<220)name.append(t).append(' ');
            if(x>750&&y>60&&y<230){String p=pair(t);if(!p.isEmpty())performance.add(p);}
        }
        return Map.of("name",compact(name.toString()),"performance",performance.size()==1?performance.iterator().next():"");
    }
}
