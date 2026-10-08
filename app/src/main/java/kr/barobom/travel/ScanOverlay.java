package kr.barobom.travel;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

public final class ScanOverlay extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<ScanItem> items = Collections.emptyList();
    private int imageWidth=1,imageHeight=1,selected;
    public interface Listener { void select(int index); }
    public Listener listener;
    public ScanOverlay(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    public void update(List<ScanItem> data,int width,int height,int choice) { items=data; imageWidth=width;imageHeight=height;selected=choice;invalidate(); }
    private RectF mapped(Rect b) {
        float scale=Math.min((float)getWidth()/imageWidth,(float)getHeight()/imageHeight);
        float x=(getWidth()-imageWidth*scale)/2,y=(getHeight()-imageHeight*scale)/2;
        RectF r=new RectF(b.left*scale+x,b.top*scale+y,b.right*scale+x,b.bottom*scale+y);r.inset(-6,-6);return r;
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); float d=getResources().getDisplayMetrics().density;
        if(items.isEmpty()) {
            float x=getWidth()*.15f,y=getHeight()*.28f,w=getWidth()*.70f,h=getHeight()*.40f;
            paint.setColor(0x99D4F77D);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*d);
            float len=22*d;
            c.drawLine(x,y,x+len,y,paint);c.drawLine(x,y,x,y+len,paint);
            c.drawLine(x+w,y,x+w-len,y,paint);c.drawLine(x+w,y,x+w,y+len,paint);
            c.drawLine(x,y+h,x+len,y+h,paint);c.drawLine(x,y+h,x,y+h-len,paint);
            c.drawLine(x+w,y+h,x+w-len,y+h,paint);c.drawLine(x+w,y+h,x+w,y+h-len,paint);
        }
        for(int i=0;i<items.size();i++) {
            RectF r=mapped(items.get(i).bounds);
            paint.setStyle(Paint.Style.FILL);paint.setColor(i==selected?0x32D4F77D:0x10000000);c.drawRoundRect(r,6*d,6*d,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((i==selected?2:1)*d);paint.setColor(i==selected?0xFFD4F77D:0x88FFFFFF);c.drawRoundRect(r,6*d,6*d,paint);
        }
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_UP) {
            for(int i=0;i<items.size();i++)if(mapped(items.get(i).bounds).contains(event.getX(),event.getY())){if(listener!=null)listener.select(i);performClick();return true;}
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
