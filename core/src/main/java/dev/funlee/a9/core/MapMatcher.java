package dev.funlee.a9.core;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public final class MapMatcher {
    private static final Pattern LAPS = Pattern.compile("(?<![0-9])[1-9][0-9]?[圈圏固]");
    private static final Pattern CHINESE_TITLE = Pattern.compile("\\p{sc=Han}{3,}");
    public static final class Rule {
        public final String display, internal;
        public final List<List<String>> groups;
        public final List<String> aliases;
        final String loadingRegion, loadingName;
        Rule(String display,String internal,List<List<String>> groups,List<String> aliases) {
            this.display=display;
            this.internal=internal;
            this.groups=groups;
            this.aliases=aliases;
            String full=normalizeLoading(display);
            loadingName=aliases.isEmpty()?"":normalizeLoading(aliases.get(0));
            loadingRegion=loadingName.isEmpty()||!full.endsWith(loadingName)?"":full.substring(0,full.length()-loadingName.length());
        }
        /** Panel format: 赛道名-地图名, split by the first alias suffix; unsplit names stay whole. */
        String splitDisplay() {
            if(aliases.isEmpty())return display;
            String name=aliases.get(0);
            if(!name.isEmpty()&&display.endsWith(name)&&display.length()>name.length())
                return display.substring(0,display.length()-name.length())+"-"+name;
            return display;
        }
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
    /** User maps use literal names, never the bundled TSV keyword syntax. */
    public void addCustomMap(String key,String track,String name) {
        rules.add(new Rule(track+name,key,List.of(),List.of(name)));
    }
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
    private static String normalizeLoading(String text) {
        return normalize(text).replace("|","").replace("｜","").replace('鷹','鹰').replace('櫻','樱');
    }
    private static boolean hasLapCount(String title) {
        return title.contains("圈")||LAPS.matcher(normalize(title)).find();
    }
    /** Both inputs are OCR reads of the map header on the same captured frame. */
    public static String loadingTitle(String header,String focused) {
        if(hasLapCount(focused))return focused;
        if(hasLapCount(header))return focused.isEmpty()?header:focused+"\n"+header;
        return focused;
    }
    public Match matchLoadingTitle(String header,String focused) {
        String title=loadingTitle(header,focused);
        String headerText=normalizeLoading(header),focusedText=normalizeLoading(focused);
        List<String> reads=List.of(focusedText,headerText);
        List<Rule> qualified=new ArrayList<>();
        Set<String> regions=new HashSet<>();
        for(Rule rule:rules) {
            if(rule.loadingRegion.isEmpty())continue;
            for(String read:reads) {
                if(read.contains(rule.loadingRegion))regions.add(rule.loadingRegion);
                if(read.contains(rule.loadingRegion+rule.loadingName))qualified.add(rule);
            }
        }
        if(!qualified.isEmpty())return new Match(dedupe(qualified),true);
        if(!hasLapCount(title))return new Match(List.of(),false);
        Match existing=match(normalizeLoading(title));
        if(!existing.candidates.isEmpty()||regions.size()!=1)return existing;

        String region=regions.iterator().next();
        Set<String> observedNames=new HashSet<>();
        for(String read:reads) {
            int start=read.indexOf(region);
            if(start<0)continue;
            String name=LAPS.split(read.substring(start+region.length()),2)[0];
            if(CHINESE_TITLE.matcher(name).matches())observedNames.add(name);
        }
        List<Rule> fuzzy=new ArrayList<>();
        for(Rule rule:rules) {
            if(!region.equals(rule.loadingRegion)||rule.loadingName.length()<4||
                    !CHINESE_TITLE.matcher(rule.loadingName).matches())continue;
            for(String observed:observedNames) {
                if(withinOneEdit(observed,rule.loadingName)){fuzzy.add(rule);break;}
            }
        }
        return new Match(dedupe(fuzzy),false);
    }
    private static boolean withinOneEdit(String shorter,String longer) {
        if(shorter.length()>longer.length())return withinOneEdit(longer,shorter);
        int difference=longer.length()-shorter.length();
        if(difference>1)return false;
        int prefix=0;
        while(prefix<shorter.length()&&shorter.charAt(prefix)==longer.charAt(prefix))prefix++;
        if(prefix==shorter.length())return true;
        int next=prefix+(difference==0?1:0);
        return shorter.regionMatches(next,longer,prefix+1,shorter.length()-next);
    }
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
        for(Rule r:rules) if(r.internal.equals(internal)) return r.splitDisplay();
        for(Rule r:rules) if(r.aliases.contains(internal)) return r.splitDisplay(); return internal;
    }
}
