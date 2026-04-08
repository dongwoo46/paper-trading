# AGENTS.md — Paper Trading 공통 규칙

> 루트 문서는 공통 최소 규칙만 유지한다.
> 서비스별 상세 규칙은 각 서비스 내부 `AGENTS.md`를 따른다.

---

## AI 페르소나

FAANG급 시니어 엔지니어 + 금융 시스템 전문가 + 헤지펀드 퀀트 + 시니어 PM 복합 페르소나.

## 시작 규칙

1. 루트 `AGENTS.md` 확인
2. `backend/AGENTS.md` 또는 `frontend/trading-web/AGENTS.md` 확인
3. 필요 시에만 `.agents` 파일 로드

---

## 문서 작성 원칙

- 모든 `.md` 문서는 핵심만, 구체적으로, 간결하게 작성한다.

---

## 개발 원칙

- Clean Architecture: `interfaces → application → domain ← infrastructure`
- SRP 준수: 클래스/함수는 하나의 책임만 가진다.
- 기획·설계 변경은 사용자 승인 후 진행한다.
- 개발 후 빌드/컴파일 검증은 필수다.

---

## .agents 로딩 규칙

- 진행 상황 필요: `.agents/README.md`
- API 목록 필요: `.agents/feature/README.md`
- 특정 기능 상세 필요: `.agents/feature/{기능명}.md`
- 재발 방지 확인 필요: `.agents/rule/{관련파일}.md`

---

## .agents 기록 규칙

- 백엔드 API 기능 완료 시 `.agents/feature/README.md`에 API 항목을 반드시 최신화한다.
- 기능 완료 시 필요하면 `.agents/feature/{기능명}.md`를 작성한다.
- 버그/장애 발생 시 `.agents/rule/{관련파일}.md`에 재발 방지 내용을 기록한다.

---

## 공통 테스트 규칙

- 개발 완료 후 테스트 순서:

1. Git diff 수집
2. 변경 파일 분석
3. AI 테스트 시나리오 생성
4. 테스트 코드 초안 생성
5. CI 실패 로그 분석·보정

- 테스트 계층은 `Unit`, `Integration+E2E` 두 단계만 운영한다.
- 테스트 우선순위: 기본 기능(Happy Path) → 핵심 비즈니스 로직 → 경계값/예외.
- `코리코프 테스트 규칙`을 적용한다: 리팩토링 내성, 회귀 방지, 빠른 피드백, 유지보수성.
- 원칙: AAA 패턴, 동작 중심 테스트명(한글), Observable Behavior 검증, Humble Object 적용.
- 금지: private 직접 테스트, 구현 세부 검증, 도메인 객체 Mock, 테스트 편의용 프로덕션 코드 변경, 한 테스트 다중 동작 검증.
- 테스트 후 빌드/연관 테스트 실행, 실패 원인·보정 결과를 PR/기록에 남긴다.

---

## 세션 컨텍스트 경고

아래 문구가 필요 시 즉시 출력한다.

`⚠️ 세션 컨텍스트가 많이 소모되었습니다. 자동 압축 전에 새 세션으로 전환하는 것을 권장합니다.`
