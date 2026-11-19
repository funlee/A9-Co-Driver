package dev.funlee.a9.core;

import party.iroiro.luajava.*;
import party.iroiro.luajava.lua53.Lua53;
import java.util.*;

/** One Lua VM on one worker. Lua coroutines provide cooperative sleep and background tasks. */
public final class LuaRuntime implements AutoCloseable {
    public interface Host { int call(String name,Lua stack); }
    private final Lua lua;
    private volatile boolean stopped;
    public LuaRuntime(Host host,Collection<String> functions) {
        lua=new Lua53(); lua.openLibraries();
        for(String name:functions) {
            lua.push((JFunction) stack -> host.call(name,stack)); lua.setGlobal(name);
        }
        lua.push((JFunction) stack->{ stack.push(stopped); return 1; }); lua.setGlobal("_cancelled");
        lua.run("_tasks={}; _now=0; _current=nil\n"+
                "function _hook() if _cancelled() then error('运行已停止') end end\n"+
                "debug.sethook(_hook,'',10000)\n"+
                "function sleep(ms) return coroutine.yield(math.max(1,tonumber(ms) or 1)) end\n"+
                "function beginThread(fn,...) local args=table.pack(...); local co=coroutine.create(function() fn(table.unpack(args,1,args.n)) end); " +
                "debug.sethook(co,_hook,'',10000); local t={co=co,due=_now}; _tasks[#_tasks+1]=t; return #_tasks end\n"+
                "function tickCount() return _now end\n"+
                "function exitScript() error('运行已结束') end\n"+
                "function _step(now) _now=now; local n=#_tasks; for i=1,n do local t=_tasks[i]; if t and t.due<=now and coroutine.status(t.co)~='dead' then " +
                "local ok,ms=coroutine.resume(t.co); if not ok then error(debug.traceback(t.co,tostring(ms))) end; t.due=now+math.max(1,tonumber(ms) or 1) end end; " +
                "for i=#_tasks,1,-1 do if coroutine.status(_tasks[i].co)=='dead' then table.remove(_tasks,i) end end; return #_tasks end\n");
    }
    public void load(String name,String source) {
        try { lua.run(source); } catch(RuntimeException e) { throw new IllegalStateException(name+": "+e.getMessage(),e); }
    }
    public void set(String name,Object value) { push(lua,value); lua.setGlobal(name); }
    public void start(String function) { lua.run("beginThread("+function+")"); }
    public boolean step(long now) {
        if(stopped) return false;
        lua.getGlobal("_step"); lua.push(now); lua.pCall(1,1);
        boolean alive=lua.toInteger(-1)>0; lua.pop(1); return alive;
    }
    public void requestStop() { stopped=true; }
    public static void push(Lua l,Object value) {
        if(value==null) l.pushNil();
        else if(value instanceof Boolean) l.push((boolean)value);
        else if(value instanceof Number) l.push((Number)value);
        else if(value instanceof Map) {
            l.newTable(); for(Object entry:((Map<?,?>)value).entrySet()) {
                Map.Entry<?,?> e=(Map.Entry<?,?>)entry; push(l,e.getValue()); l.setField(-2,String.valueOf(e.getKey()));
            }
        } else if(value instanceof Collection) {
            l.newTable(); int i=1; for(Object item:(Collection<?>)value) { push(l,item); l.rawSetI(-2,i++); }
        } else l.push(String.valueOf(value));
    }
    @Override public void close() { stopped=true; lua.close(); }
}
