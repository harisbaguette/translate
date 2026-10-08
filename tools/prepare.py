#!/usr/bin/env python3
"""고정된 추론 코드와 모델을 준비하고 공개 GGUF의 STQ 형식 번호를 맞춘다."""
import collections, hashlib, pathlib, struct, subprocess, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[1]
REV='1e411d8f5a1e23525fa3265dfb4bd76265465397'
MODEL_URL='https://huggingface.co/tencent/Hy-MT2-1.8B-1.25Bit-GGUF/resolve/9df5c82/Hy-MT2-1.8B-1.25Bit.gguf'
PATCHED_SHA='f1e0adf9183536e42589650348305176c570be9e588fe6955bcdef82d7f66a01'
ORIGINAL_SHA='cc497fe8f033b52b3b8b00a7669e9661435432f9d4cd43f7ed24400c01507a93'

def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
    return h.hexdigest()

def align_stq(path):
    changes=[]
    with path.open('r+b') as f:
        def number(fmt):return struct.unpack('<'+fmt,f.read(struct.calcsize(fmt)))[0]
        def string():return f.read(number('Q')).decode('utf-8')
        def skip(t):
            if t==8:f.seek(number('Q'),1)
            elif t==9:
                kind=number('I');n=number('Q')
                for _ in range(n):skip(kind)
            else:f.seek({0:1,1:1,2:2,3:2,4:4,5:4,6:4,7:1,10:8,11:8,12:8}[t],1)
        if f.read(4)!=b'GGUF' or number('I')!=3:raise ValueError('Unexpected GGUF')
        tensors=number('Q');metadata=number('Q')
        for _ in range(metadata):string();skip(number('I'))
        types=collections.Counter()
        for _ in range(tensors):
            name=string();n=number('I');f.seek(n*8,1);at=f.tell();kind=number('I');number('Q');types[kind]+=1
            if kind==42:changes.append(at)
        if changes:
            if len(changes)!=224:raise ValueError('Unexpected tensor count')
            if sha(path)!=ORIGINAL_SHA:raise ValueError('Source model SHA mismatch')
            for at in changes:f.seek(at);f.write(struct.pack('<I',43))
        elif types[43]!=224 or sha(path)!=PATCHED_SHA:raise ValueError('Patched model SHA mismatch')
    print('STQ tensor metadata:',len(changes),'updated; weights unchanged')
    return sha(path)

if __name__=='__main__':
    vendor=ROOT/'vendor/llama.cpp'
    if not (vendor/'.git').is_dir():
        vendor.mkdir(parents=True,exist_ok=True)
        subprocess.run(['git','init',str(vendor)],check=True)
        subprocess.run(['git','-C',str(vendor),'remote','add','origin','https://github.com/sjl623/llama.cpp.git'],check=True)
        subprocess.run(['git','-C',str(vendor),'fetch','--depth','1','origin',REV],check=True)
        subprocess.run(['git','-C',str(vendor),'checkout','--detach','FETCH_HEAD'],check=True)
    current=subprocess.check_output(['git','-C',str(vendor),'rev-parse','HEAD'],text=True).strip()
    if current!=REV:raise RuntimeError('Pinned llama.cpp revision differs; check out '+REV)
    model=ROOT/'app/src/main/assets/models/hy-mt2.gguf';model.parent.mkdir(parents=True,exist_ok=True)
    if not model.exists():urllib.request.urlretrieve(MODEL_URL,model)
    digest=align_stq(model)
    (ROOT/'tools/model.sha256').write_text(digest+'  app/src/main/assets/models/hy-mt2.gguf\n')
    print('Model SHA-256:',digest)
