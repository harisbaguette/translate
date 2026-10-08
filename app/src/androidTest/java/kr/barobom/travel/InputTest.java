package kr.barobom.travel;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class InputTest {
    @Test public void sharedPhotoAndBrokenInputRecover() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiDevice d=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"barobom-menu-test.png");values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        Uri image=c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(image);
        try {
            try(OutputStream out=c.getContentResolver().openOutputStream(image)){DeviceTest.menu().compress(Bitmap.CompressFormat.PNG,100,out);}
            Intent share=new Intent(c,MainActivity.class).setAction(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,image).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(share)) {
                assertTrue(d.wait(Until.hasObject(By.text("육수 달걀말이")),8000));
                assertNotNull(d.findObject(By.desc("다시 읽기")));
                d.takeScreenshot(new File(c.getExternalFilesDir("verification"),"shared-photo.png"));
                d.pressBack();assertTrue(d.wait(Until.hasObject(By.desc("결과 고정")),3000));
            }
            try(OutputStream out=c.getContentResolver().openOutputStream(image,"wt")){out.write("not an image".getBytes());}
            try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(share)) {
                assertTrue(d.wait(Until.hasObject(By.desc("결과 고정")),5000));
                assertNotNull(d.findObject(By.desc("사진")));
            }
            Intent malformed=new Intent(c,MainActivity.class).setAction(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,new android.os.Bundle());
            try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(malformed)) {
                assertTrue(d.wait(Until.hasObject(By.desc("결과 고정")),3000));
            }
        } finally {c.getContentResolver().delete(image,null,null);}
    }
    @Test public void blankPhotoHasRecovery() throws Exception {
        UiDevice d=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)) {
            Bitmap blank=Bitmap.createBitmap(2048,2048,Bitmap.Config.ARGB_8888);blank.eraseColor(Color.WHITE);
            a.onActivity(activity->activity.processPhoto(blank));
            assertTrue(d.wait(Until.hasObject(By.text("읽을 글자를 찾지 못했어요")),8000));
            UiObject2 resume=d.wait(Until.findObject(By.desc("다시 읽기")),3000);assertNotNull(resume);resume.click();
            assertTrue(d.wait(Until.hasObject(By.desc("결과 고정")),3000));
        }
    }
}
