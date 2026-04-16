@../../CLAUDE.md

## trading-web
운영 대시보드 UI
React / TypeScript / Vite

빌드 검증: npm run build

## 아키텍처
FSD 구조: pages → features → entities → shared (역방향 의존 금지)
API 호출은 shared/api/로 일원화, feature에서 직접 fetch 금지

## 코드 규칙
- any 금지, 불명확 타입은 unknown + 타입 가드
- 함수형 컴포넌트만 사용
- 상태 최소화, 서버 상태는 전용 관리 도구 사용
- 사이드이펙트는 useEffect 내부에서만 처리
- 로딩/에러/빈 상태 분리해서 사용자에게 명시

## 안정성
- 서버 계약(API 스키마/타입) 우회 금지
- 금액·수량 포맷 단일화, 임의 반올림/절삭 금지
- 인증 토큰/시크릿 평문 저장·로그 금지
- 핵심 비즈니스 계산 UI 재구현 금지, 서버 값 우선
