package kr.barobom.travel;

import java.util.List;

/** Keeps a readable result steady while OCR boxes move between camera frames. */
public final class ScanSelection {
    private String current = "", candidate = "";
    private long candidateSince;
    private boolean manual;

    public void clear() { current=""; candidate=""; manual=false; }
    public void select(String key) { current=key; candidate=""; manual=true; }
    public int choose(List<String> keys, List<Float> distances, long now) {
        if(keys.isEmpty()) return -1;
        if(current.isEmpty()) { current=keys.get(0); return 0; }
        int previous=keys.indexOf(current);
        if(previous==0 || (previous>=0 && (manual || distances.get(previous)-distances.get(0)<.12f))) {
            candidate=""; return previous;
        }
        String next=keys.get(0);
        if(!next.equals(candidate)) { candidate=next; candidateSince=now; }
        // A missed frame must not discard a manually selected result immediately.
        long hold=previous<0?240:320;
        if(now-candidateSince<hold) return previous;
        current=next;candidate="";manual=false;return 0;
    }
}
