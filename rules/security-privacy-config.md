# Security·config — 토큰·내부 API·비밀값 (paper-trading)
<!-- devkit:pointer
path: ^(backend/(trading-api|collector-api)/src/main/.+\.kt|frontend/trading-web/src/.+\.(ts|tsx))$
why: 토큰·비밀값 저장과 서비스 간 내부 API 를 다루는 코드
-->
<!-- devkit:scope narrows SC21, SC26 -->
<!-- devkit:form v2 -->

이 파일은 서비스 간 내부 API 와 증권사 토큰을 다루는 이 레포 고유 기준을 정한다. 내부 경로로 인증을 우회하거나 토큰이 새어 계좌 권한이 넘어가는 것을 막는다. 비밀값을 환경에서 받는 원칙은 devkit SC1 이 SSoT 다.

## 조항

<!-- gt:PTS1 -->
- `/api/internal/**` 서비스 간 API 는 외부에 노출하지 않고, 외부 API 와 다른 인증을 건다.
  Why: 서비스 간 호출은 호출자를 믿고 사용자 인증을 건너뛰므로, 밖에 노출되면 외부인이 그 우회로를 쓴다(사람 결정 2026-11-22). — enforce: review
<!-- gt:PTS2 -->
- 증권사(KIS·토스) 토큰과 그 밖의 인증 토큰은 평문으로 저장하지 않는다.
  Why: 토큰이 평문이면 DB·Redis 덤프나 로그 하나로 계좌 주문 권한이 넘어간다(사람 결정 2026-11-22). devkit SC21 은 클라이언트 저장만 다뤄 서버 저장을 여기서 좁힌다. — enforce: review
