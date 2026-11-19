package dev.funlee.a9;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.*;
import java.lang.reflect.*;

/** Exercise the production service window without starting automation or changing preferences. */
final class PanelVisibilityDeviceCheck {
    static void run(Instrumentation test,Bundle report)throws Exception{
        if(SessionState.running)throw new IllegalStateException("Stop automation before checking panel visibility");
        AutomationService service=new AutomationService();
        Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);
        attach.invoke(service,new ContextThemeWrapper(test.getTargetContext(),R.style.AppTheme));
        Method add=method("addOverlay"),update=method("updateOverlay"),remove=method("removeOverlay");
        try{
            test.runOnMainSync(()->invoke(add,service));
            check(test,service,false,false,false,false,update,"startup/menu/loading");
            check(test,service,true,false,false,true,update,"race");
            SessionState.paused=true;
            check(test,service,true,false,false,true,update,"paused race");SessionState.paused=false;
            check(test,service,true,true,false,false,update,"settlement");
            check(test,service,true,false,true,false,update,"car menu");
            check(test,service,false,false,false,false,update,"returned to menu");
            check(test,service,true,false,false,true,update,"next race");
            report.putString("result","PASS: production panel hidden and non-touchable at startup/menu/loading/settlement; visible during race and paused race; visibility restored on next race; no config changes");
        }finally{test.runOnMainSync(()->invoke(remove,service));SessionState.paused=false;}
    }
    private static void check(Instrumentation test,AutomationService service,boolean race,boolean result,boolean menu,boolean expected,Method update,String scene)throws Exception{
        test.runOnMainSync(()->{
            SessionState.racePanelVisible=race;SessionState.resultOverlay=result;SessionState.overlayMenuCompact=menu;invoke(update,service);
        });test.waitForIdleSync();Thread.sleep(150);
        test.runOnMainSync(()->{
            try{
                TaskPanelView overlay=(TaskPanelView)field("overlay").get(service);
                WindowManager.LayoutParams params=(WindowManager.LayoutParams)field("overlayParams").get(service);
                if((overlay.getVisibility()==View.VISIBLE)!=expected)throw new AssertionError("Wrong panel visibility: "+scene);
                if(((params.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)==0)!=expected)throw new AssertionError("Hidden panel intercepts gestures: "+scene);
                if((SessionState.overlayBounds!=null)!=expected)throw new AssertionError("Stale overlay bounds: "+scene);
                if(expected){
                    Field controls=TaskPanelView.class.getDeclaredField("actions");
                    controls.setAccessible(true);
                    View actions=(View)controls.get(overlay);
                    if(actions.getVisibility()!=View.GONE)throw new AssertionError("Race panel contains task controls");
                }
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
    }
    private static Method method(String name)throws Exception{Method m=AutomationService.class.getDeclaredMethod(name);m.setAccessible(true);return m;}
    private static Field field(String name)throws NoSuchFieldException{Field f=AutomationService.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void invoke(Method method,Object target){try{method.invoke(target);}catch(Exception e){throw new AssertionError(e);}}
}
