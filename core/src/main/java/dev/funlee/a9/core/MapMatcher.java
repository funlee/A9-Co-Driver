package dev.funlee.a9.core;

import java.io.*;
import java.util.*;

public final class MapMatcher {
    public static final class Rule {
        public final String display, internal;
        public final List<List<String>> groups;
        Rule(String display,String internal,List<List<String>> groups) { this.display=display; this.internal=internal; this.groups=groups; }
        boolean matches(String text) {
            for(List<String> g:groups) { boolean yes=true; for(String w:g) yes &= text.contains(w); if(yes) return true; }
            return false;
        }
    }
    public static final class Match {
        public final List<Rule> candidates;
        public final boolean exact;
        Match(List<Rule> c,boolean e) { candidates=Collections.unmodifiableList(c); exact=e; }
        public boolean unique() { return candidates.size()==1; }
    }
    public final List<Rule> rules=new ArrayList<>();
    /** TSV columns: display, route key, keyword alternatives (AND '/', OR '|'). */
    public MapMatcher(Reader input) throws IOException {
        BufferedReader r=new BufferedReader(input); String line;
        while((line=r.readLine())!=null) {
            String[] v=line.split("\t",-1); if(v.length!=3) throw new IOException("Invalid map rule");
            List<List<String>> groups=new ArrayList<>();
            for(String g:v[2].split("\\|")) groups.add(Arrays.asList(g.split("/")));
            rules.add(new Rule(v[0],v[1],groups));
        }
    }
    public static String normalize(String s) { return s==null?"":s.replaceAll("[\\s\\p{Punct}，。·：%]", ""); }
    public Match match(String input) {
        String text=normalize(input); List<Rule> exact=new ArrayList<>(), partial=new ArrayList<>();
        if(text.isEmpty()) return new Match(exact,false);
        for(Rule r:rules) {
            if(text.contains(normalize(r.display)) || text.contains(normalize(r.internal))) exact.add(r);
            else if(r.matches(text)) partial.add(r);
        }
        return new Match(dedupe(exact.isEmpty()?partial:exact),!exact.isEmpty());
    }
    private List<Rule> dedupe(List<Rule> input) {
        Map<String,Rule> out=new LinkedHashMap<>(); for(Rule r:input) out.putIfAbsent(r.internal,r); return new ArrayList<>(out.values());
    }
    public String displayName(String internal) {
        for(Rule r:rules) if(r.internal.equals(internal)) return r.display; return internal;
    }
}
