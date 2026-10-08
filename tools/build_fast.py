#!/usr/bin/env python3
import pathlib,os,subprocess,shutil
root=pathlib.Path(__file__).resolve().parents[1]
sdk=pathlib.Path(os.environ.get('ANDROID_HOME',pathlib.Path.home()/'Library/Android/sdk'))
cmake=sdk/'cmake/3.22.1/bin/cmake';ninja=sdk/'cmake/3.22.1/bin/ninja'
build=root/'.native-fast'
args=[str(cmake),'-S',str(root/'app/src/main/cpp'),'-B',str(build),'-G','Ninja',f'-DCMAKE_MAKE_PROGRAM={ninja}',f'-DCMAKE_TOOLCHAIN_FILE={sdk}/ndk/28.2.13676358/build/cmake/android.toolchain.cmake','-DANDROID_ABI=arm64-v8a','-DANDROID_PLATFORM=android-28','-DANDROID_STL=c++_static','-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON','-DCMAKE_BUILD_TYPE=Release','-DBAROBOM_FAST=ON']
subprocess.run(args,check=True)
subprocess.run([str(cmake),'--build',str(build),'--target','barobom','-j','6'],check=True)
target=root/'app/src/main/jniLibs/arm64-v8a/libbarobom_fast.so';target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(build/'libbarobom_fast.so',target)
