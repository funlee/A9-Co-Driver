package dev.funlee.a9.core;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.*;

/** Only the personal reward page is rank evidence; the winner animation is not. */
public final class ResultRank {
    public static boolean personalReward(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛');
        return t.contains("比赛奖励")&&t.contains("你的用时")&&!VehicleUnlockGuard.prompt(text);
    }
    public static boolean personalFooter(String text) {
        String t=MapMatcher.normalize(text);
        return t.contains("你的排名")&&t.contains("你的用时")&&!VehicleUnlockGuard.prompt(text);
    }
    public static int fromLines(String context,List<String> lines,boolean footer){
        if(!(footer?personalFooter(context):personalReward(context)))return 0;
        int rank=0;
        for(String line:lines){
            int n=badge(line);
            if(n>0){if(rank>0&&rank!=n)return 0;rank=n;}
        }
        return rank;
    }
    public static int parse(String context,String rankCrop) {
        if(!personalReward(context))return 0;
        return badge(rankCrop);
    }
    private static int badge(String rankCrop){
        String t=Normalizer.normalize(rankCrop,Normalizer.Form.NFKC).replaceAll("\\s+","").replaceAll("[-—:：|]+$","");
        Matcher m=Pattern.compile("第([1-8])(?:名)?").matcher(t);
        // The crop contains only the large personal-rank badge. Reject merged/ambiguous text.
        return m.matches()?Integer.parseInt(m.group(1)):0;
    }
}
