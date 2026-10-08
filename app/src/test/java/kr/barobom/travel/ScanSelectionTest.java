package kr.barobom.travel;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScanSelectionTest {
 @Test public void firstResultDoesNotWait(){ScanSelection s=new ScanSelection();assertEquals(0,s.choose(List.of("A"),List.of(.1f),0));}
 @Test public void jitterDoesNotSwitchOrStarveTranslation(){ScanSelection s=new ScanSelection();s.choose(List.of("A","B"),List.of(.10f,.12f),0);for(int i=1;i<40;i++)assertEquals(1,s.choose(List.of("B","A"),List.of(.10f,.12f),i*180));}
 @Test public void movingToAnotherMenuChangesWithinTwoFrames(){ScanSelection s=new ScanSelection();s.choose(List.of("A"),List.of(.1f),0);assertEquals(-1,s.choose(List.of("B"),List.of(.1f),180));assertEquals(-1,s.choose(List.of("B"),List.of(.1f),360));assertEquals(0,s.choose(List.of("B"),List.of(.1f),540));}
 @Test public void explicitChoiceSurvivesCenterMovement(){ScanSelection s=new ScanSelection();s.select("A");assertEquals(1,s.choose(List.of("B","A"),List.of(.01f,.4f),1000));}
 @Test public void briefMissDoesNotChangeSelection(){ScanSelection s=new ScanSelection();s.select("A");assertEquals(-1,s.choose(List.of("B"),List.of(.1f),180));assertEquals(0,s.choose(List.of("A","B"),List.of(.1f,.2f),360));}
}
