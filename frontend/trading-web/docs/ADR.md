# ADR — trading-web

## ADR-001: FSD (Feature-Sliced Design) 아키텍처
**결정**: pages → features → entities → shared 레이어 구조 채택
**이유**: 기능이 늘어날수록 파일이 도메인 단위로 응집. 역방향 의존 금지 규칙으로 순환 참조 방지.
**트레이드오프**: 초기 세팅 비용. 소규모 기능도 feature 폴더 구조를 만들어야 해서 파일 수 증가.

## ADR-002: TanStack React Query 서버 상태 전담
**결정**: 모든 서버 상태는 useQuery/useMutation. 수동 useEffect + fetch 금지.
**이유**: 캐싱, 재시도, 낙관적 업데이트를 라이브러리가 처리. 로딩/에러 상태 처리 일관성.
**트레이드오프**: React Query 학습 비용. 캐시 무효화 전략을 명시적으로 설계해야 함.

## ADR-003: Glassmorphism 다크 테마
**결정**: 배경 #06070a 기반 다크 글래스 UI. 브랜드 컬러 blue(#3b82f6)/green(#10b981).
**이유**: 트레이딩 도구 특성상 장시간 사용. 다크 테마가 눈 피로 감소.
**트레이드오프**: 과도한 glass 효과는 가독성 저하. 그라디언트/글로우 남용 금지.
