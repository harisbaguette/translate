package kr.barobom.travel;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends ComponentActivity {
    static final int BG=0xFF101A17, PANEL=0xFF1C2923, GREEN=0xFFD4F77D, TEXT=0xFFF3F4EC, MUTED=0xFFAFBCAF;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService analysisExecutor=Executors.newSingleThreadExecutor();
    private final ExecutorService photoExecutor=Executors.newSingleThreadExecutor();
    private TextRecognizer recognizer;
    private NativeTranslator translator;
    private Lexicon lexicon;
    private ProcessCameraProvider cameraProvider;
    private androidx.camera.core.Camera camera;
    private PreviewView preview;
    private ImageView photoView;
    private ScanOverlay overlay;
    private LinearLayout results, root, body, bottom;
    private FrameLayout viewfinder;
    private TextView engineStatus;
    private int modalDepth;
    private boolean cameraStarting;
    private final ScanSelection selection=new ScanSelection();
    private ScanState scanState;
    public static final class ScanState extends ViewModel { Bitmap photo; List<ScanItem> items=new ArrayList<>(); int width=1,height=1,selected; String source=""; boolean held; }
    private TextView stateText, guide, freezeText;
    private ImageButton flashButton;
    private LinearLayout zoomControls;
    private View permissionPanel;
    private List<ScanItem> items=new ArrayList<>();
    private int selected=0,imageWidth=1,imageHeight=1;
    private volatile boolean frozen=false,photoMode=false,active=false,ocrBusy=false;
    private boolean torch=false, destroyed=false, photoLoading=false;
    private volatile long lastFrame=0;
    private long ocrMs=0;
    private String selectedText="", signature="", aiText="", aiFor="", aiErrorFor="";
    private long aiMs=0;
    private int inputEpoch=0;
    private Runnable translateTask;
    private final Set<String> favorites=new LinkedHashSet<>();
    private static final int PICK=21,CAMERA_PERMISSION=22;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        try {lexicon=new Lexicon(getAssets().open("lexicon.tsv"));}
        catch(IOException e){throw new IllegalStateException("여행 사전을 열지 못했습니다",e);}
        String stored=getPreferences(MODE_PRIVATE).getString("favorites_ordered",null);
        if(stored!=null)try {JSONArray a=new JSONArray(stored);for(int i=0;i<a.length();i++)favorites.add(a.getString(i));}catch(JSONException ignored){}
        else favorites.addAll(getPreferences(MODE_PRIVATE).getStringSet("favorites",Collections.emptySet()));
        scanState=new ViewModelProvider(this).get(ScanState.class);
        recognizer=TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
        translator=NativeTranslator.get(this);
        buildScreen();
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){if(photoMode||frozen)resumeScan();else finish();}});
        if(state!=null && state.containsKey("selected")) {
            String restore=state.getString("selected", "");
            if(!restore.isBlank()) {
                items=scanState.items.isEmpty()?List.of(new ScanItem(restore,new Rect())):new ArrayList<>(scanState.items);
                selected=Math.min(scanState.selected,items.size()-1);selectedText=restore;frozen=true;photoMode=true;
                imageWidth=scanState.width;imageHeight=scanState.height;photoView.setImageBitmap(scanState.photo);photoView.setVisibility(View.VISIBLE);
                zoomControls.setVisibility(View.GONE);flashButton.setVisibility(View.GONE);overlay.update(items,imageWidth,imageHeight,selected);
                setFreezeLabel("다시 읽기");guide.setText(scanState.photo==null?"이전에 읽은 결과 · 다시 읽기를 눌러 주세요":"궁금한 글자를 누르면 뜻이 바뀌어요");renderResults();
            }
        }
        if(state==null&&Intent.ACTION_SEND.equals(getIntent().getAction()))handleShare(getIntent());
        else if(!photoMode&&!frozen)requestCameraIfNeeded(false);
        watchPreparation();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("kern");return v;}
    private GradientDrawable bg(int color,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private ImageButton icon(int drawable,String label,Runnable action){ImageButton b=new ImageButton(this);b.setImageResource(drawable);b.setImageTintList(ColorStateList.valueOf(TEXT));b.setContentDescription(label);b.setBackground(bg(PANEL,16));pad(b,12,12,12,12);b.setOnClickListener(v->action.run());return b;}
    private void gap(LinearLayout l,int size){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    private void buildScreen() {
        root=column();root.setBackgroundColor(BG);setContentView(root);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());root.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        LinearLayout header=row();pad(header,20,10,16,12);
        LinearLayout brand=column();TextView title=text("바로봄",24,TEXT);title.setTypeface(null,Typeface.BOLD);brand.addView(title);
        TextView sub=text("일본어 → 한국어 · 메뉴와 제품명",11,MUTED);brand.addView(sub);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton saved=icon(R.drawable.ic_bookmark,"저장한 단어",this::showFavorites);header.addView(saved,new LinearLayout.LayoutParams(dp(48),dp(48)));
        ImageButton settings=icon(R.drawable.ic_settings,"사용 안내와 설정",this::showInfo);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(48),dp(48));ip.leftMargin=dp(8);header.addView(settings,ip);root.addView(header);
        body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        viewfinder=new FrameLayout(this);viewfinder.setBackgroundColor(0xFF08110D);
        body.addView(viewfinder,new LinearLayout.LayoutParams(-1,0,.46f));
        preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);preview.setScaleType(PreviewView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams cameraArea=new FrameLayout.LayoutParams(-1,-1);cameraArea.bottomMargin=dp(32);viewfinder.addView(preview,cameraArea);
        photoView=new ImageView(this);photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);photoView.setBackgroundColor(0xFF08110D);photoView.setVisibility(View.GONE);FrameLayout.LayoutParams photoArea=new FrameLayout.LayoutParams(-1,-1);photoArea.bottomMargin=dp(32);viewfinder.addView(photoView,photoArea);
        overlay=new ScanOverlay(this);overlay.listener=this::selectItem;FrameLayout.LayoutParams overlayArea=new FrameLayout.LayoutParams(-1,-1);overlayArea.bottomMargin=dp(32);viewfinder.addView(overlay,overlayArea);

        flashButton=icon(R.drawable.ic_flashlight,"손전등 켜기",this::toggleTorch);FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.END);fp.setMargins(0,dp(8),dp(12),0);viewfinder.addView(flashButton,fp);
        LinearLayout zoom=row();zoomControls=zoom;zoom.setBackground(bg(0xDB101A17,24));
        for(int z:new int[]{1,2,3}){TextView b=text(z+"×",13,TEXT);b.setGravity(Gravity.CENTER);b.setContentDescription(z+"배 확대");b.setOnClickListener(v->{if(camera!=null)camera.getCameraControl().setZoomRatio(Math.min(z,camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio()));});zoom.addView(b,new LinearLayout.LayoutParams(dp(48),dp(48)));}
        FrameLayout.LayoutParams zp=new FrameLayout.LayoutParams(-2,dp(48),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);zp.bottomMargin=dp(38);viewfinder.addView(zoom,zp);
        guide=text("메뉴나 제품명을 가운데에 비춰 주세요",12,TEXT);guide.setGravity(Gravity.CENTER);guide.setBackgroundColor(0xD9101A17);FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(-1,dp(32),Gravity.BOTTOM);viewfinder.addView(guide,gp);
        permissionPanel=permissionView();viewfinder.addView(permissionPanel,new FrameLayout.LayoutParams(-1,-1));permissionPanel.setVisibility(View.GONE);
        bottom=column();pad(bottom,20,14,20,0);body.addView(bottom,new LinearLayout.LayoutParams(-1,0,.54f));
        LinearLayout status=row();stateText=text("비추면 바로 읽어요",12,GREEN);stateText.setTypeface(null,Typeface.BOLD);status.addView(stateText,new LinearLayout.LayoutParams(0,-2,1));TextView offline=text("●  오프라인",11,MUTED);status.addView(offline);bottom.addView(status);
        engineStatus=text("",12,MUTED);engineStatus.setVisibility(View.GONE);engineStatus.setOnClickListener(v->{if(translator.failed){translator.prepare();watchPreparation();if(!selectedText.isBlank())scheduleTranslation(0);}});bottom.addView(engineStatus);gap(bottom,10);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);results=column();scroll.addView(results);bottom.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout toolbar=row();pad(toolbar,0,8,0,12);
        toolbar.addView(actionButton(R.drawable.ic_image,"사진",this::pickImage),new LinearLayout.LayoutParams(0,toolbarHeight(),1));
        View freeze=actionButton(R.drawable.ic_pause,"결과 고정",this::toggleFreeze);freezeText=(TextView)((LinearLayout)freeze).getChildAt(1);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,toolbarHeight(),1.3f);mp.setMargins(dp(8),0,dp(8),0);toolbar.addView(freeze,mp);
        toolbar.addView(actionButton(R.drawable.ic_bookmark,"저장",this::saveSelected),new LinearLayout.LayoutParams(0,toolbarHeight(),1));bottom.addView(toolbar);
        adaptLayout();renderResults();
    }
    private void adaptLayout(){
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        viewfinder.setLayoutParams(new LinearLayout.LayoutParams(wide?0:-1,wide?-1:0,.46f));
        bottom.setLayoutParams(new LinearLayout.LayoutParams(wide?0:-1,wide?-1:0,.54f));
    }
    @Override public void onConfigurationChanged(@NonNull Configuration c){super.onConfigurationChanged(c);adaptLayout();if(!photoMode&&!frozen){stopCamera();startCamera();}}
    private void watchPreparation(){
        if(destroyed)return;
        engineStatus.setVisibility(translator.ready?View.GONE:View.VISIBLE);
        engineStatus.setText(translator.failed?translator.failureMessage:"첫 AI 준비 "+translator.preparationPercent+"% · 기본 메뉴는 바로 읽어요");
        engineStatus.setMinHeight(translator.failed?dp(48):0);
        if(!translator.ready&&!translator.failed)main.postDelayed(this::watchPreparation,400);
    }
    private void stopCamera(){stopTorch();if(cameraProvider!=null)cameraProvider.unbindAll();camera=null;}
    private AlertDialog showDialog(AlertDialog dialog){
        modalDepth++;cancelTranslation();stopCamera();
        dialog.setOnDismissListener(d->{modalDepth=Math.max(0,modalDepth-1);if(modalDepth==0&&active&&!destroyed){if(!photoMode&&!frozen)startCamera();if(!selectedText.isBlank())scheduleTranslation(0);}});
        dialog.show();return dialog;
    }
    private void setFreezeLabel(String label){freezeText.setText(label);((View)freezeText.getParent()).setContentDescription(label);}
    private void stopTorch(){if(camera!=null&&torch)camera.getCameraControl().enableTorch(false);torch=false;if(flashButton!=null){flashButton.setImageTintList(ColorStateList.valueOf(TEXT));flashButton.setContentDescription("손전등 켜기");}}
    private int toolbarHeight(){return dp(60+Math.max(0,getResources().getConfiguration().fontScale-1)*36);}
    private View actionButton(int icon,String label,Runnable action){LinearLayout b=row();b.setGravity(Gravity.CENTER);b.setBackground(bg(PANEL,16));b.setContentDescription(label);b.setFocusable(true);b.setClickable(true);ImageView i=new ImageView(this);i.setImageResource(icon);i.setImageTintList(ColorStateList.valueOf(TEXT));b.addView(i,new LinearLayout.LayoutParams(dp(20),dp(20)));TextView t=text(label,13,TEXT);pad(t,8,0,0,0);b.addView(t);b.setOnClickListener(v->action.run());return b;}
    private View permissionView(){LinearLayout p=column();p.setGravity(Gravity.CENTER);p.setBackgroundColor(BG);pad(p,30,12,30,12);TextView t=text("카메라로 바로 읽기",21,TEXT);t.setTypeface(null,Typeface.BOLD);p.addView(t);TextView d=text("카메라 접근을 허용해 주세요.\n사진을 선택해서 읽을 수도 있어요.",14,MUTED);d.setGravity(Gravity.CENTER);pad(d,0,12,0,18);p.addView(d);TextView b=text("카메라 허용",15,BG);b.setGravity(Gravity.CENTER);b.setBackground(bg(GREEN,14));b.setOnClickListener(v->requestCameraIfNeeded(true));p.addView(b,new LinearLayout.LayoutParams(dp(200),dp(52)));return p;}
    private void requestCameraIfNeeded(boolean explicit){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){permissionPanel.setVisibility(View.GONE);startCamera();return;}
        permissionPanel.setVisibility(View.VISIBLE);
        boolean asked=getPreferences(MODE_PRIVATE).getBoolean("camera_asked",false);
        if(!asked||(explicit&&shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))){
            getPreferences(MODE_PRIVATE).edit().putBoolean("camera_asked",true).apply();requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
        }else if(explicit){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    }
    @Override public void onRequestPermissionsResult(int code,@NonNull String[] permissions,@NonNull int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==CAMERA_PERMISSION && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED){permissionPanel.setVisibility(View.GONE);startCamera();}}
    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void startCamera(){
        if(photoMode||frozen||destroyed||!active||modalDepth>0||cameraStarting||camera!=null)return;
        cameraStarting=true;
        ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
        f.addListener(()->{
            try{
                if(destroyed||photoMode||frozen||!active||modalDepth>0)return;
                cameraProvider=f.get();cameraProvider.unbindAll();
                if(!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)){guide.setText("카메라를 사용할 수 없어요 · 사진을 선택해 주세요");return;}
                Preview p=new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();p.setSurfaceProvider(preview.getSurfaceProvider());
                ImageAnalysis a=new ImageAnalysis.Builder().setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setTargetResolution(new Size(1280,960)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                a.setAnalyzer(analysisExecutor,proxy->{
                    long now=SystemClock.elapsedRealtime();
                    if(!active||modalDepth>0||photoMode||frozen||ocrBusy||now-lastFrame<180){proxy.close();return;}
                    android.media.Image media=proxy.getImage();if(media==null){proxy.close();return;}
                    ocrBusy=true;lastFrame=now;int epoch=inputEpoch;int rotation=proxy.getImageInfo().getRotationDegrees();
                    int w=(rotation==90||rotation==270)?proxy.getHeight():proxy.getWidth();int h=(rotation==90||rotation==270)?proxy.getWidth():proxy.getHeight();
                    recognizer.process(InputImage.fromMediaImage(media,rotation)).addOnSuccessListener(result->{if(active&&modalDepth==0&&!frozen&&!photoMode&&epoch==inputEpoch)acceptText(result,w,h,SystemClock.elapsedRealtime()-now);}).addOnFailureListener(e->{if(active)stateText.setText("글자를 읽지 못했어요 · 다시 비춰 주세요");}).addOnCompleteListener(task->{proxy.close();ocrBusy=false;});
                });
                camera=cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,a);
                flashButton.setVisibility(camera.getCameraInfo().hasFlashUnit()?View.VISIBLE:View.GONE);
            }catch(Exception e){camera=null;guide.setText("카메라를 열지 못했어요 · 사진을 선택해 주세요");}
            finally {cameraStarting=false;}
        },ContextCompat.getMainExecutor(this));
    }
    private void acceptText(Text result,int w,int h,long elapsed){
        List<ScanItem> found=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()){
            String s=line.getText().trim();Rect rect=line.getBoundingBox();
            if(rect==null||s.isBlank()||s.length()>220||!s.matches("(?s).*[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}].*"))continue;
            if(seen.add(Lexicon.normalize(s)))found.add(new ScanItem(s,rect));
        }
        found.sort(Comparator.comparingDouble(i->Math.abs(i.bounds.exactCenterY()/h-.5f)+Math.abs(i.bounds.exactCenterX()/w-.5f)*.25f));
        if(found.size()>10)found=new ArrayList<>(found.subList(0,10));
        if(found.isEmpty()){
            if(!items.isEmpty()&&SystemClock.elapsedRealtime()-lastNonEmpty>1500){selection.clear();items=new ArrayList<>();signature="";selectedText="";cancelTranslation();overlay.update(items,w,h,0);renderResults();}
            return;
        }
        lastNonEmpty=SystemClock.elapsedRealtime();
        List<String> keys=new ArrayList<>();List<Float> distances=new ArrayList<>();
        for(ScanItem i:found){keys.add(Lexicon.normalize(i.text));distances.add(Math.abs(i.bounds.exactCenterY()/h-.5f)+Math.abs(i.bounds.exactCenterX()/w-.5f)*.25f);}
        int choice=photoMode?Math.min(selected,found.size()-1):selection.choose(keys,distances,lastNonEmpty);
        if(choice<0)return;
        imageWidth=w;imageHeight=h;ocrMs=elapsed;
        String sig=String.join("\n",keys);
        String next=found.get(choice).text;
        boolean changed=!Lexicon.normalize(next).equals(Lexicon.normalize(selectedText));
        items=found;selected=choice;overlay.update(items,w,h,selected);
        if(sig.equals(signature)&&!changed)return;
        signature=sig;
        if(changed||selectedText.isEmpty()){selectedText=next;aiText="";aiFor="";aiErrorFor="";scheduleTranslation(0);}
        renderResults();
    }
    private long lastNonEmpty=0;
    private void selectItem(int index){if(index<0||index>=items.size())return;selected=index;selectedText=items.get(index).text;selection.select(Lexicon.normalize(selectedText));if(!photoMode&&!frozen)holdScan();aiText="";aiFor="";aiErrorFor="";overlay.update(items,imageWidth,imageHeight,index);scheduleTranslation(0);renderResults();}
    private void cancelTranslation(){if(translateTask!=null)main.removeCallbacks(translateTask);translator.cancel();}
    private void scheduleTranslation(long delay){
        cancelTranslation();String source=selectedText;
        if(source.isBlank()||lexicon.exact(source)!=null)return;
        String cached=translator.cached(source);if(cached!=null){aiText=cached;aiFor=source;aiMs=0;return;}
        translateTask=()->{
            if(!active||modalDepth>0||destroyed||!source.equals(selectedText))return;
            translator.translate(source,(translation,ms)->{if(destroyed||!source.equals(selectedText))return;aiFor=source;aiText=translation;aiMs=ms;if(translation.isEmpty())aiErrorFor=source;renderResults();});
        };main.postDelayed(translateTask,delay);
    }
    private void renderResults(){
        results.removeAllViews();
        if(items.isEmpty()){
            if(photoLoading){stateText.setText("사진 읽는 중");TextView wait=text("글자를 찾고 있어요",24,TEXT);pad(wait,0,12,0,8);results.addView(wait);return;}
            stateText.setText(photoMode?"읽을 글자를 찾지 못했어요":"비추면 바로 읽어요");
            TextView title=text(photoMode?"더 가까운 사진으로\n다시 읽어볼까요":"이 메뉴,\n어떤 음식일까?",30,TEXT);title.setTypeface(null,Typeface.BOLD);pad(title,0,12,0,8);results.addView(title);
            TextView hint=text(photoMode?"밝고 선명한 사진을 선택해 주세요.":"촬영 버튼 없이 비추기만 하세요.\n메뉴·성분·제품 용도를 한국어로 읽어요.",14,MUTED);hint.setLineSpacing(dp(4),1);results.addView(hint);
            return;
        }
        Lexicon.Entry e=lexicon.exact(selectedText);String translated=e!=null?e.korean:(selectedText.equals(aiFor)?aiText:translator.cached(selectedText));
        boolean has=translated!=null&&!translated.isBlank();
        stateText.setText(e!=null?"기본 뜻":has?"AI 번역 · 원문도 확인해 주세요":aiErrorFor.equals(selectedText)?"번역을 마치지 못했어요":"글자 읽음 · 뜻 확인 중");
        TextView original=text(selectedText,16,MUTED);original.setTextIsSelectable(true);results.addView(original);gap(results,7);
        String quickHints=e==null?lexicon.hints(selectedText):"";
        TextView ko=text(has?translated:!quickHints.isBlank()?quickHints:(aiErrorFor.equals(selectedText)?"번역을 마치지 못했어요":"한국어 뜻을 읽고 있어요"),has?28:22,has||!quickHints.isBlank()?GREEN:TEXT);ko.setTypeface(null,Typeface.BOLD);ko.setTextIsSelectable(true);ko.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);results.addView(ko);
        if(e!=null){
            if(!e.reading.isBlank()){TextView pronunciation=text("읽는 법  "+e.reading,13,TEXT);pad(pronunciation,0,9,0,0);results.addView(pronunciation);}
            TextView detail=text(e.detail,14,MUTED);detail.setLineSpacing(dp(3),1);pad(detail,0,7,0,0);results.addView(detail);
        }else{
            String hints=quickHints;
            if(!hints.isBlank()){TextView hint=text(has?"참고 용어  "+hints:aiErrorFor.equals(selectedText)?"찾아낸 낱말이에요 · 전체 문장의 뜻은 확인하지 못했어요":"먼저 알아본 낱말이에요 · 문장 번역 중",13,MUTED);pad(hint,0,9,0,0);results.addView(hint);}
            if(!has){String msg=translator.failed?"기기에서 번역을 시작하지 못했어요. 앱을 다시 열어 주세요.":!translator.ready?"첫 사용 준비 중이에요. 기본 메뉴는 바로 읽을 수 있어요.":aiErrorFor.equals(selectedText)?"글자를 더 가까이 비추거나 다시 시도해 주세요.":"원문을 확인하면서 잠시 기다려 주세요.";TextView t=text(msg,12,MUTED);pad(t,0,9,0,0);results.addView(t);}
            if(aiErrorFor.equals(selectedText)){TextView retry=text("다시 번역",14,GREEN);retry.setGravity(Gravity.CENTER_VERTICAL);retry.setMinHeight(dp(48));retry.setOnClickListener(v->{aiErrorFor="";scheduleTranslation(0);renderResults();});results.addView(retry);}
        }
        if(items.size()>1){gap(results,20);TextView more=text("함께 읽은 글자 · 눌러서 뜻 보기",11,MUTED);results.addView(more);gap(results,8);
            for(int i=0;i<items.size();i++){if(i==selected)continue;ScanItem item=items.get(i);int choice=i;Lexicon.Entry match=lexicon.exact(item.text);LinearLayout card=column();pad(card,12,9,12,9);card.setBackground(bg(PANEL,12));card.setMinimumHeight(dp(56));TextView jt=text(item.text,12,MUTED);card.addView(jt);TextView kt=text(match!=null?match.korean:"눌러서 번역",15,TEXT);card.addView(kt);card.setOnClickListener(v->selectItem(choice));card.setFocusable(true);card.setContentDescription(item.text+", "+(match!=null?match.korean:"번역하기"));results.addView(card,new LinearLayout.LayoutParams(-1,-2));gap(results,6);}
        }
    }
    private void toggleTorch(){if(camera==null||!camera.getCameraInfo().hasFlashUnit())return;torch=!torch;camera.getCameraControl().enableTorch(torch);flashButton.setImageTintList(ColorStateList.valueOf(torch?GREEN:TEXT));flashButton.setContentDescription(torch?"손전등 끄기":"손전등 켜기");}
    private void toggleFreeze(){if(photoMode||frozen){resumeScan();return;}if(items.isEmpty()){toast("글자가 보이면 결과를 고정할 수 있어요");return;}holdScan();}
    private void holdScan(){
        frozen=true;inputEpoch++;Bitmap b=preview.getBitmap();scanState.photo=b;
        if(b!=null){photoView.setImageBitmap(b);photoView.setVisibility(View.VISIBLE);}
        stopCamera();zoomControls.setVisibility(View.GONE);flashButton.setVisibility(View.GONE);
        setFreezeLabel("다시 읽기");guide.setText("결과를 고정했어요 · 다른 글자도 눌러 보세요");
    }
    private void resumeScan(){inputEpoch++;selection.clear();selected=0;scanState.photo=null;scanState.items.clear();photoMode=false;photoLoading=false;frozen=false;zoomControls.setVisibility(View.VISIBLE);signature="";items=new ArrayList<>();selectedText="";photoView.setVisibility(View.GONE);photoView.setImageDrawable(null);setFreezeLabel("결과 고정");guide.setText("메뉴나 제품명을 가운데에 비춰 주세요");overlay.update(items,1,1,0);cancelTranslation();renderResults();requestCameraIfNeeded(false);}
    private void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){toast("사진 선택 앱이 없어요");}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==PICK&&result==RESULT_OK&&data!=null&&data.getData()!=null)loadPhoto(data.getData());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleShare(intent);}
    private void handleShare(Intent i){
        if(!Intent.ACTION_SEND.equals(i.getAction()))return;
        try {android.os.Parcelable value=i.getParcelableExtra(Intent.EXTRA_STREAM);if(value instanceof Uri){loadPhoto((Uri)value);return;}}
        catch(RuntimeException ignored){}
        toast("사진 앱에서 이미지를 선택해 주세요");requestCameraIfNeeded(false);
    }
    private void loadPhoto(Uri uri){
        if(!"content".equals(uri.getScheme())){toast("사진 앱에서 이미지를 선택해 주세요");return;}
        photoMode=true;photoLoading=true;frozen=true;zoomControls.setVisibility(View.GONE);flashButton.setVisibility(View.GONE);inputEpoch++;int epoch=inputEpoch;cancelTranslation();stopCamera();permissionPanel.setVisibility(View.GONE);setFreezeLabel("다시 읽기");guide.setText("사진에서 글자를 읽고 있어요");stateText.setText("사진 읽는 중");items=new ArrayList<>();selectedText="";signature="";overlay.update(items,1,1,0);photoView.setImageDrawable(null);photoView.setVisibility(View.VISIBLE);renderResults();
        photoExecutor.execute(()->{
            try {
                Bitmap bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,source)->{Size size=info.getSize();int largest=Math.max(size.getWidth(),size.getHeight());if(largest>2048)decoder.setTargetSize(Math.max(1,size.getWidth()*2048/largest),Math.max(1,size.getHeight()*2048/largest));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});
                main.post(()->{if(epoch!=inputEpoch||destroyed)return;processPhoto(bitmap);});
            }catch(Exception|OutOfMemoryError e){main.post(()->{if(destroyed||epoch!=inputEpoch)return;toast("사진을 열지 못했어요. 다른 사진을 선택해 주세요.");resumeScan();});}
        });
    }
    void processPhoto(Bitmap bitmap){
        photoMode=true;photoLoading=true;frozen=true;zoomControls.setVisibility(View.GONE);flashButton.setVisibility(View.GONE);inputEpoch++;int epoch=inputEpoch;cancelTranslation();stopCamera();permissionPanel.setVisibility(View.GONE);scanState.photo=bitmap;selected=0;photoView.setImageBitmap(bitmap);photoView.setVisibility(View.VISIBLE);setFreezeLabel("다시 읽기");items=new ArrayList<>();selectedText="";signature="";overlay.update(items,1,1,0);long start=SystemClock.elapsedRealtime();renderResults();
        recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(r->{if(!destroyed&&epoch==inputEpoch){photoLoading=false;acceptText(r,bitmap.getWidth(),bitmap.getHeight(),SystemClock.elapsedRealtime()-start);guide.setText(items.isEmpty()?"글자가 더 크게 보이는 사진을 선택해 주세요":"궁금한 글자를 누르면 뜻이 바뀌어요");renderResults();}}).addOnFailureListener(e->{if(!destroyed&&epoch==inputEpoch){photoLoading=false;guide.setText("사진을 읽지 못했어요 · 다른 사진을 선택해 주세요");renderResults();}});
    }
    private void saveSelected(){
        if(selectedText.isBlank()){toast("읽은 글자가 생기면 저장할 수 있어요");return;}
        Lexicon.Entry e=lexicon.exact(selectedText);String ko=e!=null?e.korean:translator.cached(selectedText);if(ko==null||ko.isBlank()){toast("번역이 끝나면 저장할 수 있어요");return;}
        try{JSONObject o=new JSONObject();o.put("ja",selectedText);o.put("ko",ko);o.put("reading",e==null?"":e.reading);o.put("detail",e==null?"AI 번역":e.detail);String data=o.toString();favorites.removeIf(s->{try{return new JSONObject(s).optString("ja").equals(selectedText);}catch(Exception ex){return true;}});favorites.add(data);while(favorites.size()>100)favorites.remove(favorites.iterator().next());persist();toast("여행 단어장에 저장했어요");}catch(JSONException ignored){}
    }
    private void persist(){getPreferences(MODE_PRIVATE).edit().putString("favorites_ordered",new JSONArray(favorites).toString()).remove("favorites").apply();}
    private void showFavorites(){
        LinearLayout content=column();pad(content,20,8,20,8);renderFavorites(content);
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog dialog=showDialog(new AlertDialog.Builder(this).setTitle("여행 단어장").setView(scroll).setPositiveButton("닫기",null).setNeutralButton("전체 삭제",null).create());
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->confirmClear(()->renderFavorites(content)));
    }
    private void renderFavorites(LinearLayout content){
        content.removeAllViews();
        if(favorites.isEmpty()){TextView t=text("아직 저장한 단어가 없어요.\n읽은 결과에서 ‘저장’을 눌러 보세요.",15,TEXT);pad(t,0,16,0,16);content.addView(t);return;}
        List<String> reverse=new ArrayList<>(favorites);Collections.reverse(reverse);
        for(String saved:reverse)try{
            JSONObject o=new JSONObject(saved);LinearLayout card=column();pad(card,12,8,12,12);card.setBackground(bg(PANEL,12));
            LinearLayout top=row();top.addView(text(o.optString("ja"),14,MUTED),new LinearLayout.LayoutParams(0,-2,1));
            ImageButton remove=icon(R.drawable.ic_trash_2,o.optString("ja")+" 삭제",()->showDialog(new AlertDialog.Builder(this).setMessage("이 단어를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->{favorites.remove(saved);persist();renderFavorites(content);}).create()));
            top.addView(remove,new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(top);
            card.addView(text(o.optString("ko"),21,GREEN));if(!o.optString("reading").isBlank())card.addView(text(o.optString("reading"),13,TEXT));card.addView(text(o.optString("detail"),13,MUTED));
            content.addView(card);gap(content,8);
        }catch(JSONException ignored){}
    }
    private void confirmClear(){confirmClear(()->{});}
    private void confirmClear(Runnable after){
        if(favorites.isEmpty()){toast("저장한 단어가 없어요");return;}
        showDialog(new AlertDialog.Builder(this).setMessage("저장한 단어를 모두 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("전체 삭제",(d,w)->{favorites.clear();persist();after.run();toast("저장한 단어를 삭제했어요");}).create());
    }
    private void showInfo(){
        String[] actions={"사용 방법", "이미지·저장 정보", "오픈소스 라이선스", "저장한 단어 모두 삭제"};
        showDialog(new AlertDialog.Builder(this).setTitle("바로봄 · 일본 여행").setItems(actions,(d,which)->{
            if(which==0)message("바로 읽는 방법","1. 메뉴나 제품명을 카메라에 비춰 주세요.\n2. 궁금한 글자를 누르면 화면이 고정되고 뜻을 읽을 수 있어요.\n3. 흔들리면 ‘결과 고정’을 눌러 보세요.\n\n작은 글자는 2×로 확대하거나 가까이 비춰 주세요. 사진과 스크린샷도 가져올 수 있어요.\n\n기본 메뉴는 즉시 뜻을 찾고, 처음 보는 표현은 기기 안에서 AI로 번역해요. AI 번역은 틀릴 수 있어요. 상품명만으로 성분이나 용도를 확정하지 않으며, 알레르기·약 복용 판단에 사용하지 마세요.");
            if(which==1)message("기기 안에서 처리해요","카메라 영상과 선택한 사진은 기기에서만 읽고, 앱이 원본을 저장하지 않아요. 인터넷 권한이 없어 이 앱에서 서버로 전송할 수 없어요.\n\n‘저장’을 누른 원문과 뜻만 앱 내부 단어장에 남아요. 단어장 전체 삭제 또는 앱 삭제로 지울 수 있어요. 자동 백업은 사용하지 않아요.\n\n글자 인식: Google ML Kit\nAI 번역: Tencent Hy-MT2\n단어장과 기본 뜻은 여행용 사전이에요.");
            if(which==2)showLicenses();if(which==3)confirmClear();
        }).create());
    }
    private void showLicenses(){try{StringBuilder b=new StringBuilder("ML Kit와 AndroidX의 고지는 아래 문서를 따릅니다.\nhttps://developers.google.com/ml-kit/terms\nhttps://source.android.com/docs/setup/about/licenses\n\n");for(String f:getAssets().list("licenses")){try(InputStream in=getAssets().open("licenses/"+f)){b.append(f).append("\n").append(readUtf8(in)).append("\n\n");}}message("라이선스",b.toString());}catch(IOException e){toast("라이선스를 열지 못했어요");}}
    private void message(String title,String body){TextView t=text(body,15,TEXT);t.setTextIsSelectable(true);pad(t,24,12,24,16);ScrollView scroll=new ScrollView(this);scroll.addView(t);showDialog(new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("닫기",null).create());}
    private String readUtf8(InputStream in) throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8"); }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onResume(){super.onResume();active=true;if(preview!=null&&!photoMode&&ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){permissionPanel.setVisibility(View.GONE);startCamera();}if(!selectedText.isBlank())scheduleTranslation(200);}
    @Override protected void onPause(){active=false;cancelTranslation();stopTorch();stopCamera();super.onPause();}
    @Override protected void onSaveInstanceState(@NonNull Bundle state){super.onSaveInstanceState(state);state.putString("selected",selectedText);scanState.items=new ArrayList<>(items);scanState.source=selectedText;scanState.selected=selected;scanState.width=imageWidth;scanState.height=imageHeight;scanState.held=frozen||photoMode;}
    @Override protected void onDestroy(){destroyed=true;active=false;inputEpoch++;main.removeCallbacksAndMessages(null);translator.cancel();if(cameraProvider!=null)cameraProvider.unbindAll();recognizer.close();analysisExecutor.shutdown();photoExecutor.shutdown();super.onDestroy();}
}
