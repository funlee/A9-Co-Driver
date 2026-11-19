package dev.funlee.a9;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.*;

/** Launchable A9 installations; no broad package-list permission is needed. */
final class InstalledGames {
    static final class Game {
        final String packageName,name;
        Game(String packageName,String name){this.packageName=packageName;this.name=name;}
    }
    static List<Game> find(PackageManager manager,String current) {
        List<Game> apps=new ArrayList<>();
        Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo info:manager.queryIntentActivities(launcher,0)) {
            if(info.activityInfo==null||!info.activityInfo.enabled||!info.activityInfo.applicationInfo.enabled)continue;
            String pkg=info.activityInfo.packageName;
            if(manager.getLaunchIntentForPackage(pkg)==null)continue;
            apps.add(new Game(pkg,manager.getApplicationLabel(info.activityInfo.applicationInfo).toString()));
        }
        return select(apps,current);
    }
    static List<Game> select(List<Game> apps,String current) {
        Map<String,Game> unique=new LinkedHashMap<>();
        for(Game app:apps) {
            String name=app.name.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{P}]","");
            String pkg=app.packageName.toLowerCase(Locale.ROOT);
            if(app.packageName.equals(current)||pkg.startsWith("com.aligames.kuang.kybc.")
                    ||name.startsWith("狂野飙车9")||name.startsWith("狂野飙车竞速传奇")
                    ||name.startsWith("狂野飙车传奇")||name.startsWith("asphalt9")||name.startsWith("asphaltlegends"))
                unique.putIfAbsent(app.packageName,app);
        }
        List<Game> games=new ArrayList<>(unique.values());
        games.sort(Comparator.comparing((Game game)->game.name).thenComparing(game->game.packageName));
        return games;
    }
    static List<String> labels(List<Game> games) {
        Map<String,Integer> counts=new HashMap<>();for(Game game:games)counts.merge(game.name,1,Integer::sum);
        List<String> labels=new ArrayList<>();
        for(Game game:games)labels.add(counts.get(game.name)>1?game.name+"\n"+game.packageName:game.name);
        return labels;
    }
}
