package kr.barobom.travel;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.uiautomator.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
public class AuditTest {
 private final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
 private final UiDevice d=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
 private void write(String name,Object data)throws Exception{try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir("audit"),name))){out.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
 private static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 private boolean awaitText(String value,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;do{if(d.findObject(By.text(value))!=null)return true;Thread.sleep(16);}while(SystemClock.elapsedRealtime()<end);return false;}
 private void click(BySelector selector){UiObject2 b=d.wait(Until.findObject(selector),4000);assertNotNull(selector.toString(),b);android.graphics.Point point=b.getVisibleCenter();d.click(point.x,point.y);d.waitForIdle(1000);}
 @Test public void a_coldStartupAndPhotoToMeaning() throws Exception {
  long begin=SystemClock.elapsedRealtime();JSONArray samples=new JSONArray();
  try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)){
   long ui=SystemClock.elapsedRealtime()-begin;
   for(int i=0;i<12;i++){
    CountDownLatch drawn=new CountDownLatch(1);long[] elapsed={0};Bitmap menu=DeviceTest.menu();
    a.onActivity(v->{long started=SystemClock.elapsedRealtime();View content=v.findViewById(android.R.id.content);
      content.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){try{android.widget.TextView status=(android.widget.TextView)field(v,"stateText");if("기본 뜻".contentEquals(status.getText())){elapsed[0]=SystemClock.elapsedRealtime()-started;content.getViewTreeObserver().removeOnPreDrawListener(this);drawn.countDown();}}catch(Exception e){throw new AssertionError(e);}return true;}});
      v.processPhoto(menu);
    });assertTrue(drawn.await(8,TimeUnit.SECONDS));samples.put(elapsed[0]);
   }
   
  NativeTranslator t=NativeTranslator.get(c);long limit=SystemClock.elapsedRealtime()+45000;
   while(!t.ready&&!t.failed&&SystemClock.elapsedRealtime()<limit)Thread.sleep(20);
   assertTrue("Model preparation",t.ready);
   write("startup.json",new JSONObject().put("activity_launch_ms",ui).put("model_ready_after_test_start_ms",Math.max(0,t.preparedAtElapsedMs-begin)).put("photo_ocr_to_first_frame_ms",samples).toString(2));
   d.takeScreenshot(new File(c.getExternalFilesDir("audit"),"portrait.png"));
  }
 }
 @Test public void b_translationBreadthAndLatency() throws Exception {
  if(InstrumentationRegistry.getArguments().getString("alternate_model")!=null){File alternate=new File(c.getExternalFilesDir(null),"compare-2bit.gguf");assertTrue(alternate.isFile());assertTrue(NativeTranslator.nativeLoad(alternate.getAbsolutePath(),4));}
  NativeTranslator t=NativeTranslator.get(c);long limit=SystemClock.elapsedRealtime()+60000;while(!t.ready&&!t.failed&&SystemClock.elapsedRealtime()<limit)Thread.sleep(20);assertTrue(t.ready);
  String[] sources={"数量限定の炙りサーモン丼","北海道産ミルクの濃厚プリン","お一人様一品のご注文をお願いします","辛さは控えめにできます","本品は飲み物ではありません","これは詰め替え用ではありません","開封後は冷蔵庫で保存してください","レンジで温めず、そのままお召し上がりください","香ばしい味噌だれの焼きおにぎり","さっぱりした梅しそ風味の鶏天","食器用洗剤のつめかえパック","ご使用前によく振ってください","お好みで七味をかけてください","国産豚バラのねぎ塩炒め","海老と帆立のバター醤油焼き","麺の大盛りは無料です","こちらは化粧水ではなく乳液です","洗濯用なので食器には使用しないでください","お子様には辛すぎる場合があります","賞味期限は袋の裏面に記載しています"};
  JSONArray data=new JSONArray();
  for(String source:sources){CountDownLatch done=new CountDownLatch(1);String[] result={""};long[] elapsed={0};t.translate(source,(v,ms)->{result[0]=v;elapsed[0]=ms;done.countDown();});assertTrue(done.await(20,TimeUnit.SECONDS));data.put(new JSONObject().put("ja",source).put("ko",result[0]).put("ms",elapsed[0]));write("translation-breadth.json",data.toString(2));assertFalse("Empty: "+source,result[0].isEmpty());}
 }
 @Test public void b2_unseenTravelPhrases() throws Exception {
  NativeTranslator t=NativeTranslator.get(c);long limit=SystemClock.elapsedRealtime()+60000;while(!t.ready&&!t.failed&&SystemClock.elapsedRealtime()<limit)Thread.sleep(20);assertTrue(t.ready);
  String[] sources={"わさび抜きでお願いします","つゆは別添えです","鯖の骨にご注意ください","卵と乳成分を含みます","辛くないカレーもあります","こちらは食べられません","2個入りで税込398円です","保湿成分を配合した日焼け止め","冷凍したまま油で揚げてください","トイレは階段を上がって右側です"};
  JSONArray data=new JSONArray();
  for(String source:sources){CountDownLatch done=new CountDownLatch(1);String[] result={""};long[] elapsed={0};t.translate(source,(v,ms)->{result[0]=v;elapsed[0]=ms;done.countDown();});assertTrue(done.await(20,TimeUnit.SECONDS));data.put(new JSONObject().put("ja",source).put("ko",result[0]).put("ms",elapsed[0]));write("translation-holdout.json",data.toString(2));assertFalse("Empty: "+source,result[0].isEmpty());}
 }
 @Test public void c_rotationPreservesReadablePhoto() throws Exception {
  try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)){
   a.onActivity(v->v.processPhoto(DeviceTest.menu()));assertTrue(d.wait(Until.hasObject(By.text("육수 달걀말이")),5000));
   d.setOrientationLeft();Thread.sleep(700);assertNotNull(d.findObject(By.text("육수 달걀말이")));assertNotNull(d.findObject(By.desc("저장")));d.takeScreenshot(new File(c.getExternalFilesDir("audit"),"landscape.png"));
   a.recreate();assertTrue(d.wait(Until.hasObject(By.text("육수 달걀말이")),5000));
   a.onActivity(v->{try{assertTrue((boolean)field(v,"photoMode"));assertTrue((boolean)field(v,"frozen"));assertNull(field(v,"camera"));}catch(Exception e){throw new AssertionError(e);}});
  } finally {d.setOrientationNatural();d.unfreezeRotation();}
 }
 @Test public void d_favoritesOrderAndModalCameraPause() throws Exception {
  c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).edit().remove("favorites_ordered").remove("favorites").commit();
  try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)){
   a.onActivity(v->v.processPhoto(DeviceTest.menu()));assertTrue(d.wait(Until.hasObject(By.text("육수 달걀말이")),5000));click(By.desc("저장"));
   click(By.descContains("닭고기 대파 꼬치"));click(By.desc("저장"));
   a.recreate();click(By.desc("저장한 단어"));
   assertTrue(d.wait(Until.hasObject(By.text("여행 단어장")),3000));
   assertTrue(d.findObject(By.text("닭고기 대파 꼬치")).getVisibleCenter().y<d.findObject(By.text("육수 달걀말이")).getVisibleCenter().y);
   a.onActivity(v->{try{assertNull(field(v,"camera"));}catch(Exception e){throw new AssertionError(e);}});
   click(By.text("전체 삭제"));assertTrue(d.wait(Until.hasObject(By.text("저장한 단어를 모두 삭제할까요?")),3000));click(By.text("취소"));assertTrue(d.wait(Until.hasObject(By.text("여행 단어장")),3000));
   click(By.text("전체 삭제"));assertTrue(d.wait(Until.hasObject(By.text("저장한 단어를 모두 삭제할까요?")),3000));click(By.res("android:id/button1"));assertTrue(d.wait(Until.hasObject(By.textContains("아직 저장한 단어가 없어요")),2000));
  }
 }
 @Test public void e_liveCameraToMeaningAndFreeze() throws Exception {
  try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)){
   long start=SystemClock.elapsedRealtime();
   assertTrue("Camera must recognize the emulator menu image",awaitText("육수 달걀말이",15000));
   long elapsed=SystemClock.elapsedRealtime()-start;click(By.desc("결과 고정"));
   a.onActivity(v->{try{assertTrue((boolean)field(v,"frozen"));assertNull(field(v,"camera"));}catch(Exception e){throw new AssertionError(e);}});
   assertTrue(awaitText("다시 읽기",3000));d.waitForIdle(1000);d.takeScreenshot(new File(c.getExternalFilesDir("audit"),"camera-menu.png"));write("camera.json",new JSONObject().put("camera_to_meaning_ms",elapsed).put("source","Emulator imagefile camera, real CameraX ImageAnalysis + ML Kit pipeline").toString(2));
  }
 }
}
