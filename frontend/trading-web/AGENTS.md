@../../AGENTS.md

---

# trading-web 서비스 규칙

사용자 UI — 모의투자 대시보드, 주문, 포트폴리오, 전략 관리.
**React / TypeScript / Vite**
검증: `npm run build`

---

## 디렉토리 구조 (FSD — Feature-Sliced Design)

```
src/
├── app/          # 앱 초기화, 라우터, 글로벌 프로바이더
├── pages/        # 페이지 단위 컴포넌트 (라우트 진입점)
├── features/     # 사용자 행동 단위 (주문, 전략 등록 등)
├── entities/     # 도메인 엔티티 UI (계좌, 종목, 포지션 등)
├── shared/       # 재사용 UI 컴포넌트, 유틸, API 클라이언트
└── assets/       # 정적 파일
```

의존 방향: `pages → features → entities → shared` (역방향 금지)

---

## TypeScript 규칙

- `any` 사용 금지. 타입 불명확 시 `unknown` + 타입 가드 사용.
- API 응답 타입은 반드시 명시적으로 정의. 추론에만 의존하지 않는다.
- 컴포넌트 Props는 `interface`로 정의. `type`과 혼용 금지.
- `null` / `undefined` 구분 명확히. 옵셔널 체이닝(`?.`) 남용 금지.

---

## React 코딩 규칙

- 함수형 컴포넌트만 사용. Class 컴포넌트 금지.
- 상태는 최소화. 서버 상태는 서버 상태 관리 라이브러리(React Query 등) 활용.
- 렌더링 최적화: `useMemo`, `useCallback`은 실제 성능 문제가 있을 때만 적용.
- 사이드 이펙트는 `useEffect` 내부에서만. 컴포넌트 최상위에서 직접 실행 금지.
- 컴포넌트 파일 1개당 1개의 컴포넌트 원칙.

---

## 금융 데이터 표시 규칙

- 금액 표시: 원화 `toLocaleString('ko-KR')`, 달러 등은 단위 명시.
- 수익률: 양수 = 초록, 음수 = 빨강으로 색상 구분.
- 실시간 데이터는 WebSocket 끊김 상태를 사용자에게 명시적으로 표시.
- 주문 확인 등 불가역 행동은 반드시 확인 모달 거친다.

---

## API 통신 규칙

- API 클라이언트는 `shared/api/` 에 집중. 각 feature에서 직접 fetch 금지.
- 에러 응답은 공통 핸들러로 처리. 각 컴포넌트에서 개별 try-catch 남발 금지.
- 로딩/에러/성공 상태를 항상 UI에 반영한다.

---

## .agents 관리

- 기능 완료 시 `.agents/feature/{날짜}-{기능명}.md` 생성.
- 버그/장애 발생 시 `.agents/rule/`에 재발 방지 기록 (핵심만 짧게).
