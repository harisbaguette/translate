package kr.barobom.travel;
import android.graphics.Rect;
public final class ScanItem {
    public final String text;
    public final Rect bounds;
    public ScanItem(String text, Rect bounds) { this.text=text;this.bounds=new Rect(bounds); }
}
