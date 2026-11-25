package dev.funlee.a9.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recognizes a home menu even when OCR reads the two rows of tab labels separately. */
public final class MainMenuUi {
    private MainMenuUi() {}

    static boolean exitDialog(String text) {
        String label=MapMatcher.normalize(text);
        return label.contains("确定退出游戏")||
                (label.contains("退出游戏")&&label.contains("再玩一会")&&label.contains("领取游戏福利"));
    }

    public static String classify(String text,List<?> words) {
        if(exitDialog(text))return "exitGame";
        Set<String> tabs=new HashSet<>();
        for(Object line:words){
            Map<?,?> word=(Map<?,?>)line;
            int y=((Number)word.get("y")).intValue();
            if(y<585)continue;
            int x=((Number)word.get("x")).intValue();
            String label=MapMatcher.normalize(word.get("text").toString()).replace('賽','赛');
            if(x>=420&&x<650&&(label.equals("每日")||label.equals("每日赛事")))tabs.add("daily");
            if(x>=650&&x<850&&(label.equals("多人")||label.equals("游戏")||label.equals("多人游戏")))tabs.add("multiplayer");
            if(x>=840&&x<1100&&(label.equals("俱乐部")||label.equals("我的俱乐部")))tabs.add("club");
            if(x>=1050&&(label.equals("单人")||label.equals("模式")||label.equals("单人模式")))tabs.add("single");
        }
        return tabs.size()>=3?"home":"";
    }

    /** backToMain color signatures in the automation's rotated BGR coordinates. */
    public static String returnOverlay(PixelFrame frame) {
        if (ColorPattern.matches(frame, "493|416|FF7856-101010,493|425|FF7956-101010,506|441|FFA157-101010", 1)) return "back";
        if (ColorPattern.matches(frame, "70|920|10FBC3-101010,70|970|10FBC3-101010", 1)) return "continue";
        if (ColorPattern.matches(frame, "705|1251|191919-050505,690|1251|060302-050505,666|1226|060302-050505", 1)
                || ColorPattern.matches(frame, "698|1249|181818-050505,676|1250|060302-050505,679|1238|151413-050505", 1)) return "ad";
        if (ColorPattern.matches(frame, "286|397|424242-090909,441|394|444241-090909,398|821|424242-090909,401|983|424242-090909", 1)) return "gray";
        return "";
    }

    /** Each bit is a distinct way of confirming an unobstructed home menu in this frame. */
    public static int homeEvidence(PixelFrame frame, String text, List<?> words, String scene, boolean obstructed) {
        if (obstructed || !(scene.equals("home") || scene.equals("unknown") || scene.equals("series"))
                || exitDialog(text) || RaceHud.backWedge(frame)) return 0;
        int evidence = ColorPattern.matches(frame, "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) ? 1 : 0;
        if (classify(text, words).equals("home")) evidence |= 2;
        return evidence;
    }

    /** The daily-goals clipboard uses a yellow tile when a reward is available. */
    public static boolean dailyGoalsHighlighted(PixelFrame frame) {
        if(frame.width!=720||frame.height!=1280)return false;
        int yellow=0,samples=0;
        // Sample the clipboard tile interior, away from adjacent reward icons and small badges.
        for(int x=424;x<466;x+=2)for(int y=10;y<60;y+=2){
            int color=frame.pixel(719-y,x);
            int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
            if(r>=215&&g>=160&&b<=105)yellow++;
            samples++;
        }
        return yellow*4>=samples;
    }

    /** The Daily home hero has a single wide lime action above the bottom tabs. */
    public static Map<String,Integer> dailyExplore(String text,PixelFrame frame) {
        String normalized=MapMatcher.normalize(text).replace('戰','战').replace('時','时');
        if(!normalized.contains("挑战限时"))return Map.of();
        Map<String,Integer> button=Map.of();
        int top=-1,left=1280,right=-1;
        for(int y=385;y<=583;y+=3){
            int rowLeft=1280,rowRight=-1,count=0;
            if(y<583)for(int x=710;x<1265;x++){
                int color=frame.pixel(719-y,x);
                int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
                if(r>=140&&r<=225&&g>=215&&b<=55){
                    rowLeft=Math.min(rowLeft,x);rowRight=x;count++;
                }
            }
            // The dark button label can cover nearly a third of a row.
            if(rowRight-rowLeft>=175&&count*3>=2*(rowRight-rowLeft)){
                if(top<0)top=y;
                left=Math.min(left,rowLeft);right=Math.max(right,rowRight);
            }else if(top>=0){
                int height=y-top;
                if(height>=36&&height<=115){
                    if(!button.isEmpty())return Map.of();
                    button=Map.of("x",(left+right)/2,"y",top+height/2);
                }
                top=-1;left=1280;right=-1;
            }
        }
        return button;
    }
}
