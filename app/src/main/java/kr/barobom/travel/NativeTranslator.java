package kr.barobom.travel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class NativeTranslator {
    static {
        boolean dot = false;
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line; while ((line=r.readLine())!=null) if(line.startsWith("Features") && line.contains("asimddp")) { dot=true; break; }
        } catch(IOException ignored) {}
        System.loadLibrary(dot ? "barobom_fast" : "barobom");
    }
    private static NativeTranslator instance;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger generation = new AtomicInteger();
    private Lexicon glossary;
    private final LruCache<String, String> cache = new LruCache<>(160);
    public volatile boolean ready;
    public volatile boolean failed;
    public volatile int preparationPercent;
    public volatile long preparedAtElapsedMs;
    public volatile String failureMessage = "";
    private final Context appContext;
    private volatile boolean preparing;
    public interface Callback { void complete(String result, long elapsedMs); }
    public static synchronized NativeTranslator get(Context c) {
        if (instance == null) instance = new NativeTranslator(c.getApplicationContext());
        return instance;
    }
    private NativeTranslator(Context c) {
        appContext=c;
        try { glossary = new Lexicon(c.getAssets().open("lexicon.tsv")); } catch(IOException ignored) {}
        prepare();
    }
    public synchronized void prepare() {
        if(preparing || ready)return;
        preparing=true;failed=false;preparationPercent=0;failureMessage="";
        Context c=appContext;
        worker.execute(() -> {
            try {
                File model = new File(c.getNoBackupFilesDir(), "hy-mt2-stq43.gguf");
                long expected;
                try (android.content.res.AssetFileDescriptor fd = c.getAssets().openFd("models/hy-mt2.gguf")) { expected = fd.getLength(); }
                if (!model.isFile() || model.length() != expected) {
                    File part = new File(model.getPath() + ".part");
                    if(part.exists()&&!part.delete())throw new IOException("model cleanup");
                    if(c.getNoBackupFilesDir().getUsableSpace()<expected+32L*1024*1024) throw new IOException("storage");
                    try (InputStream in = c.getAssets().open("models/hy-mt2.gguf"); OutputStream out = new FileOutputStream(part)) {
                        byte[] buffer = new byte[1024 * 1024]; int n; long copied=0;
                        while ((n = in.read(buffer)) != -1) {out.write(buffer, 0, n);copied+=n;preparationPercent=(int)(copied*90/expected);}
                    }
                    if (part.length() != expected || !part.renameTo(model)) throw new IOException("model copy");
                }
                preparationPercent=95;
                ready = nativeLoad(model.getAbsolutePath(), Math.min(4, Runtime.getRuntime().availableProcessors()));
                failed = !ready;
                if(ready)preparedAtElapsedMs=android.os.SystemClock.elapsedRealtime();
                preparationPercent=ready?100:95;
                if(failed)failureMessage="AI를 준비하지 못했어요 · 눌러서 다시 시도";
            } catch (Exception | LinkageError e) { android.util.Log.e("BarobomModel", "Model initialization failed", e); failed = true;failureMessage="storage".equals(e.getMessage())?"저장 공간 약 500MB가 더 필요해요 · 확보 후 누르세요":"AI 준비 실패 · 눌러서 다시 시도"; }
            finally {preparing=false;}
        });
    }
    public void cancel() { generation.incrementAndGet(); nativeCancel(); }
    public String cached(String text) { synchronized (cache) { return cache.get(text); } }
    public void translate(String text, Callback callback) {
        cancel(); int id = generation.get();
        String hit = cached(text);
        if (hit != null) { callback.complete(hit, 0); return; }
        worker.execute(() -> {
            if (id != generation.get()) return;
            long start = android.os.SystemClock.elapsedRealtime();
            String result = "";
            if (ready && text.length() <= 220) {
                try {
                    Lexicon.ProtectedText protectedText=glossary==null?null:glossary.protect(text);
                    String translated=nativeTranslate(protectedText==null?text:protectedText.source, "").trim().replaceAll("[\\r\\n]+", " ");
                    result=protectedText==null?translated:protectedText.restore(translated);
                }
                catch (Exception | LinkageError ignored) { }
            }
            if (!result.matches("(?s).*[가-힣].*") || result.length() > 400 || result.contains("<|")) result = "";
            if (id != generation.get()) return;
            String finalResult = result;
            if (!result.isEmpty()) synchronized (cache) { cache.put(text, result); }
            main.post(() -> { if (id == generation.get()) callback.complete(finalResult, android.os.SystemClock.elapsedRealtime() - start); });
        });
    }
    static native boolean nativeLoad(String path, int threads);
    static native String nativeTranslate(String input, String glossary);
    static native void nativeCancel();
}
