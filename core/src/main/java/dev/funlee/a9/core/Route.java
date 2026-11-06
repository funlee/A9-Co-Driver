package dev.funlee.a9.core;

import java.util.*;

/** Immutable route. Invalid recovered data remains visible but cannot be executed. */
public final class Route {
    public static final Set<String> CHOICES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("21", "22", "31", "32", "33", "41", "42", "43", "44", "360")));
    public static final class Event {
        public final int progress;
        public final String op;
        public final int a, count, interval;
        public final String choice;
        Event(int p, String op, int a, int n, int interval, String choice) {
            this.progress=p; this.op=op; this.a=a; this.count=n; this.interval=interval; this.choice=choice;
        }
    }
    public final String name, raw;
    public final List<Event> events;
    public final List<String> errors;
    private Route(String name, String raw, List<Event> events, List<String> errors) {
        this.name=name; this.raw=raw;
        this.events=Collections.unmodifiableList(events); this.errors=Collections.unmodifiableList(errors);
    }
    public boolean valid() { return errors.isEmpty() && !events.isEmpty(); }
    public static Route parse(String name, String input) {
        String raw=input.trim().replace('，', ',');
        List<Event> out=new ArrayList<>(); List<String> errors=new ArrayList<>();
        if (raw.length()>100_000) return new Route(name,raw,out,Collections.singletonList("线路超过长度限制"));
        String[] pairs=raw.split(",", -1); int previous=-1;
        if (pairs.length%2!=0) errors.add("进度与指令必须成对出现");
        for (int i=0; i+1<pairs.length; i+=2) {
            try {
                int p=Integer.parseInt(pairs[i].trim());
                if (p<0 || p>100) errors.add("进度超出 0–100："+p);
                if (p<previous) errors.add("进度倒退："+previous+" → "+p);
                previous=p;
                for(String command:pairs[i+1].split("\\|",-1)) {
                    String[] v=command.trim().split(":",-1);
                    if(v[0].equals("N") && v.length==4) {
                        int a=number(v[1]), n=number(v[2]), b=number(v[3]);
                        if(n>200 || a>60_000 || b>60_000) throw new IllegalArgumentException("氮气参数过大");
                        out.add(new Event(p,"N",a,n,b,null));
                    } else if ((v[0].equals("D") || v[0].equals("S")) && v.length==2) {
                        int duration=number(v[1]);
                        if(duration>60_000) throw new IllegalArgumentException("持续时间超过 60 秒");
                        out.add(new Event(p,v[0],duration,0,0,null));
                    } else if(v.length==1 && CHOICES.contains(v[0])) {
                        out.add(new Event(p,"C",0,0,0,v[0]));
                    } else throw new IllegalArgumentException("未知指令："+command);
                }
            } catch(IllegalArgumentException e) { errors.add("第 "+(i/2+1)+" 组："+e.getMessage()); }
        }
        if(out.isEmpty()) errors.add("线路为空");
        return new Route(name,raw,out,errors);
    }
    private static int number(String s) {
        int n=Integer.parseInt(s.trim()); if(n<0) throw new IllegalArgumentException("参数不能为负数"); return n;
    }
}
