#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <chrono>
#include <algorithm>
#include "llama.h"

static llama_model *model = nullptr;
static llama_context *ctx = nullptr;
static std::atomic<bool> cancelled{false};
static std::chrono::steady_clock::time_point deadline;
static bool abort_decode(void *) { return cancelled.load() || std::chrono::steady_clock::now() > deadline; }
static void quiet_log(ggml_log_level, const char *, void *) {}
static std::string utf8(JNIEnv *env,jstring s) {
    const char *v=env->GetStringUTFChars(s,nullptr); std::string r(v); env->ReleaseStringUTFChars(s,v); return r;
}
static jstring java_text(JNIEnv *env, const std::string &s) {
    jbyteArray b=env->NewByteArray(s.size()); env->SetByteArrayRegion(b,0,s.size(),reinterpret_cast<const jbyte *>(s.data()));
    jclass c=env->FindClass("java/lang/String"); jmethodID ctor=env->GetMethodID(c,"<init>","([BLjava/lang/String;)V");
    jstring enc=env->NewStringUTF("UTF-8"); jstring result=(jstring)env->NewObject(c,ctor,b,enc);
    env->DeleteLocalRef(b); env->DeleteLocalRef(c); env->DeleteLocalRef(enc); return result;
}
extern "C" JNIEXPORT jboolean JNICALL Java_kr_barobom_travel_NativeTranslator_nativeLoad(JNIEnv *env,jclass,jstring path,jint threads) {
    if(ctx) return true;
    llama_log_set(quiet_log,nullptr); llama_backend_init();
    auto mp=llama_model_default_params(); mp.n_gpu_layers=0; mp.use_mmap=true;
    model=llama_model_load_from_file(utf8(env,path).c_str(),mp); if(!model) return false;
    auto cp=llama_context_default_params(); cp.n_ctx=512; cp.n_batch=256; cp.n_ubatch=256;
    cp.n_threads=std::clamp((int)threads,2,4); cp.n_threads_batch=cp.n_threads; cp.no_perf=true;
    ctx=llama_init_from_model(model,cp); if(!ctx) {llama_model_free(model); model=nullptr; return false;}
    llama_set_abort_callback(ctx,abort_decode,nullptr); return true;
}
extern "C" JNIEXPORT void JNICALL Java_kr_barobom_travel_NativeTranslator_nativeCancel(JNIEnv *,jclass) {cancelled=true;}
extern "C" JNIEXPORT jstring JNICALL Java_kr_barobom_travel_NativeTranslator_nativeTranslate(JNIEnv *env,jclass,jstring input) {
    if(!ctx) return java_text(env,"");
    cancelled=false; deadline=std::chrono::steady_clock::now()+std::chrono::seconds(12);
    std::string source=utf8(env,input);
    std::string prompt="Translate the following Japanese text into Korean. Output only the Korean translation, without explanations:\n"+source;
    llama_chat_message message{"user",prompt.c_str()};
    const char *tmpl=llama_model_chat_template(model,nullptr);
    std::vector<char> formatted(4096); int n=llama_chat_apply_template(tmpl,&message,1,true,formatted.data(),formatted.size());
    if(n<0 || n>=(int)formatted.size()) return java_text(env,"");
    const llama_vocab *vocab=llama_model_get_vocab(model);
    std::vector<llama_token> tokens(512); int count=llama_tokenize(vocab,formatted.data(),n,tokens.data(),tokens.size(),true,true);
    if(count<1 || count>400) return java_text(env,"");
    llama_memory_clear(llama_get_memory(ctx),true);
    for(int offset=0;offset<count;offset+=256){int size=std::min(256,count-offset); if(llama_decode(ctx,llama_batch_get_one(tokens.data()+offset,size))!=0)return java_text(env,"");}
    llama_sampler *sampler=llama_sampler_init_greedy(); std::string out;
    for(int i=0;i<64 && !abort_decode(nullptr);++i){
        llama_token token=llama_sampler_sample(sampler,ctx,-1);
        if(llama_vocab_is_eog(vocab,token))break;
        char piece[512]; int size=llama_token_to_piece(vocab,token,piece,sizeof(piece),0,true);
        if(size>0 && size<(int)sizeof(piece))out.append(piece,size);
        if(llama_decode(ctx,llama_batch_get_one(&token,1))!=0)break;
    }
    llama_sampler_free(sampler);
    if(abort_decode(nullptr))return java_text(env,"");
    return java_text(env,out);
}
