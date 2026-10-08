package kr.barobom.travel;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DeviceTest {
    private Context context;
    @Before public void context(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();}
    static Bitmap menu(){
        Bitmap b=Bitmap.createBitmap(1280,1000,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(0xFFF4ECDD);Paint p=new Paint(3);p.setColor(0xFF322A20);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextLocale(Locale.JAPAN);p.setTextSize(48);c.drawText("本日のおすすめ",70,100,p);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(54);
        String[] rows={"あさりの酒蒸し　680円", "鶏の唐揚げ　780円", "だし巻き卵　580円", "ねぎま　220円", "生ビール　550円", "税込"};for(int i=0;i<rows.length;i++)c.drawText(rows[i],70,230+i*120,p);return b;
    }
    @Test public void actualJapaneseOcrAndInstantDictionary() throws Exception {
        TextRecognizer r=TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
        Bitmap b=menu(); try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir("verification"),"menu-fixture.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
        JSONArray runs=new JSONArray();String result="";
        for(int n=0;n<4;n++){long t=SystemClock.elapsedRealtime();Text found=Tasks.await(r.process(InputImage.fromBitmap(b,0)),30,TimeUnit.SECONDS);long duration=SystemClock.elapsedRealtime()-t;result=found.getText();runs.put(duration);}
        assertTrue(result,result.contains("酒蒸"));assertTrue(result,result.contains("唐揚"));assertTrue(result,result.contains("ねぎま"));
        Lexicon dictionary=new Lexicon(context.getAssets().open("lexicon.tsv"));long t=System.nanoTime();for(int i=0;i<1000;i++){assertEquals("바지락 술찜",dictionary.exact("あさりの酒蒸し 680円").korean);}double mean=(System.nanoTime()-t)/1e6/1000;
        JSONObject data=new JSONObject().put("ocr_ms",runs).put("lookup_mean_ms",mean).put("recognized",result).put("entries",dictionary.size());write("ocr.json",data.toString(2));r.close();
    }
    @Test public void actualHyMtModel() throws Exception {
        NativeTranslator translator=NativeTranslator.get(context);long load=SystemClock.elapsedRealtime();
        while(!translator.ready&&!translator.failed&&SystemClock.elapsedRealtime()-load<90000)Thread.sleep(200);
        assertFalse("Hy-MT2 load failed",translator.failed);assertTrue("Hy-MT2 did not become ready",translator.ready);
        JSONArray results=new JSONArray();String[] inputs={"季節の野菜と鶏肉のスープ", "本品は詰め替え用です", "ゆず香る鶏白湯らーめん", "冷暗所に保存してください"};
        for(String source:inputs){CountDownLatch latch=new CountDownLatch(1);String[] translation={""};long[] ms={0};translator.translate(source,(s,time)->{translation[0]=s;ms[0]=time;latch.countDown();});assertTrue(latch.await(30,TimeUnit.SECONDS));results.put(new JSONObject().put("ja",source).put("ko",translation[0]).put("ms",ms[0]));write("translation.json",results.toString(2));assertFalse("Empty translation: "+source,translation[0].isEmpty());
            if(source.contains("詰め替え"))assertTrue(translation[0],translation[0].contains("리필"));
            if(source.contains("ゆず"))assertTrue(translation[0],translation[0].contains("유자")&&translation[0].contains("라멘"));
            if(source.contains("冷暗所"))assertTrue(translation[0],translation[0].contains("어두"));}
        write("translation.json",results.toString(2));
        CountDownLatch cached=new CountDownLatch(1);translator.translate(inputs[0],(s,time)->{assertEquals(0,time);cached.countDown();});assertTrue(cached.await(3,TimeUnit.SECONDS));
    }
    @Test public void photoScreenAndLifecycle() throws Exception {
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->a.processPhoto(menu()));Thread.sleep(2400);
            InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,new FileOutputStream(new File(context.getExternalFilesDir("verification"),"photo-screen.png")));
            activity.recreate();Thread.sleep(800);
            activity.onActivity(a->assertNotNull(a.findViewById(android.R.id.content)));
        }
    }
    private void write(String name,String text) throws IOException {File dir=context.getExternalFilesDir("verification");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){out.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}Log.i("BarobomTest",name+" "+text);}
}
