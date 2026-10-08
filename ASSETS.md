# ASSETS.md — 에셋 장부 (이 프로젝트에 들어간 남의 저작물 전부)

| 파일/폴더 | 출처(URL) | 팩/작가 | 라이선스 | 크레딧 | 받은 날 |
|-----------|-----------|---------|----------|--------|---------|
| assets/icons/scan-line.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/scan-line.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/image.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/image.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/bookmark.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/bookmark.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/flashlight.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/flashlight.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/settings.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/settings.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/arrow-left.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/arrow-left.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/volume-2.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/volume-2.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/x.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/x.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/check.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/check.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/copy.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/copy.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/camera.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/camera.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/pause.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/pause.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/play.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/play.svg | lucide | ISC | 불필요 | 2026-10-08 |
| assets/icons/trash-2.svg | https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/trash-2.svg | lucide | ISC | 불필요 | 2026-10-08 |

| 실제 파일 | 공식 출처 | 사용 조건 |
|---|---|---|
| app/src/main/assets/models/hy-mt2.gguf | https://huggingface.co/tencent/Hy-MT2-1.8B-1.25Bit-GGUF | Apache-2.0, 모델 라이선스 앱에 동봉 |
| vendor/llama.cpp | https://github.com/sjl623/llama.cpp/tree/1e411d8f5a1e23525fa3265dfb4bd76265465397 | MIT, STQ1_0 지원 PR 브랜치 사용, 라이선스 동봉 |
| app/src/main/res/drawable/ic_*.xml | https://lucide.dev/license | Lucide ISC, 내려받은 SVG를 Android VectorDrawable로 변환 |
| app/src/main/assets/lexicon.tsv | 직접 작성한 일본 여행용 용어와 한국어 설명 | 프로젝트 자체 데이터. 음식 설명은 일반적 조리법이며 개별 매장 재료 보증 아님 |
| DeviceTest.menu() 합성 메뉴 | 직접 작성한 시험용 메뉴 | 실제 업소 메뉴가 아닌 시험 데이터 |

아이콘은 일본 제작 사이트 ICOOON MONO와 플랫폼용 세트를 비교했고, 단일한 선 두께·수정과 재배포 조건이 명확한 Lucide를 사용한다. 장식 이미지는 카메라 판독을 방해하므로 화면에 필요하지 않다.

Hy-MT2 GGUF는 공개 모델의 STQ1_0 식별자(42)를 고정된 llama.cpp PR의 식별자(43)에 맞췄다. 텐서 가중치는 그대로이고 224개 형식 필드만 변환한다. 원본 SHA-256: cc497fe8f033b52b3b8b00a7669e9661435432f9d4cd43f7ed24400c01507a93. 재현 도구와 배포 SHA는 tools/prepare.py 및 tools/model.sha256에 있다.
