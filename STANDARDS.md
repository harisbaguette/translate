# 이 프로젝트가 지키는 표준

산출물: 모바일 앱.
국제 표준의 기준 규칙을 바탕으로 이 프로젝트에 맞게 고쳐 쓰는 규칙이다. 작업을 시작할 때마다 읽고 지켜서 만든다.

- '기준 규칙'은 project start(`readiness.py start`)를 다시 돌리면 갱신된다. 손으로 고치지 않는다.
- '이 프로젝트 맞춤'은 AI가 프로젝트 성격에 맞게 쓴다. 더한 규칙, 바꾼 규칙(원래 → 바꾼 것, 까닭), 이 표준을 지킬 계획을 적는다. 맞춤이 기준 규칙보다 앞선다.
- 법이 정한 의무(개인정보, 결제, 등급과 확률 표시, 아동 보호)는 맞춤으로 빼지 않는다.
- 규칙을 어기는 코드나 파일을 만들지 않고, 자동 검사는 시험 명령에 넣어 바뀔 때마다 돌린다.

<!-- standard:iso-25010 -->
## ISO/IEC 25010:2023 제품 품질 모델

소프트웨어 품질을 9가지 특성으로 나눈 국제 표준.

### 기준 규칙

- 기능 적합성, 성능 효율, 호환성, 상호작용 능력, 신뢰성, 보안, 유지보수성, 유연성, 안전 가운데 이 제품에 해당하는 특성의 목표를 요구사항에 적는다.
- 목표마다 확인 방법(시험, 측정, 실제 사용)을 정한다.

원문: `python3 ~/.agents/work-methods/readiness.py source iso-25010`

### 이 프로젝트 맞춤

<!-- custom:iso-25010 -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:iso-25010 -->
<!-- /standard:iso-25010 -->

<!-- standard:wcag -->
## WCAG 2.2 AA 접근성 (ISO/IEC 40500)

장애가 있는 사람도 쓸 수 있게 하는 국제 표준. 웹이 아닌 앱과 문서에는 WCAG2ICT로 적용한다.

### 기준 규칙

- 본문 글자와 배경의 대비는 4.5:1 이상, 큰 글자와 아이콘, 입력 칸 테두리는 3:1 이상으로 한다.
- 모든 기능을 키보드만으로 쓸 수 있게 하고, 포커스가 어디 있는지 보이게 한다.
- 의미 있는 이미지에는 대체 텍스트를, 입력 칸에는 보이는 이름표를 단다.
- 색만으로 정보를 전하지 않는다. 오류는 글로도 알린다.
- 누르는 대상은 24×24 CSS px 이상으로 하고, 200% 확대와 320 CSS px 폭에서도 내용이 잘리지 않게 한다.
- 1초에 3번 넘게 번쩍이는 화면을 만들지 않는다.
- 설치형 앱(데스크톱, 모바일)은 운영체제 접근성 API로 화면 요소의 이름, 역할, 상태를 알려 주고, 사용자가 운영체제에서 정한 글자 크기와 대비 설정을 따른다(EN 301 549 11장).
- 대상 국가·업종의 접근성 의무와 채택 표준을 구분한다. EU는 EAA·웹 접근성 지침의 적용 범위와 회원국법, 해당 조화표준의 관보 인용 여부를 확인한다. WCAG 2.2 AA 목표를 모든 법의 동일한 합격선으로 단정하지 않는다.

자동 검사: axe-core(@axe-core/playwright)로 주요 화면 검사; Lighthouse 접근성 점수의 실패 항목 확인

원문: `python3 ~/.agents/work-methods/readiness.py source wcag wcag2ict wcag-em apg en-301-549 accessibleeu-en301549`

### 이 프로젝트 맞춤

<!-- custom:wcag -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:wcag -->
<!-- /standard:wcag -->

<!-- standard:asvs -->
## OWASP ASVS 5.0 레벨 1과 Top 10:2025

웹과 서버 보안의 국제 검증 기준.

### 기준 규칙

- 권한은 화면이 아니라 서버에서 요청마다 확인한다.
- 외부 입력은 서버에서 검증하고, 화면과 쿼리에 넣을 때 인코딩하거나 매개변수로 넘긴다.
- 채택한 비밀번호 기준을 명시한다. 기본 정책은 NIST SP 800-63B-4를 참고해 단독 인증 최소 15자, 항상 MFA와 함께 쓰면 최소 8자로 두며, 유출 비밀번호를 차단하고 Argon2id 등 적절한 해시·비용으로 저장한다. bcrypt는 기존 시스템의 제한과 이전 계획을 확인한다.
- 비밀 키와 토큰은 코드와 저장소에 넣지 않고 환경 변수나 비밀 저장소에 둔다.
- 세션은 로그아웃과 만료 때 서버에서 끊는다.
- 의존성은 잠금 파일로 고정하고 알려진 취약점을 검사한다.
- 파일 올리기를 받으면 크기 상한을 두고, 확장자와 실제 내용(매직 바이트)이 맞는지 확인하며, 저장 파일 이름은 서버가 새로 만든다.
- 올린 파일은 서버 코드로 실행되지 않는 곳에 두고, 이미지는 다시 인코딩해 위치(GPS), 기기 정보 같은 Exif 메타데이터를 지운다.

자동 검사: gitleaks로 비밀 노출 검사; osv-scanner나 npm audit, pip-audit로 의존성 취약점 검사; OWASP ZAP baseline 검사(공개 서비스)

원문: `python3 ~/.agents/work-methods/readiness.py source asvs owasp-top10-2025 nist-800-63b owasp-auth-tests cipa-exif owasp-password-storage`

### 이 프로젝트 맞춤

<!-- custom:asvs -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:asvs -->
<!-- /standard:asvs -->

<!-- standard:masvs -->
## OWASP MASVS 2.1

모바일 앱 보안의 국제 기준.

### 기준 규칙

- 토큰과 개인정보는 Keychain이나 Android Keystore 같은 안전한 저장소에 둔다.
- 통신은 HTTPS만 허용한다.
- 로그와 크래시 보고에 개인정보와 토큰을 남기지 않는다.
- 권한은 기능을 쓰는 순간에만 묻는다.

자동 검사: MobSF 정적 분석

원문: `python3 ~/.agents/work-methods/readiness.py source masvs`

### 이 프로젝트 맞춤

<!-- custom:masvs -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:masvs -->
<!-- /standard:masvs -->

<!-- standard:data-formats -->
## 데이터 형식 표준 (UTF-8, JSON, ISO 8601/RFC 3339)

문자, 날짜, 데이터 교환의 기본 표준.

### 기준 규칙

- 글자는 UTF-8로 저장하고 주고받는다.
- 날짜와 시간은 RFC 3339(ISO 8601) 형식과 시간대를 함께 저장한다. 화면에는 사용자 지역 형식으로 보인다.
- 데이터 교환은 JSON(RFC 8259)이나 표준 형식을 쓰고, 금액은 정수 최소 단위와 통화 코드(ISO 4217)로 다룬다.
- 미래 일정과 반복 일정은 UTC 오프셋과 함께 IANA 시간대 이름(Asia/Seoul)을 저장한다. 문자열로 주고받을 때는 RFC 9557 형식(2026-10-08T09:00:00+09:00[Asia/Seoul])을 쓴다.
- 나라는 ISO 3166-1 두 글자 코드(KR), 전화번호는 E.164 국제 형식(+82로 시작)으로 저장한다.
- 사용자가 입력한 글자는 유니코드 NFC로 정규화해 저장하고 비교한다. 그래야 자모가 풀린 한글과 합쳐진 한글이 다른 글자로 취급되지 않는다.

원문: `python3 ~/.agents/work-methods/readiness.py source rfc3339 rfc8259 rfc9557 iana-tz iso-3166 itu-e164 w3c-charmod-norm`

### 이 프로젝트 맞춤

<!-- custom:data-formats -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:data-formats -->
<!-- /standard:data-formats -->

<!-- standard:supply-chain -->
## 공급망 보안 (NIST SSDF, SLSA, SBOM) — 배포물을 다른 사람에게 줄 때

배포물이 무엇으로 만들어졌는지 밝히고 안전하게 빌드하는 기준.

### 기준 규칙

- 배포물은 개인 PC가 아니라 CI에서 같은 방법으로 빌드한다.
- 배포물마다 SBOM(CycloneDX나 SPDX)을 만든다.
- 배포 계정은 2단계 인증을 켜고, 배포 토큰은 범위와 기한을 좁힌다.
- 취약점 신고를 받는 곳과 처리 절차(접수 확인, 수정, 공개)를 SECURITY.md에 적고, 고친 취약점은 보안 공지로 알린다(ISO/IEC 29147, 30111).

자동 검사: syft나 cdxgen으로 SBOM 생성; OpenSSF Scorecard(공개 저장소)

원문: `python3 ~/.agents/work-methods/readiness.py source ssdf slsa cyclonedx spdx scorecard iso-29147 cra`

### 이 프로젝트 맞춤

<!-- custom:supply-chain -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:supply-chain -->
<!-- /standard:supply-chain -->

<!-- standard:licensing -->
## 라이선스 표기 (SPDX, REUSE)

코드, 글꼴, 그림, 소리를 쓸 권리를 밝히는 표준.

### 기준 규칙

- 가져다 쓴 코드와 에셋의 라이선스를 확인하고 출처를 ASSETS.md나 고지 파일에 남긴다.
- 내 프로젝트의 라이선스를 SPDX 식별자로 LICENSE에 밝힌다.
- 상업 이용, 변형, 재배포 조건이 맞지 않는 에셋은 쓰지 않는다.

자동 검사: reuse lint; license-checker나 pip-licenses로 의존성 라이선스 목록 만들기

원문: `python3 ~/.agents/work-methods/readiness.py source spdx-licenses reuse cc`

### 이 프로젝트 맞춤

<!-- custom:licensing -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:licensing -->
<!-- /standard:licensing -->

<!-- standard:graphics -->
## 그래픽 형식과 색 (SVG, PNG, sRGB, 인쇄는 PDF/X)

로고, 아이콘, 그림 파일을 어디서나 같게 보이게 하는 표준.

### 기준 규칙

- 로고와 아이콘은 SVG 원본을 두고, 필요한 크기의 PNG를 함께 내보낸다.
- 화면용 색과 이미지는 sRGB로 만든다. 인쇄물은 인쇄소가 요구하는 PDF/X 형식과 CMYK 사양으로 낸다.
- 브랜드 색은 HEX와 RGB 값으로 적어 디자인과 코드가 같은 값을 쓰게 한다.
- 공개하는 사진에서는 위치(GPS), 기기 일련번호 같은 Exif 정보를 지운다. C2PA 내용 증명은 남긴다.

자동 검사: SVGO로 SVG 정리; ImageMagick identify로 색 공간과 크기 확인

원문: `python3 ~/.agents/work-methods/readiness.py source svg2 png3 srgb cipa-exif`

### 이 프로젝트 맞춤

<!-- custom:graphics -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:graphics -->
<!-- /standard:graphics -->

<!-- standard:mobile-platform -->
## 모바일 플랫폼 기준 (Apple HIG와 App Review Guidelines, Android Core App Quality와 Material 3)

스토어에 올리려면 맞춰야 하는 플랫폼 기준.

### 기준 규칙

- 플랫폼의 뒤로 가기, 탭, 시트 같은 화면 관례를 따른다.
- 누르는 영역은 iOS 44×44pt, Android 48×48dp 이상으로 한다.
- 앱 개인정보 라벨과 데이터 보안 양식을 실제 수집 내용과 맞춘다.
- 스토어의 최신 타깃 SDK와 빌드 도구 요구를 맞춘다.

자동 검사: Android Accessibility Scanner; Xcode Accessibility Inspector

원문: `python3 ~/.agents/work-methods/readiness.py source apple-hig apple-review android material3 play-target-api apple-privacy`

### 이 프로젝트 맞춤

<!-- custom:mobile-platform -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:mobile-platform -->
<!-- /standard:mobile-platform -->

<!-- standard:privacy -->
## 개인정보 보호 (한국 개인정보보호법, GDPR) — 개인정보를 모을 때(문의 폼, 회원, 분석 도구, 광고, 댓글 포함)

개인정보를 모을 때 지키는 법.

### 기준 규칙

- 꼭 필요한 정보만 모은다. 문의 폼도 답장에 필요한 칸만 둔다.
- 무엇을 왜 얼마나 보관하는지 처리방침에 적고, 모든 페이지에서 처리방침으로 갈 수 있게 한다.
- 분석 도구, 광고, 필수가 아닌 쿠키는 대상 지역 규칙에 맞게 미리 알리고, 동의가 필요한 곳에서는 동의 전에 켜지 않는다.
- 한국에서 동의가 필요한 만 14세 미만 개인정보 처리는 법정대리인의 동의와 확인을 거친다. 미국 COPPA와 EU 회원국의 아동 기준은 적용 범위를 따로 확인한다.
- 사용자가 자기 정보를 보고 지우고 탈퇴할 수 있게 한다.
- 적용되는 다른 법은 regulations.md의 판별 질문으로 가린다.
- EEA, 영국, 스위스 사용자에게 Google AdSense, Ad Manager, AdMob 맞춤 광고를 띄우면 Google 인증을 받은 CMP(IAB TCF 연동)로 동의를 받는다.

원문: `python3 ~/.agents/work-methods/readiness.py source kr-privacy gdpr-scope cookies google-cmp kr-child-consent gdpr-official us-coppa`

### 이 프로젝트 맞춤

<!-- custom:privacy -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:privacy -->
<!-- /standard:privacy -->

<!-- standard:qr-code -->
## QR 코드 (ISO/IEC 18004:2024) — 인쇄물이나 화면에 QR 코드를 넣을 때

인쇄물과 화면의 QR 코드가 잘 읽히게 하는 국제 표준.

### 기준 규칙

- QR 코드 둘레에 4모듈 폭 이상의 빈 여백(quiet zone)을 둔다.
- 오류 정정 수준은 M 이상으로 만들고, 짙은 점을 밝은 바탕에 둔다.
- 실제 인쇄 크기로 시험 인쇄해 여러 휴대폰 카메라로 읽히는지, 넣은 주소가 HTTPS로 바로 열리는지 확인한다.

자동 검사: zbarimg로 만든 이미지 읽기 확인

원문: `python3 ~/.agents/work-methods/readiness.py source iso-18004 denso-qr-margin`

### 이 프로젝트 맞춤

<!-- custom:qr-code -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:qr-code -->
<!-- /standard:qr-code -->

<!-- standard:plain-language -->
## 쉬운 글 (ISO 24495-1:2023)

읽는 사람이 필요한 내용을 찾고 이해하고 쓸 수 있게 글을 쓰는 국제 표준.

### 기준 규칙

- 쓰기 전에 읽는 사람, 읽는 목적, 읽는 상황을 정하고 거기에 필요한 내용만 넣는다.
- 제목, 목차, 목록으로 필요한 내용을 쉽게 찾게 짠다.
- 읽는 사람이 아는 낱말과 짧고 분명한 문장을 쓰고, 전문 용어는 처음 나올 때 풀어 쓴다.
- 대상 독자 몇 명에게 읽혀 보고 찾고 이해하고 쓸 수 있는지 확인한 뒤 고친다.

원문: `python3 ~/.agents/work-methods/readiness.py source iso-24495-1 iplf-iso-standard google-writing`

### 이 프로젝트 맞춤

<!-- custom:plain-language -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:plain-language -->
<!-- /standard:plain-language -->

<!-- standard:children -->
## 아동 대상 설계 (UK Age Appropriate Design Code, IEEE 2089-2021) — 아동이 쓰거나 아동이 쓸 가능성이 큰 서비스일 때

아이가 쓰는 서비스의 개인정보와 화면 설계 기준.

### 기준 규칙

- 개인정보 설정은 처음부터 가장 보호되는 쪽으로 둔다.
- 위치 정보와 프로파일링(맞춤 추천, 맞춤 광고)은 기본으로 꺼 둔다.
- 아이가 정보를 더 내거나 보호 설정을 낮추도록 유도하는 화면을 쓰지 않는다.
- 꼭 필요한 정보만 모아 보관하고, 아이가 읽는 약관과 안내는 나이에 맞는 말로 쓴다.

원문: `python3 ~/.agents/work-methods/readiness.py source ico-childrens-code ieee-2089 kr-child-privacy-guide`

### 이 프로젝트 맞춤

<!-- custom:children -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:children -->
<!-- /standard:children -->

<!-- standard:risk-prevention -->
## 법적·보안 위험 예방 (한국 기본, 해외 조건별)

가입·수집·추적·발송·결제·업로드에서 처음부터 예방할 위험.

### 기준 규칙

- 한국을 기본 검토 지역으로 삼고 사업 거점·제공 대상·데이터 흐름에 따라 해외 법을 추가한다. 한국 법만 적용된다는 뜻이 아니다. 적용 범위가 미정이면 확인 필요로 둔다.
- 구현 전에 코드·외부 서비스 설정의 법적·보안 위험을 확인한다. 무료, 매출 0, 개인 개발, 혼자 쓰는 도구라는 이유로 일괄 면제하지 않는다.
- 아동의 연령·동의 요건, 개인정보 수집·국외 이전·삭제, 외부 폰트·SDK 호출과 세션 재생을 설계에서 점검한다. 세션 재생은 기본 끔, 폰트는 라이선스가 허용하면 자체 호스팅을 우선한다.
- 광고성 메시지의 동의·발신자·수신 거부와 구독의 요금·주기·갱신 동의·취소를 실제 동작으로 연결한다.
- 업로드 저작권 신고·게시 중단·이의 처리, 판매자 표시·환불과 개인정보 사고 대응을 적용 관할에 맞게 준비한다.
- 인증·권한·입력·파일·비밀키·의존성·결제 위조·남용을 해당 경로에서 시험하고 정상 동작도 확인한다.
- 주소·가격·사업자번호를 만들지 않는다. 모르는 사실, 외부 등록과 대시보드 미확인을 남기고 관련 작업은 확인 전 완료하지 않는다.
- 미성년자 계약, 개인정보 처리업체 계약·감독, 정보주체 권리와 광고 공유 거부, 접근성, AI·규제 제품·세금·정산 의무도 실제 기능·대상국에 맞게 확인한다.
- 공식 원문과 시행 상태는 착수·출시·범위 변경 때 확인한다. 검사 무검출이나 체크리스트 완료를 소송 방지·법적 준수·보안의 보장으로 쓰지 않는다.

원문: `python3 ~/.agents/work-methods/readiness.py source kr-child-consent kr-overseas-transfer kr-ecommerce-guidance kr-spam gdpr-official us-coppa us-can-spam us-ca-renewal us-dmca asvs`

### 이 프로젝트 맞춤

<!-- custom:risk-prevention -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:risk-prevention -->
<!-- /standard:risk-prevention -->

## 이 프로젝트만의 규칙과 계획

<!-- custom:project -->
Android 개인용 APK에 맞춰 적용한다. 서버·웹·스토어 제출 기준은 해당 구성 요소가 없으므로 적용 범위를 로컬 앱으로 좁힌다. 48dp 터치 영역, 시스템 글꼴 확대, 카메라 권한 거부 복구, 오프라인 처리, 입력 길이·이미지 크기 제한, 라이선스 동봉을 구현하고 정적 검사와 실제 에뮬레이터 실행으로 확인한다.
<!-- /custom:project -->

## 해당 없음으로 뺀 표준

<!-- custom:skipped -->
- OAuth 2.0 보안 모범 사례와 OpenID Connect (RFC 9700, RFC 7636 PKCE, OIDC Core = ISO/IEC 26131:2024): 로그인·권한 위임 기능 없음
- 글꼴 형식 (Open Font Format ISO/IEC 14496-22:2026 = OpenType, WOFF2): 운영체제 기본 글꼴 사용
<!-- /custom:skipped -->

## 다 됐는지 확인

계획 지도의 '완성 기준' 작업이 이 표준을 포함한 점검 목록이다. 모두 증거와 함께 끝나야 완성이다.
