package kr.barobom.travel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class Lexicon {
    public static final class Entry {
        public final String japanese, korean, reading, detail, category;
        Entry(String[] fields) { japanese=fields[0]; korean=fields[1]; reading=fields[2]; detail=fields[3]; category=fields[4]; }
    }
    private final Map<String, Entry> entries = new HashMap<>();
    private final List<String> keys = new ArrayList<>();
    private static final Pattern PRICE = Pattern.compile("[¥￥]\\s*[0-9,]+|[0-9,]+\\s*円|(?<=\\s)[0-9][0-9,]{2,}\\s*(?=税込|税抜|$)");
    public Lexicon(InputStream input) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line=r.readLine())!=null) {
                if (line.startsWith("#") || line.isBlank()) continue;
                String[] f=line.split("\t",-1); if(f.length!=5) throw new IOException("Invalid lexicon entry");
                Entry e=new Entry(f);
                for(String alias:f[0].split("\\|")) {
                    String key=normalize(alias);
                    if(entries.put(key,e)!=null) throw new IOException("Duplicate lexicon entry: "+key);
                }
            }
        }
        keys.addAll(entries.keySet()); keys.sort(Comparator.comparingInt(String::length).reversed());
    }
    public static String normalize(String text) {
        String t=Normalizer.normalize(text,Normalizer.Form.NFKC);
        t=PRICE.matcher(t).replaceAll("");
        t = t.replaceAll("[\\s・･:：()（）「」『』【】\\[\\]、。]", "");
        if (t.length() > 2) t = t.replaceAll("(?:税込|税抜)$", "");
        return t;
    }
    public Entry exact(String text) { return entries.get(normalize(text)); }
    public String hints(String text) {
        String rest=normalize(text); List<String> result=new ArrayList<>();
        for(String key:keys) {
            if(key.length()<2 || !rest.contains(key)) continue;
            Entry entry=entries.get(key);
            if(!result.contains(entry.korean)) result.add(entry.korean);
            rest=rest.replace(key, " ");
            if(result.size()==3) break;
        }
        return String.join(" · ",result);
    }
    public String glossary(String text) {
        String rest=normalize(text);StringBuilder out=new StringBuilder();int count=0;
        for(String key:keys){if(key.length()<2||!rest.contains(key))continue;
            out.append(key).append(" translates to ").append(entries.get(key).korean).append(".\n");
            rest=rest.replace(key," ");if(++count==6)break;
        }
        return out.toString();
    }
    public static final class ProtectedText {
        public final String source;
        private final LinkedHashMap<String,String> terms;
        private final Map<String,Integer> counts;
        ProtectedText(String source,LinkedHashMap<String,String> terms,Map<String,Integer> counts){this.source=source;this.terms=terms;this.counts=counts;}
        public String restore(String translated){
            for(Map.Entry<String,String> term:terms.entrySet()){
                if(occurrences(translated,term.getKey())!=counts.get(term.getKey()))return "";
                translated=translated.replace(term.getKey(),term.getValue());
            }
            return translated.contains("__TERM")?"":translated;
        }
    }
    private static int occurrences(String text,String token){return (text.length()-text.replace(token, "").length())/token.length();}
    public ProtectedText protect(String text){
        String source=Normalizer.normalize(text,Normalizer.Form.NFKC);
        LinkedHashMap<String,String> terms=new LinkedHashMap<>();Map<String,Integer> counts=new HashMap<>();
        if(source.contains("__TERM"))return new ProtectedText(source,terms,counts);
        for(String key:keys){
            if(key.length()<2||!source.contains(key))continue;
            String marker="__TERM"+terms.size()+"__";
            counts.put(marker,occurrences(source,key));terms.put(marker,entries.get(key).korean);
            source=source.replace(key,marker);
            if(terms.size()==6)break;
        }
        return new ProtectedText(source,terms,counts);
    }
    public int size() { return entries.size(); }
}
