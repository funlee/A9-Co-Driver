package dev.funlee.a9.core;

import java.util.*;
import java.util.regex.*;

/** Evidence from the current CN garage. A completed swipe alone is never a list origin. */
public final class MultiplayerGarage {
    private static final java.util.regex.Pattern SOLO_SCORE=java.util.regex.Pattern.compile("(?<![0-9/])([0-9]{3,4})(?![0-9])");
    private static final Pattern PAIR=Pattern.compile("(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)");
    private static final List<String> LEAGUES=List.of("青铜","白银","黄金","白金","翡翠","钻石","精英","宗师","传奇");
    private static final int[] BADGE_COLORS={0xd88560,0x96b2d4,0xf0ca30,0xb18eff,0x65c6a5,0x21a9ff,0xc0569f,0xc85059,0xf5e2a4};

    public static String leagueBadge(String text) {
        String normalized=leagueText(text);
        String found="";
        for(String league:LEAGUES)if(normalized.contains(league)){
            if(!found.isEmpty())return "";
            found=league;
        }
        return found;
    }

    /** The league shield is centered above its series title on the same entry card. */
    public static String worldEntryLeague(List<?> words) {
        Map<?,?> title=null;
        for(Object item:words){
            Map<?,?> w=(Map<?,?>)item;
            if(leagueText(w.get("text").toString()).equals("世界系列赛")){
                if(title!=null)return "";
                title=w;
            }
        }
        if(title==null)return "";
        String found="";
        for(Object item:words){
            Map<?,?> w=(Map<?,?>)item;
            int dy=pos(title,"y")-pos(w,"y");
            if(Math.abs(pos(title,"x")-pos(w,"x"))>65||dy<145||dy>270)continue;
            String label=leagueText(w.get("text").toString());
            if(label.endsWith("联赛"))label=label.substring(0,label.length()-2);
            if(!LEAGUES.contains(label))continue;
            if(!found.isEmpty()&&!found.equals(label))return "";
            found=label;
        }
        return found;
    }

    private static String leagueText(String text) {
        return MapMatcher.normalize(text).replace('銅','铜').replace('銀','银')
                .replace('鑽','钻').replace('傳','传').replace('黃','黄').replace('鉑','铂')
                .replace('聯','联').replace('賽','赛').replace('師','师')
                .replace("铂金","白金");
    }

    /** Read the saturated center stripe of the event's small rating emblem, not the garage tabs. */
    public static String eventLeagueIcon(PixelFrame frame) {
        return eventEmblem(frame,77,115,307);
    }

    /** Locate the shield beside the rating number across different event layouts. */
    public static String eventLeagueIcon(PixelFrame frame,List<?> words,boolean wide) {
        int y=eventRatingRow(words);
        String badge=y<0?"":eventEmblem(frame,wide?69:75,wide?117:116,y);
        return badge.isEmpty()&&!wide?eventLeagueIcon(frame):badge;
    }

    /** Upright bounds of the badge itself, excluding the rating digits. */
    public static int[] eventLeagueBadgeBounds(List<?> words,boolean wide) {
        int y=eventRatingRow(words);
        return new int[]{wide?66:72,y<0?(wide?265:283):y-21,120,y<0?(wide?409:347):y+38};
    }

    public static boolean hasEventRating(List<?> words){return eventRatingRow(words)>=0;}

    private static int eventRatingRow(List<?> words) {
        int row=-1;
        for(Object item:words){
            Map<?,?> word=(Map<?,?>)item;
            int x=pos(word,"x"),y=pos(word,"y");
            String label=leagueText(word.get("text").toString());
            if(x<123||x>265||y<267||y>390||!label.matches("[0-9]{3,5}"))continue;
            if(row>=0&&Math.abs(row-y)>8)return -1;
            row=y;
        }
        return row;
    }

    private static String eventEmblem(PixelFrame frame,int left,int right,int ratingY) {
        String badge=leagueStripe(frame,left,ratingY-1,right,ratingY+13);
        if(!badge.isEmpty()&&!badge.equals("青铜"))return badge;
        // The gold shield has an orange gradient through the lettering. Its
        // lower stripe stays gold even when the main crop looks bronze.
        for(int offset=11;offset<=16;offset++){
            int y=ratingY+offset;
            if(leagueStripe(frame,left,y,right,y+6).equals("黄金"))return "黄金";
        }
        if(badge.isEmpty()&&silverEmblem(frame,left,right,ratingY))return "白银";
        return badge;
    }

    // Silver has a low-saturation face between a pale blue crest and a darker blue base.
    // Checking all three regions keeps a grey panel or a solid diamond stripe from matching.
    private static boolean silverEmblem(PixelFrame frame,int left,int right,int ratingY) {
        int crest=0,face=0,base=0;
        int inset=(right-left)/8,width=right-left-2*inset;
        for(int dy=-8;dy<25;dy++)for(int x=left+inset;x<right-inset;x++){
            int rgb=frame.pixel(719-ratingY-dy,x);
            int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
            if(g<r||b<g)continue;
            if(dy< -2&&b>=165&&g>=115&&b-r>=55)crest++;
            if(dy>=0&&dy<13&&r>=68&&r<=225&&b-r>=3&&b-r<=52)face++;
            if(dy>=16&&b>=100&&b-r>=55&&b-g>=28)base++;
        }
        return crest*4>=width*6&&face*3>=width*13*2&&base*4>=width*9;
    }

    private static String leagueStripe(PixelFrame frame,int left,int top,int right,int bottom) {
        int[] votes=new int[LEAGUES.size()];
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++){
            int pixel=frame.pixel(719-y,x);
            int r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
            int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),chroma=max-min;
            if(max<100||chroma<64||chroma*100<38*max)continue;
            double hue=(max==r?(double)(g-b)/chroma:max==g?2.0+(double)(b-r)/chroma:4.0+(double)(r-g)/chroma);
            int band=Math.floorMod((int)Math.round(hue*4),24);
            int league=eventLeagueStripe(band);
            if(league>=0)votes[league]++;
        }
        int best=-1,first=0,second=0;
        for(int i=0;i<votes.length;i++){
            if(votes[i]>first){second=first;first=votes[i];best=i;}
            else second=Math.max(second,votes[i]);
        }
        return first*10>=(right-left)*(bottom-top)*3&&first>=second*2?LEAGUES.get(best):"";
    }

    private static int eventLeagueStripe(int hue) {
        if(hue==0||hue==23)return 7; // Grandmaster red
        if(hue<=2)return 0;
        if(hue<=4)return 2;
        if(hue>=9&&hue<=11)return 4;
        if(hue>=12&&hue<=15)return 5;
        if(hue>=17&&hue<=20)return 3;
        if(hue==21||hue==22)return 6;
        return -1;
    }

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
    public static boolean validPerformance(String text){
        String value=pair(text);
        if(value.isEmpty())return false;
        String[] parts=value.split("/");
        try{
            int current=Integer.parseInt(parts[0]),maximum=Integer.parseInt(parts[1]);
            return current>=100&&maximum>=current;
        }catch(NumberFormatException ignored){return false;}
    }
    private static boolean edge(PixelFrame f,int x,int top){return edge(f,x,top,false);}
    private static boolean edge(PixelFrame f,int x,int top,boolean scrolled){
        int hits=0;
        for(int y=top+12;y<top+205;y++){
            int c=f.pixel(719-y,x),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            if((scrolled&&r<35&&g>=40&&b>=80&&b>g+25)||((g>140&&b>110&&r<100)||(g>100&&b>180&&r<30))||(r>150&&g>180&&b<160)||(r>180&&g<100&&b>50))hits++;
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
        String league=league(f,words,rank,wide,layout18By9);
        int left=-1;
        int top=wide?215:layout18By9?206:195;
        int rowPitch=wide?231:layout18By9?233:237;
        int columnPitch=wide?359:layout18By9?405:464;
        int cardWidth=wide?348:layout18By9?393:450;
        int nameStart=wide?235:layout18By9?258:290;
        int centerOffset=cardCenterOffset(aspect);
        for(int x=wide?215:202;x<=(wide?233:220);x++)if(edge(f,x,top)&&edge(f,x,top+rowPitch)){left=x;break;}
        int originLeft=left;
        List<Integer> candidates=new ArrayList<>();
        if(left>=0)candidates.add(left);
        for(int x=4;x<=650;x++)if(x!=originLeft&&edge(f,x,top,true)&&edge(f,x,top+rowPitch,true))candidates.add(x);
        for(int candidate:candidates){
        left=candidate;
        List<Object> cards=new ArrayList<>();
        if(left>=0)for(int i=1;i<=6;i++){
            int x=left+((i-1)/2)*columnPitch,y=top+((i-1)%2)*rowPitch;
            // A clipped card cannot supply identity and does not authorize a tap.
            if(x+cardWidth>(wide?1260:1280))continue;
            String performance="";int fuel=-1,capacity=0;Set<String> tokens=new TreeSet<>();
            for(Object o:words){Map<?,?> w=(Map<?,?>)o;int wx=pos(w,"x"),wy=pos(w,"y");String t=w.get("text").toString();
                if(wx>x&&wx<x+150&&wy>y&&wy<y+70){String p=pair(t);if(!p.isEmpty())performance=p;}
                if(wx>x&&wx<x+130&&wy>y+170&&wy<y+225){
                    String p=pair(t);if(!p.isEmpty()){String[] a=p.split("/");try{int n=Integer.parseInt(a[0]),d=Integer.parseInt(a[1]);if(d>=1&&d<=9&&n>=0&&n<=d){fuel=n;capacity=d;}}catch(NumberFormatException ignored){}}
                }
                if(wx>x+nameStart&&wx<x+cardWidth&&wy>y+90&&wy<y+185){
                    for(String token:t.toUpperCase(Locale.ROOT).split("[^A-Z0-9]+"))if((token.length()>=4&&token.matches(".*[A-Z].*"))||token.matches("[A-Z]{1,3}[0-9]{1,3}"))tokens.add(token);
                    String model=compact(t);
                    if((layout18By9||wide)&&model.length()>=6&&model.matches("(?=.*[A-Z])(?=.*[0-9]).*"))tokens.add(model);
                }
            }
            if(!wide&&!layout18By9&&FuelAvailability.emptySlots(f).contains(i)){fuel=0;capacity=6;}
            if(!performance.isEmpty()&&!tokens.isEmpty())cards.add(Map.of("index",i,"x",x+centerOffset,"y",y+110,"performance",performance,"tokens",new ArrayList<>(tokens),"fuel",fuel,"capacity",capacity));
        }
        Map<?,?> first=null;for(Object c:cards)if(((Number)((Map<?,?>)c).get("index")).intValue()==1)first=(Map<?,?>)c;
        if(first==null)continue;
        boolean start=!league.isEmpty()&&left==originLeft;
        return Map.of("left",left,"start",start,"rank",league,"key",start?league+":"+first.get("performance"):"","cards",cards,"pitch",columnPitch);
        }
        return Map.of("left",-1,"start",false,"rank",league,"key","","cards",List.of(),"pitch",columnPitch);
    }
    private static String league(PixelFrame f,List<?> words,String rank,boolean wide,boolean layout18By9){
        StringBuilder rail=new StringBuilder(rank);
        for(Object o:words){Map<?,?> w=(Map<?,?>)o;if(pos(w,"x")>=90&&pos(w,"x")<=180&&pos(w,"y")>=350&&pos(w,"y")<=480)rail.append(w.get("text"));}
        // Observed OCR substitution on the small bronze shield, only inside the origin rail.
        rank=rail.toString().replace("青洞","青铜").replace("青銅","青铜").replace("吉铜","青铜").replace("铂金","白金");
        String league="";for(String s:LEAGUES)if(rank.contains(s))league=s;
        if((wide||layout18By9)&&league.isEmpty()&&bronzeShield(f,layout18By9))league="青铜";
        return league;
    }

    /** Number physical row pairs, including unavailable cars. No labels are needed to locate a slot. */
    public static Map<String,Object> slots(PixelFrame frame,List<?> words,String rank,double aspect){
        return slots(frame,words,rank,aspect,false);
    }
    public static Map<String,Object> slots(PixelFrame frame,List<?> words,String rank,double aspect,boolean master){
        boolean wide=aspect>2.07,medium=aspect>=2.0&&!wide;
        int top=wide?215:medium?206:195,rowPitch=wide?231:medium?233:237;
        int pitch=wide?359:medium?405:464,width=wide?348:medium?393:450;
        String league=master?(masterRail(frame,words)?"大师系列赛":""):league(frame,words,rank,wide,medium);
        int left=-1;
        for(int x=wide?18:4;x<650;x++){
            if(slotEdge(frame,x,top)&&slotEdge(frame,x,top+rowPitch)){left=x;break;}
        }
        boolean originRegion=left>=(wide?215:202)&&left<=(wide?233:220);
        // The rail emblem is separate from the highlighted shortcut. Read it
        // only when the first card's geometry confirms the rail is present.
        if(!master&&!wide&&!medium&&originRegion&&league.isEmpty()){
            league=leagueStripe(frame,109,409,175,433);
            // The Gold shield blends into orange near the label. Confirm its lower
            // stripe against the highlighted shortcut before trusting the first column.
            if(league.isEmpty()&&leagueStripe(frame,109,430,175,442).equals("黄金")&&
                    "黄金".equals(leagueTabs(frame).get("selected")))league="黄金";
        }
        boolean origin=originRegion&&!league.isEmpty();
        List<Object> cards=new ArrayList<>();
        for(int column=0;left>=0&&column<3;column++){
            int x=left+column*pitch;
            int right=wide?1260:1280;
            if(x+190>right)break;
            boolean clipped=x+width>right;
            for(int row=0;row<2;row++){
                int y=top+row*rowPitch;
                boolean bordered=false;
                for(int dx=-3;dx<=3&&!bordered;dx++)bordered=slotEdge(frame,x+dx,y);
                if(!bordered)continue;
                cards.add(Map.of("index",2*column+row+1,"x",x+cardCenterOffset(aspect),"y",y+110,
                        "signature",thumbnail(frame,x,y),"clipped",clipped,
                        "availability",clipped?"unknown":availability(frame,x,y,words)));
            }
        }
        return Map.of("left",left,"start",origin,"rank",league,"key",origin?league:"","cards",cards,"pitch",pitch);
    }

    /** Solo-event garages use wider cards and have no league rail. */
    public static Map<String,Object> soloSlots(PixelFrame frame,List<?> words,double aspect){
        boolean wide=aspect>2.07,medium=aspect>=2.0&&!wide;
        int top=wide?216:medium?206:195,rowPitch=wide?239:medium?241:244;
        int pitch=wide?371:medium?424:474,width=pitch-10;
        int left=-1;
        for(int x=18;x<674;x++){
            if(slotEdge(frame,x,top,true)&&slotEdge(frame,x,top+rowPitch,true)){left=x;break;}
        }
        if(left<0){
            for(int x=87;x<=132;x++)if(slotEdge(frame,x,top,true)){left=x;break;}
        }
        List<Object> cards=new ArrayList<>();
        int right=wide?1260:1280;
        for(int column=0,x=left;left>=0&&x+190<=right;column++,x+=pitch){
            for(int row=0;row<2;row++){
                int y=top+row*rowPitch;
                int edge=-1;
                for(int dx=-4;dx<=4&&edge<0;dx++)if(slotEdge(frame,x+dx,y,true))edge=x+dx;
                if(edge<0)continue;
                boolean clipped=edge+width>right;
                cards.add(Map.of("index",2*column+row+1,"x",edge+(wide?145:190),"y",y+113,"left",edge,"top",y,
                        "signature",thumbnail(frame,edge,y),"clipped",clipped,
                        "availability",clipped?"unknown":soloAvailability(frame,edge,y,words),
                        "score",scoreIn(words,edge+12,edge+169,y+27,y+78),
                        "model",modelIn(words,edge+width*2/3,edge+width-5,y+132,y+180)));
            }
        }
        boolean leadingClear=left>=87&&soloOriginGutter(frame,left,top,rowPitch);
        return Map.of("left",left,"start",leadingClear&&left<=132,"leadingClear",leadingClear,
                "cards",cards,"pitch",pitch);
    }

    /** The observed empty solo list keeps its heading over an uninterrupted blue background. */
    public static boolean soloEmpty(PixelFrame frame,List<?> words,double aspect){
        return soloEmpty(frame,words,aspect,false);
    }

    /** Focused heading OCR can recover text hidden or omitted in the full-screen read. */
    public static boolean soloEmpty(PixelFrame frame,List<?> words,double aspect,boolean focusedHeading){
        boolean heading=false,instruction=false;
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            int x=pos(word,"x"),y=pos(word,"y");
            String label=MapMatcher.normalize(word.get("text").toString()).replace('擇','择').replace('賽','赛');
            if(x>=70&&x<=460&&y>=75&&y<=125&&(label.equals("车辆选择")||label.equals("车辅选择")))heading=true;
            if(x>=70&&x<=620&&y>=120&&y<=165&&label.contains("选择参与此赛事的车辆"))instruction=true;
            if(x>=90&&x<=1240&&y>=180&&y<=610)return false;
        }
        if((!focusedHeading&&(!heading||!instruction))||!RaceHud.backWedge(frame)||
                !((List<?>)soloSlots(frame,words,aspect).get("cards")).isEmpty())return false;
        int background=0,total=0;
        for(int y=185;y<=610;y+=5)for(int x=90;x<=1240;x+=5){
            int color=frame.pixel(719-y,x),r=color>>16&255,g=color>>8&255,b=color&255;
            if(r<=12&&g>=8&&g<=45&&b>=25&&b<=85&&b-g>=14)background++;
            total++;
        }
        return background*1000>=total*995;
    }

    private static String soloAvailability(PixelFrame frame,int left,int top,List<?> words){
        String ribbon=availabilityRibbon(frame,left,top,words);
        if(!ribbon.isEmpty())return ribbon;
        // Solo cards carry the multiplayer fuel pill at the lower-left: bright
        // yellow-green while fuel remains, pink once the tank is empty.
        int green=0,pink=0,samples=0;
        for(int y=top+198;y<=top+236;y+=2)for(int x=left+8;x<=left+95;x+=2){
            int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
            if(r>=150&&g>=200&&b<=90&&g>r+30)green++;
            if(r>=195&&g<=75&&b>=50&&b<=185)pink++;
            samples++;
        }
        if(pink>=samples/3)return "empty";
        if(green>=samples/3){
            for(Object word:words){
                Map<?,?> w=(Map<?,?>)word;
                int x=pos(w,"x")-left,y=pos(w,"y")-top;
                if(x>0&&x<95&&y>=190&&y<=240&&pair(w.get("text").toString()).matches("0/[1-9]"))return "empty";
            }
            return "ready";
        }
        // Locked cards have the same dark background under both the fuel area
        // and the artwork. Unlocked cards keep their colored background.
        return darkCardPatch(frame,left+12,left+69,top+186,top+215)&&
                darkCardPatch(frame,left+96,left+199,top+190,top+215)?"locked":"unknown";
    }

    private static boolean darkCardPatch(PixelFrame frame,int left,int right,int top,int bottom){
        int dark=0,total=0;
        for(int y=top;y<=bottom;y+=3)for(int x=left;x<=right;x+=3){
            int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
            if(r<45&&g<55&&b<80)dark++;
            total++;
        }
        return dark*10>=total*9;
    }

    private static boolean soloOriginGutter(PixelFrame frame,int left,int top,int rowPitch){
        int filled=0;
        for(int row=0;row<2;row++)for(int dy:new int[]{2,62,110,164,205})
            for(int x=45;x<left-8;x+=8){
                int color=frame.pixel(719-top-row*rowPitch-dy,x);
                if((color>>16&255)>46||(color>>8&255)>78||(color&255)>113)filled++;
            }
        return filled<=3;
    }

    public static String soloDetailScore(List<?> words){
        return scoreIn(words,800,1170,78,178);
    }

    public static String soloDetailName(List<?> words){
        List<String> parts=new ArrayList<>();
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            int x=pos(word,"x"),y=pos(word,"y");
            String text=word.get("text").toString().trim();
            if(x>=150&&x<=550&&y>=80&&y<=192&&text.matches("[A-Za-z0-9 .+'’\\-]+"))parts.add(text);
        }
        return String.join(" ",parts);
    }

    private static String modelIn(List<?> words,int left,int right,int top,int bottom){
        String model="";
        int lowest=top;
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            int x=pos(word,"x"),y=pos(word,"y");
            String raw=word.get("text").toString();
            String compact=compact(raw);
            if(x>=left&&x<=right&&y>=lowest&&y<=bottom&&compact.length()>=3&&raw.matches("[A-Za-z0-9 .+'’\\-]+")){
                model=compact;
                lowest=y;
            }
        }
        return model;
    }

    private static String scoreIn(List<?> words,int left,int right,int top,int bottom){
        String score="";
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            int x=pos(word,"x"),y=pos(word,"y");
            if(x<left||x>right||y<top||y>bottom)continue;
            String number=soloScore(word.get("text").toString());
            if(number.isEmpty())continue;
            if(!score.isEmpty()&&!score.equals(number))return "";
            score=number;
        }
        return score;
    }
    public static String soloScore(String text){
        java.util.regex.Matcher number=SOLO_SCORE.matcher(text.replaceAll("[,，\\s]",""));
        return number.find()?number.group(1):"";
    }

    private static boolean masterRail(PixelFrame frame,List<?> words){
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            if(pos(word,"x")>=95&&pos(word,"x")<=185&&pos(word,"y")>=370&&pos(word,"y")<=480&&
                    MapMatcher.normalize(word.get("text").toString()).equals("小白"))return true;
        }
        int gold=0;
        for(int y=365;y<495;y+=2)for(int x=100;x<190;x+=2){
            int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
            if(r>=165&&g>=110&&g<=225&&r-g>=10&&g-b>=22)gold++;
        }
        return gold>=105;
    }

    private static boolean borderColor(int c){
        if(blueGutter(c))return false;
        int r=(c>>16)&255,g=(c>>8)&255,b=c&255;
        // Maxed cards have a pale gold outline; their highlights can be brighter than yellow ribbons.
        boolean paleGold=r>=185&&b>=170&&r>=g-8&&r<=g+45&&g>=b+12&&g<=b+76;
        return (r<60&&b>=80&&b>g+24)||(g>140&&b>105&&r<105)||(r>145&&g>155&&b<170)||(r>=95&&g<85&&b>40&&r>g+65)
                ||paleGold||(Math.min(r,Math.min(g,b))>=180&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=40);
    }
    private static boolean blueGutter(int color){
        int r=color>>16&255,g=color>>8&255,b=color&255;
        return r<=48&&g<=65&&b<=95&&r<g&&g<b;
    }
    private static boolean slotEdge(PixelFrame frame,int x,int top){
        return slotEdge(frame,x,top,false);
    }
    private static boolean slotBorder(int color,boolean solo){
        int r=color>>16&255,g=color>>8&255,b=color&255;
        return borderColor(color)||solo&&r>=100&&g<=80&&b<=80&&r>=g+65&&r>=b+65;
    }
    private static boolean slotEdge(PixelFrame frame,int x,int top,boolean solo){
        // The horizontal line must begin here. A right border or a clipped card has no such corner.
        boolean corner=false;
        for(int y=top-4;y<=top+5&&!corner;y++)
            corner=slotBorder(frame.pixel(719-y,x+2),solo)&&slotBorder(frame.pixel(719-y,x+5),solo)
                    &&slotBorder(frame.pixel(719-y,x+10),solo)&&slotBorder(frame.pixel(719-y,x+76),solo)
                    &&!slotBorder(frame.pixel(719-y,x-5),solo);
        if(!corner)return false;
        int hits=0;
        for(int y=top+14;y<top+204;y+=4){
            int gap=frame.pixel(719-y,x-5);
            boolean gutter=(gap>>16&255)<24&&(gap>>8&255)<50&&(gap&255)<80;
            if((gutter||blueGutter(gap))&&slotBorder(frame.pixel(719-y,x),solo))hits++;
        }
        return hits>=36;
    }

    private static String availabilityRibbon(PixelFrame frame,int left,int top,List<?> words){
        int starBanner=0;
        for(int y=top+5;y<=top+22;y+=3)for(int x=left+12;x<=left+154;x+=7){
            int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
            // The solid yellow Star Up ribbon is distinct from the darker gold maxed-card artwork.
            if(Math.abs(r-243)<=18&&Math.abs(g-187)<=15&&b<=38)starBanner++;
        }
        String state=starBanner>=70?"starUp":"";
        for(Object word:words){
            Map<?,?> w=(Map<?,?>)word;
            int x=pos(w,"x")-left,y=pos(w,"y")-top;
            if(x<0||x>235||y<0||y>205)continue;
            String text=VehicleUnlockGuard.normalize(w.get("text").toString());
            if(text.contains("免费试驾"))return "trial";
            if(text.contains("解锁就绪")||text.contains("已可解锁")||text.contains("未解锁"))state="locked";
            if(y<=35&&text.contains("升星就绪"))state="starUp";
        }
        return state;
    }

    private static String availability(PixelFrame frame,int left,int top,List<?> words){
        String ribbon=availabilityRibbon(frame,left,top,words);
        if(!ribbon.isEmpty())return ribbon;
        int green=0,pink=0,dark=0,samples=0;
        for(int y=top+191;y<=top+207;y+=2)for(int x=left+12;x<=left+46;x+=2){
            int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
            if(g>=155&&r>=90&&g>r+15&&b<95)green++;
            if(r>=195&&g<=75&&b>=50&&b<=185)pink++;
            if(r<38&&g<53&&b<75)dark++;
            samples++;
        }
        if(pink>=samples/3)return "empty";
        if(green>=samples/3){
            for(Object word:words){
                Map<?,?> w=(Map<?,?>)word;
                int x=pos(w,"x")-left,y=pos(w,"y")-top;
                if(x>0&&x<100&&y>=174&&y<=223&&pair(w.get("text").toString()).matches("0/[1-9]"))return "empty";
            }
            return "ready";
        }
        return dark>=samples*95/100?"locked":"unknown";
    }

    // Local contrasts on the car artwork allow consecutive pages to be aligned without reading text.
    private static String thumbnail(PixelFrame frame,int left,int top){
        StringBuilder bits=new StringBuilder(80);
        for(int y=top+80;y<=top+164;y+=12){
            for(int x=left+22;x<=left+166;x+=16){
                int a=luma(frame,x,y),b=luma(frame,x+10,y);
                bits.append(a>b+7?'1':'0');
            }
        }
        return bits.toString();
    }
    private static int luma(PixelFrame frame,int x,int y){
        int total=0;
        for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++){
            int c=frame.pixel(719-y-dy,x+dx);
            total+=((c>>16&255)*3+(c>>8&255)*6+(c&255))/10;
        }
        return total/49;
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
