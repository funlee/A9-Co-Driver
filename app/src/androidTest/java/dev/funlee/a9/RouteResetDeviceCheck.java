package dev.funlee.a9;

import android.app.Instrumentation;
import android.content.*;
import android.os.Bundle;
import dev.funlee.a9.core.Route;
import java.util.*;
import org.json.JSONObject;

/** Uses isolated preferences with the actual bundled routes; never changes user routes. */
final class RouteResetDeviceCheck {
    static void run(Instrumentation test,Bundle report)throws Exception {
        Context base=test.getTargetContext();String preferences="route-reset-check-"+UUID.randomUUID();
        Context isolated=new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(preferences,mode);}
        };
        try {
            LocalStore store=new LocalStore(isolated);
            Route valid=null,empty=null,invalid=null;
            for(Route route:store.routes.values()){
                if(route.valid()&&valid==null)valid=route;
                if(route.raw.isEmpty()&&empty==null)empty=route;
                if(!route.raw.isEmpty()&&!route.valid()&&invalid==null)invalid=route;
            }
            if(valid==null||empty==null||invalid==null)throw new AssertionError("Missing route fixtures");
            String unrelated=null;
            // Pick a separate route that is not among the three reset cases.
            for(String key:store.routes.keySet())if(!key.equals(valid.name)&&!key.equals(empty.name)&&!key.equals(invalid.name)){unrelated=key;break;}
            if(unrelated==null)throw new AssertionError("Missing unrelated route fixture");
            store.saveRoute(unrelated,"1,31");
            for(Route original:List.of(valid,empty,invalid)){
                store.saveRoute(original.name,"2,21");
                store=new LocalStore(isolated);
                if(!store.routes.get(original.name).raw.equals("2,21"))throw new AssertionError("Custom route did not persist");
                store.resetRoute(original.name);store.resetRoute(original.name);
                LocalStore reloaded=new LocalStore(isolated);
                Route restored=reloaded.routes.get(original.name);
                if(!restored.raw.equals(original.raw)||restored.valid()!=original.valid())throw new AssertionError("Built-in route not restored");
                JSONObject custom=new JSONObject(isolated.getSharedPreferences("local",0).getString("routes","{}"));
                if(custom.has(original.name)||!custom.getString(unrelated).equals("1,31"))throw new AssertionError("Reset must remove only the selected override");
                store=reloaded;
            }
            report.putString("result","PASS: reset restores valid, empty and invalid built-ins; repeated reset is safe; recreation preserves reset; other custom routes retained; isolated preferences only");
        } finally {base.deleteSharedPreferences(preferences);}
    }
}
