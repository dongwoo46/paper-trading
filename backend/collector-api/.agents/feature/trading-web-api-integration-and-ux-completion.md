Trading Web API 연동 및 UI/UX 보강 (README 규칙 반영)

목적
frontend/trading-web/README.md의 FSD, React Query, 다크 글래스 UI 규칙을 준수하면서
  미연결 API를 연결하고, 운영에 필요한 누락 UX(필터/상태/개수 가시성)를 채운다.

구현 범위
실시간 페이지
UpbitPanel 신규 API 연동 완료
카탈로그/검색 조회
상태 필터(구독/미구독)
선택 목록 조회
추가/해지 mutation
총 카탈로그/총 구독/현재 조회 개수 표시
KisPanel 문자열 복구 및 상호작용 정리
WS/REST 목록 조회
종목 검색
모드별 구독/해지
요약 지표(WS/REST/모드) UI 추가

과거 데이터 페이지
SymbolCatalogPanel을 최신 백엔드 응답 스펙으로 전환
CatalogResponse<T> (items, returnedCount, totalCatalogCount, totalSubscribedCount) 사용
상태 필터(구독/미구독) 반영
구독/해지 mutation 후 캐시 갱신
카운트 메타 표시

매크로 페이지
FredPanel을 React Query 기반으로 재구성
카탈로그/선택 목록 API 연동
검색 + 카테고리 + 주기 + 상태 필터
구독/해지 mutation 및 캐시 무효화
카운트 메타 표시

공통 UI/UX 정리
깨진 한글 텍스트 복구
홈/사이드바/페이지 타이틀 구조 정리
summary-strip, meta-row, spin 등 누락 스타일 추가
색상/그라디언트 톤을 README 방향에 맞게 정리 (보라 편향 완화)

검증
frontend/trading-web에서 npm run build 성공
