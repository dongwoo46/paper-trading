Trading Web 역할 기반 페이지/탭 개편

목적/배경
소스별 역할 차이를 반영해 화면을 직관적으로 분리하기 위함.
운영자가 실시간/과거/거시경제 데이터를 페이지 단위로 관리하도록 UX를 정리하기 위함.

변경 범위
UI/라우팅:
메인 홈 + 상단 헤더 내비게이션 추가
페이지 분리:
실시간 데이터: KIS / Upbit 탭
과거 OHLCV: pykrx / yfinance 탭
거시경제: FRED 페이지
설명 영역:
각 페이지/탭 상단에 소스 특징 및 수집 데이터 설명 표시
API 연동:
KIS 구독/관심종목 관리
FRED catalog/selections
pykrx/yfinance catalog/search/selections

검증
실행 명령: npm run build (frontend/trading-web)
결과: 성공

후속 TODO
Upbit 탭에 신규 시장 catalog/subscription API 연결
심볼 상세 차트(OHLCV) 컴포넌트 연결
홈 카드에 소스별 선택 건수 실시간 표시
