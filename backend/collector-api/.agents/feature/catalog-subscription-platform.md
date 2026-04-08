카탈로그/구독 플랫폼 확장 (FRED, pykrx, yfinance, Upbit, KIS)

목적/배경
소스별 데이터 수집 대상을 catalog(전체 후보)와 subscription(실제 수집 대상)으로 분리해 운영하기 위함.
기본 대표 항목만 활성화하고, 나머지는 검색 후 수동 추가/삭제 가능한 구조를 만들기 위함.

변경 범위
API:
FRED: catalog/selections/subscriptions, series search/info, catalog sync
pykrx: catalog/search/selections/subscriptions, catalog sync
yfinance: catalog/search/selections/subscriptions
Upbit: markets catalog/search/selections/subscriptions, catalog sync
KIS: symbols catalog/subscriptions(channel=ws|rest)
DB:
fred_series_catalog (seed 및 확장)
pykrx_symbol_catalog, yfinance_symbol_catalog
upbit_market_catalog
KIS 기본 20개 seed (ws/rest, paper/live)
도메인/서비스:
catalog 조회/선택 추가/삭제 서비스
시작 시 카탈로그 자동 동기화 런타임 추가

검증
실행 명령: .\gradlew.bat compileKotlin
결과: 성공

후속 TODO
yfinance 전체 카탈로그 소스(거래소 리스트 파일/외부 공급자) 기준 확정
KIS 전용 카탈로그 테이블 분리 여부 검토(현재는 kr_symbol 기반)
sync 실행 시간/실패율 모니터링 지표 추가
