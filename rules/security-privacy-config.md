# Security·config — 토큰·내부 API·비밀값 (paper-trading)
<!-- devkit:pointer
path: ^(backend/(trading-api|collector-api)/src/main/.+\.(kt|ya?ml)|frontend/trading-web/src/.+\.(ts|tsx))$
why: 토큰·비밀값 저장과 서비스 간 내부 API 를 다루는 코드
-->
<!-- devkit:scope narrows SC1, SC21, SC26 -->
<!-- devkit:form v2 -->

이 파일은 서비스 간 내부 API, 증권사 토큰, 비밀값을 다루는 이 레포 고유 기준을 정한다. 내부 경로로 인증을 우회하거나 토큰·비밀값이 새어 계좌 권한이 넘어가는 것을 막는다.

## 조항

<!-- gt:PTS1 -->
- `/api/internal/**` 서비스 간 API 는 외부에 노출하지 않고, 외부 API 와 다른 인증을 건다.
  Why: 서비스 간 호출은 호출자를 믿고 사용자 인증을 건너뛰므로, 밖에 노출되면 외부인이 그 우회로를 쓴다(사람 결정 2026-11-22). — enforce: review
<!-- gt:PTS2 -->
- 증권사(KIS·토스) 토큰과 그 밖의 인증 토큰은 어디에도 평문으로 저장하지 않는다(서버의 DB·Redis·파일, 브라우저의 `localStorage`·`sessionStorage` 모두). 클라이언트에 두는 토큰의 자리는 devkit SC21 도 함께 따르고, SC21 이 정한 `HttpOnly`·`Secure`·`SameSite` 쿠키 자리는 그대로 쓴다.
  Why: 토큰이 평문이면 DB·Redis 덤프나 로그 하나로 계좌 주문 권한이 넘어간다(사람 결정 2026-11-22). SC21 은 모바일 토큰과 웹 refresh 토큰의 자리만 정해, 서버 저장과 그 밖의 브라우저 토큰을 여기서 더한다. — enforce: review
<!-- gt:PTS3 -->
- 비밀값(KIS·토스 앱 키와 시크릿, DB 비밀번호 등)은 Kotlin 코드와 커밋하는 설정(`application.yaml` 포함)에 쓰지 않고 환경 변수로만 받는다.
  Why: 커밋한 비밀은 이력에 영원히 남아 교체 말고는 처방이 없다(devkit SC1). SC1 은 Python·JS·TS 파일과 `.env.example` 편집에만 지목돼 이 레포 Kotlin·yml 편집엔 닿지 않아 여기서 다시 건다(리뷰 지적 2026-11-22, 옛 README 개발 원칙 «비밀값은 환경 변수로만 다룹니다»). — enforce: review
