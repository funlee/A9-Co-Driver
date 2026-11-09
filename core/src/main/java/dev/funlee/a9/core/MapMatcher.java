package dev.funlee.a9.core;

import java.io.*;
import java.util.*;

public final class MapMatcher {
    public static final class Rule {
        public final String display, internal;
        public final List<List<String>> groups;
        public final List<String> aliases;
        Rule(String display,String internal,List<List<String>> groups,List<String> aliases) { this.display=display; this.internal=internal; this.groups=groups; this.aliases=aliases; }
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
    /** TSV columns: display, route key, keywords (AND '/', OR '|'), optional exact aliases (OR '|'). */
    public MapMatcher(Reader input) throws IOException {
        BufferedReader r=new BufferedReader(input); String line;
        while((line=r.readLine())!=null) {
            String[] v=line.split("\t",-1); if(v.length!=3&&v.length!=4) throw new IOException("Invalid map rule");
            List<List<String>> groups=new ArrayList<>();
            for(String g:v[2].split("\\|")) groups.add(Arrays.asList(g.split("/")));
            rules.add(new Rule(v[0],v[1],groups,v.length==4?Arrays.asList(v[3].split("\\|")):List.of()));
        }
    }
    public static String normalize(String s) { return s==null?"":s.replaceAll("[\\s\\p{P}％%]", ""); }
    public Match match(String input) {
        String text=normalize(input); List<Rule> exact=new ArrayList<>(), aliases=new ArrayList<>(), partial=new ArrayList<>();
        int longestAlias=0;
        if(text.isEmpty()) return new Match(exact,false);
        for(Rule r:rules) {
            int aliasLength=0;for(String name:r.aliases){String value=normalize(name);if(!value.isEmpty()&&text.contains(value))aliasLength=Math.max(aliasLength,value.length());}
            if(text.contains(normalize(r.display)) || text.contains(normalize(r.internal))) exact.add(r);
            else if(aliasLength>0){
                if(aliasLength>longestAlias){aliases.clear();longestAlias=aliasLength;}
                if(aliasLength==longestAlias)aliases.add(r);
            }
            else if(r.matches(text)) partial.add(r);
        }
        return new Match(dedupe(!exact.isEmpty()?exact:!aliases.isEmpty()?aliases:partial),!exact.isEmpty()||!aliases.isEmpty());
    }
    private List<Rule> dedupe(List<Rule> input) {
        Map<String,Rule> out=new LinkedHashMap<>(); for(Rule r:input) out.putIfAbsent(r.internal,r); return new ArrayList<>(out.values());
    }
    public String displayName(String internal) {
        for(Rule r:rules) if(r.internal.equals(internal)) return r.display;
        for(Rule r:rules) if(r.aliases.contains(internal)) return r.display; return internal;
    }
}
