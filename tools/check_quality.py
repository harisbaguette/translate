#!/usr/bin/env python3
"""배포 APK의 오프라인 처리, 모델, 네이티브 정렬과 필수 파일을 검사한다."""
import hashlib,json,pathlib,re,subprocess,sys,tempfile,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
apk=pathlib.Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'app/build/outputs/apk/release/app-release.apk'
sdk=pathlib.Path.home()/'Library/Android/sdk'
aapt=sdk/'build-tools/36.0.0/aapt'
permissions=subprocess.check_output([str(aapt),'dump','permissions',str(apk)],text=True)
for denied in ['INTERNET','ACCESS_NETWORK_STATE','RECORD_AUDIO','ACCESS_FINE_LOCATION','READ_EXTERNAL_STORAGE']:
 assert 'android.permission.'+denied not in permissions,denied
assert 'android.permission.CAMERA' in permissions
readelf=sdk/'ndk/28.2.13676358/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-readelf'
report={'apk':str(apk),'permissions':permissions.strip().splitlines(),'native':{},'checks':[]}
with zipfile.ZipFile(apk) as z:
 names=z.namelist()
 assert 'assets/lexicon.tsv' in names
 for name in ['Hy-MT2.txt','Lucide.txt','llama.cpp.txt','AndroidX-MLKit.txt']:
  assert 'assets/licenses/'+name in names
 expected=(ROOT/'tools/model.sha256').read_text().split()[0]
 h=hashlib.sha256()
 with z.open('assets/models/hy-mt2.gguf') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 assert h.hexdigest()==expected,'Packaged model SHA mismatch'
 with tempfile.TemporaryDirectory() as temp:
  for name in names:
   if name.startswith('lib/') and name.endswith('.so'):
    p=pathlib.Path(temp)/pathlib.Path(name).name;p.write_bytes(z.read(name))
    headers=subprocess.check_output([str(readelf),'-lW',str(p)],text=True)
    align=[int(line.split()[-1],16) for line in headers.splitlines() if line.strip().startswith('LOAD')]
    assert align and min(align)>=16384, f'{name}: {align}'
    report['native'][name]={'min_load_alignment':min(align)}
 report['checks']=['인터넷·음성·위치·광범위 저장소 권한 없음','모델 SHA-256 일치','사전·라이선스 동봉','전체 네이티브 라이브러리 16KB ELF 정렬']
 h=hashlib.sha256(apk.read_bytes());report['sha256']=h.hexdigest();report['bytes']=apk.stat().st_size
out=ROOT/'artifacts/verification/apk-check.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,ensure_ascii=False,indent=2));print(json.dumps(report,ensure_ascii=False,indent=2))
