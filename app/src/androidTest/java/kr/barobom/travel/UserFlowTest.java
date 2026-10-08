package kr.barobom.travel;
import android.content.*;
import android.os.*;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UserFlowTest {
    private static void click(UiDevice device,BySelector selector){UiObject2 o=device.wait(Until.findObject(selector),4000);assertNotNull("Missing "+selector,o);android.graphics.Point point=o.getVisibleCenter();device.click(point.x,point.y);device.waitForIdle(1000);}
    @Test public void saveReadDeleteAndBack() throws Exception {
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->a.processPhoto(DeviceTest.menu()));
            assertTrue(device.wait(Until.hasObject(By.text("육수 달걀말이")),8000));
            click(device,By.desc("저장"));
            click(device,By.desc("저장한 단어"));
            assertTrue(device.wait(Until.hasObject(By.text("여행 단어장")),3000));
            assertNotNull(device.findObject(By.text("육수 달걀말이")));
            click(device,By.text("전체 삭제"));
            assertTrue(device.wait(Until.hasObject(By.text("저장한 단어를 모두 삭제할까요?")),2000));
            click(device,By.text("취소"));
            assertNotNull(device.wait(Until.findObject(By.text("육수 달걀말이")),2000));
            click(device,By.text("전체 삭제"));assertTrue(device.wait(Until.hasObject(By.text("저장한 단어를 모두 삭제할까요?")),3000));click(device,By.res("android:id/button1"));
            assertNotNull(device.wait(Until.findObject(By.textContains("아직 저장한 단어가 없어요")),2000));
            device.pressBack();
            click(device,By.desc("사용 안내와 설정"));click(device,By.text("이미지·저장 정보"));
            assertNotNull(device.wait(Until.findObject(By.text("기기 안에서 처리해요")),2000));
            device.pressBack();device.pressBack();
            assertTrue(device.wait(Until.hasObject(By.desc("결과 고정")),3000));
        }
    }
    @Test public void largeTextPhotoScreen() throws Exception {
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String before=device.executeShellCommand("settings get system font_scale").trim();
        try {
            device.executeShellCommand("settings put system font_scale 2.0");
            try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
                activity.onActivity(a->a.processPhoto(DeviceTest.menu()));
                assertTrue(device.wait(Until.hasObject(By.text("육수 달걀말이")),8000));
                assertNotNull(device.findObject(By.desc("사진")));assertNotNull(device.findObject(By.desc("저장")));
                device.takeScreenshot(new File(c.getExternalFilesDir("verification"),"large-text.png"));
            }
        } finally {device.executeShellCommand("settings put system font_scale "+before);}
    }
}
