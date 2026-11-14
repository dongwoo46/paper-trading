# trading-web 기존 프런트엔드 요구 인벤토리

- 목적: 기존 trading-web을 처음부터 새로 만들기 위한 기능 요구 목록. 디자인 값(색·간격·토큰)은 의도적으로 제외.
- 근거: `frontend/trading-web/src` 의 App.tsx, pages, features, entities, shared (읽기 전용 조사). 사실만 기록하고 확인 못한 것은 6장에 모음.
- 표기: API 경로는 코드에 적힌 그대로. 기본 호스트는 `VITE_API_BASE_URL`(미설정 시 same-origin), 일부는 별도 접두사(`/api-ai`, `/api-research`, `/api-collector`) 또는 `VITE_QUANT_API_BASE_URL`(기본 `http://localhost:8000`).

## 1. 앱 소개와 사용자 유형

한 줄: 모의/실전 연동 계좌의 주문·포지션을 다루고, 시장 데이터 수집을 운영하며, 차트·AI 분석으로 이어지는 «운영 워크스테이션» (화면 문구 기준 «Paper Trading · 운영 워크스테이션»).

| 사용자 유형 | 코드상 근거 | 주로 쓰는 화면 |
|---|---|---|
| 트레이더(주문·복기) | 주문 생성/취소, 포지션, 일지, 세금 | 계좌·포지션, 주문 관리, 포트폴리오, 거래 일지, 세금 요약 |
| 데이터 운영자 | 구독 추가/해지, 카탈로그 동기화, 일괄 수집 | 실시간 데이터, 과거 시세 수집, 거시경제 지표 |
| 분석가 | 차트·지표·AI 리포트 | 통합 시세 차트, 차트 분석 |

로그인/권한/사용자 구분 화면은 없음(코드에서 인증 흐름 미확인). 사용자 유형은 화면 성격에서 나눈 분류이며 코드에 역할 개념은 없음.

## 2. 화면 목록 (라우트 11개 + 미정의 경로 리다이렉트)

6상태 표기: 로딩 / 빈 / 에러 / 성공(데이터) / 경고(부분) / 처리중(진행·비활성). «O»=코드에서 확인, «-»=확인 못함 또는 없음.

### 2-1. 개요 그룹

#### `/` 홈
| 항목 | 내용 |
|---|---|
| 목적 | 업무 영역별 진입점 안내 |
| 핵심 기능 | 내비게이션 그룹별 카드에서 각 화면으로 이동 |
| 화면 데이터 | 그룹명, 화면명, 화면 설명(정적 텍스트) |
| API | 없음 |
| 6상태 | 해당 없음(정적) |

#### `/account` 계좌·포지션
| 항목 | 내용 |
|---|---|
| 목적 | 계좌별 예수금과 보유 포지션 확인 |
| 핵심 기능 | 계좌 카드 선택(미선택 시 첫 계좌 자동 선택), 포지션 새로고침 |
| 계좌 데이터 | 계좌명, 거래 모드(LOCAL/KIS 모의/KIS 실전), 계좌 유형(PAPER/LIVE/VIRTUAL), 예수금, 가용 예수금(기준통화 병기), 활성 여부(비활성 표시) |
| 포지션 데이터 | 종목, 시장, 수량, 평균단가, 현재가, 평가금액, 평가손익, 수익률, 가격소스 (응답에는 주문가능수량·잠금수량·시세갱신시각도 있으나 화면 미표시) |
| API | GET `/api/v1/accounts`, GET `/api/v1/accounts/{accountId}/positions` |
| 6상태 | 계좌 목록: 로딩 O / 빈 O(«등록된 계좌가 없습니다») / 에러 O. 포지션: 로딩 O / 빈 O(«포지션 없음») / 에러 O. 현재가 없으면 «-» 표시(설명문에 장 외 시간 안내). 새로고침 진행 표시 - |

### 2-2. 트레이딩 그룹

#### `/orders` 주문 관리
| 항목 | 내용 |
|---|---|
| 목적 | 주문 생성, 주문 조회, 취소 |
| 핵심 기능 | 계좌 선택(필수, 비활성 계좌는 선택 불가) 후 탭 «주문 생성»/«주문 내역». 주문 제출 성공 시 «주문 내역» 탭으로 자동 전환. 상태별 필터(전체/PENDING/PARTIAL/FILLED/CANCELLED/REJECTED), 주문 취소(확인창 후), 5초 주기 자동 갱신 |
| 주문 입력 | 종목코드(대문자 변환), 시장, 주문 유형, 매매 방향, 주문 조건, 수량, 지정가(LIMIT일 때만), 만료일시(GTD일 때만) |
| 클라이언트 검증 | 종목코드 필수, 수량>0, LIMIT이면 지정가>0, GTD이면 만료일시 필수이며 현재 이후 |
| 주문 내역 데이터 | 주문 ID, 종목, 시장, 매매, 유형, 조건, 수량/체결수량, 지정가, 평균체결가, 수수료, 상태, 주문일시 |
| 취소 규칙 | PENDING, PARTIAL 상태만 취소 버튼 활성 |
| API | GET `/api/v1/accounts`, POST `/api/v1/accounts/{accountId}/orders`, GET `/api/v1/accounts/{accountId}/orders`, DELETE `/api/v1/accounts/{accountId}/orders/{orderId}` (204) |
| 6상태 | 계좌 선택기: 로딩 O / 에러 O. 계좌 미선택 안내 O(«계좌를 선택해 주세요»). 주문 목록: 로딩 O / 빈 O / 에러 O. 취소 실패 에러 O. 제출 중 비활성+문구 O. 입력 검증 에러 O. 서버 에러 메시지 그대로 노출 |

#### `/portfolio` 포트폴리오 차트
| 항목 | 내용 |
|---|---|
| 목적 | 일별 평가금액과 KOSPI 대비 누적 수익률 비교 |
| 핵심 기능 | 계좌 선택, 기간 선택(1개월/3개월/6개월, 기본 3개월) |
| 화면 데이터 | 날짜, 평가금액, 포트폴리오 수익률(%), KOSPI 수익률(%). 현재 구현은 표(table)이며 실제 차트 아님 |
| API | GET `/api/v1/accounts`, GET `/api/v1/accounts/{accountId}/daily-balances?from&to`, GET `/api/v1/benchmarks/kospi?from&to` |
| 6상태 | 로딩 O / 빈 O / 에러 O / 경고 O(벤치마크 404 또는 일부 날짜 누락 시 안내). 계좌 목록 자체의 로딩·에러 처리 - |

#### `/trading-journals` 거래 일지
| 항목 | 내용 |
|---|---|
| 목적 | 거래 기록 조회와 판단 근거·회고 작성 |
| 핵심 기능 | 티커·기간(기본 최근 30일) 필터 후 «조회», 목록에서 일지 선택, 상세에서 심리/제목/내용 편집 후 저장 |
| 목록 데이터 | 티커만 표시(응답에는 제목·요약·유형·심리·주문ID 등 있음) |
| 상세/편집 | 심리(BULLISH/BEARISH/NEUTRAL/REFLECTIVE), 제목, 내용 |
| 계좌 | 계좌 선택 UI 없음. 첫 번째 계좌로 고정 |
| API | GET `/api/trading-journals?accountId&ticker&from&to&page&size`, GET `/api/trading-journals/{journalId}`, PATCH `/api/trading-journals/{journalId}`, (계좌 조회) GET `/api/v1/accounts` |
| 6상태 | 목록: 로딩 O / 빈 O / 에러 O. 상세: 로딩 O / 빈(미선택 안내) O / 에러 O / 저장 중 비활성 O. 저장 성공·실패 피드백 - . 페이지는 0페이지·20건 고정 |

#### `/tax-summary` 세금 요약
| 항목 | 내용 |
|---|---|
| 목적 | 계좌·연도별 세금 요약 조회와 재계산 |
| 핵심 기능 | 계좌 선택, 연도 선택(올해 포함 최근 6년), «세금 재계산» |
| 화면 데이터 | 상태(준비됨/계산 중/실패), 계산 시각, 실현 이익, 실현 손실, 과세 표준, 세액, 지방세, 실효 세율(소수 둘째 자리 반올림 표기). 금액은 KRW 표기 |
| 재계산 규칙 | 요청 중, 상태 RUNNING, 직전 409 응답이면 버튼 비활성 |
| API | GET `/api/accounts/{accountId}/tax-summaries/{taxYear}`, POST `/api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`, (계좌 조회) GET `/api/v1/accounts` (주의: 세금 API만 `/api/v1` 아님) |
| 6상태 | 계좌: 로딩 O / 빈 O / 에러 O. 요약: 로딩 O / 빈(null 응답) O / 에러 O(400·404 문구 구분). 재계산 에러 O(409·400·404 문구 구분). 재계산 성공 안내 - |

### 2-3. 시장 데이터 그룹

#### `/realtime` 실시간 데이터
탭 2개: «국내 주식 (KIS)», «가상화폐 (Upbit)».

KIS 탭
| 항목 | 내용 |
|---|---|
| 목적 | KIS 실시간(WS)/일반(REST) 수집 대상 종목 구독 관리 |
| 핵심 기능 | 계좌 모드(모의투자/실전투자)·수집 채널(WS/REST) 전환, 종목 카탈로그 검색(이름·코드·시장·구독상태 필터, «더보기»로 20건씩 추가), 구독 추가/해지(종목코드 입력), 현재가 REST 조회, 구독 연결 상태 확인(수동 새로고침 + 10초 자동 갱신) |
| 화면 데이터 | 모드, 채널, 현재 구독 수, 카탈로그(전체/구독중/조회결과 건수), 구독 중 종목 목록, 현재가 조회 결과, 모드별 연결 상태(연결상태, 마지막 연결시각, 재연결 횟수, WS/REST 종목, WS 슬롯 사용/최대) |
| API | GET `/api/kis/ws/subscriptions`, GET `/api/kis/rest/watchlist`, GET `/api/kis/symbols/catalog?mode&channel&limit&query&market&status`, GET `/api/kis/symbols/subscriptions?mode&channel`, POST·DELETE `/api/kis/symbols/subscriptions`, GET `/api/kis/rest/watchlist/price?mode&symbol`, GET `/api/subscriptions/status` |
| 6상태 | 연결 상태: 로딩 O / 에러 O / 빈 O. 구독 목록 빈 O. 검색 결과 빈 O. 카탈로그 로딩·에러는 별도 표시 확인 못함. 변경 결과는 «요청 실패»/«구독 상태가 변경되었습니다.» 문구 |

Upbit 탭
| 항목 | 내용 |
|---|---|
| 목적 | Upbit 마켓 구독 관리 |
| 핵심 기능 | 카탈로그 조회/검색, 마켓 구독·해지, 카탈로그 동기화 |
| 화면 데이터 | 마켓 코드, 이름(한글/영문), 마켓 그룹, 활성/기본 여부, 구독 중 마켓 목록 |
| API | GET `/api/upbit/markets/catalog`, GET `/api/upbit/markets/search`, GET `/api/upbit/markets/subscriptions`, POST·DELETE `/api/upbit/markets/subscriptions`, POST `/api/upbit/markets/catalog/sync` |
| 6상태 | 구독 목록 빈 O. 요청 실패/성공 문구 O. 그 외 로딩·에러 표시 부분적 |

#### `/historical` 과거 시세 수집
탭 2개: «국내 시장 (pykrx)», «해외 시장 (yfinance)». 각 탭 안에 하위 탭 3개.

| 하위 탭 | 핵심 기능 | 화면 데이터 |
|---|---|---|
| 종목 탐색 | 심볼/이름·시장·구독상태(구독 중/미구독) 필터 검색, 구독/해지, «더보기» | 카탈로그 건수(전체/구독중/조회결과), 카탈로그 표, 활성 구독 목록 |
| 데이터 수집 관리 | 카탈로그 수동 동기화(pykrx만 지원), 심볼별 수집 기준일 업데이트, 구독 종목 일봉 일괄 수집(소스: 모든/국내/해외, 기본 종목만 여부, 시작·종료일) | 수집 결과 요약(성공/실패 건수) |
| 시세 데이터 조회 | 심볼·기간으로 일봉 OHLCV 로드 | DB 로드 가능 종목 수, 조회 결과 건수, 일봉(소스, 심볼, 시장, 거래일, 시·고·저·종가, 거래량, 수정종가, 제공자, 주기, 수정여부, 수집시각) |

| API | 경로 |
|---|---|
| 카탈로그 | GET `/api/{pykrx\|yfinance}/symbols/catalog`, GET `.../symbols/search`, GET `.../symbols/subscriptions` |
| 구독 | POST·DELETE `/api/{pykrx\|yfinance}/symbols/subscriptions` (본문 키: pykrx=symbol, yfinance=ticker) |
| 동기화 | POST `/api/pykrx/symbols/catalog/sync` (yfinance는 «unsupported» 처리) |
| 수집 기준일 | POST `/api/{pykrx\|yfinance}/symbols/collection-status` |
| 일괄 수집 | POST `/api/collect/daily` |
| 조회 | GET `/api/{pykrx\|yfinance}/ohlcv/symbols?limit=100`, GET `/api/{pykrx\|yfinance}/ohlcv/daily?...` |

6상태: 빈(목록·구독) O, 요청 실패/성공 문구 O(상단 상태 한 줄), 로딩 표시 일부, 표 단위 에러 표시 확인 못함.

#### `/macro` 거시경제 지표
탭 2개: «경제지표 탐색», «데이터 연동 및 품질 관리».

| 항목 | 내용 |
|---|---|
| 목적 | FRED 시리즈 탐색·구독, 동기화, 관측치 확인 |
| 핵심 기능 | 시리즈 ID/이름·카테고리·주기·구독상태 필터, 구독/해지, «더보기»; FRED 외부 검색, 시리즈 상세정보 조회, 관측치 조회, 카탈로그 동기화(최대 카테고리 수·페이지 크기 입력) |
| 화면 데이터 | 시리즈 ID, 지표명, 주기, 단위, 카테고리, 활성/기본 여부, 건수 배지(전체/구독중/조회결과, 외부 검색 결과 수, 관측치 수) |
| API | GET `/api/fred/series/catalog`, GET `/api/fred/series/subscriptions`, POST·DELETE `/api/fred/series/subscriptions`, GET `/api/fred/series/search`, GET `/api/fred/series/info`, GET `/api/fred/series/observations`, POST `/api/fred/series/catalog/sync` |
| 6상태 | 구독 목록 빈 O, 요청 실패/성공 문구 O, 로딩 표시 일부, 상세·관측치 결과는 건수/성공 여부 배지로만 확인(원본 값 표시 여부 미확인) |

#### `/market-unified` 통합 시세 차트
| 항목 | 내용 |
|---|---|
| 목적 | 종목별 주봉·일봉·분봉 캔들 차트와 기술 지표 비교 |
| 핵심 기능 | 종목 검색·선택(표), 데이터 소스 선택(pykrx/yfinance), 주기 전환(1분/5분/10분/일/주), 분봉 조회 개수(50/100), 새로고침, 지표 토글, 그리기 도구(선택/추세선/수평선/크로스, «선 전체 삭제»), 크로스헤어 값 표시 |
| 지표 | 거래량, 볼린저밴드, RSI, MACD, 투자자별 수급(외국인/기관/개인), 이동평균 5/20/60/120 (초기값: 거래량·MA 4종 켜짐, 나머지 꺼짐) |
| 화면 데이터 | 종목 표(심볼, 이름), 선택 종목, 주기, 데이터 건수, 최신 종가, 크로스헤어 시점·지표값(BB 상/중/하 등), OHLCV 표(시각/일자, 시·고·저·종가 등) |
| API | GET `/api/{pykrx\|yfinance}/ohlcv/symbols?limit=300`, GET `/api/{source}/ohlcv/daily?symbol`, GET `/api/market/weekly/{symbol}?source&limit=130`, GET `/api/market/bars/{symbol}?interval&limit`, GET `{QUANT_API_BASE}/investor-flow/{symbol}?from&to&limit` |
| 6상태 | 종목 목록: 로딩 O / 빈 O / 에러 O. 차트: 로딩 O / 빈 O / 에러 O. 투자자 수급 조회 실패 처리 미확인 |
| 상태 저장 | 지표 토글은 전역 상태. 그리기 선은 `${source}-${symbol}-${interval}` 키별로 보관(영속 저장 여부 미확인) |

### 2-4. 분석 그룹

#### `/chart-analysis` 차트 분석
| 항목 | 내용 |
|---|---|
| 목적 | OHLCV 수집 → 기술 분석 → LLM 설명 저장까지 한 흐름 처리 |
| 핵심 기능 | 전체 데이터 일괄 수집 4종(yfinance 일봉, KIS 일봉, KIS 주봉, yfinance 주봉), 수집된 종목 표(검색 + 소스 필터 전체/yfinance/pykrx/kis) 및 선택, «해당 종목 분석하기», «LLM 차트 분석 요청»(백그라운드 생성, 완료 시 알림), 분석 윈도우 전환(1M/3M/6M/1Y/2Y/전체), 항목별 도움말 |
| 화면 데이터 | 수집 종목(종목, 이름, 소스, 시장, 일봉·주봉 건수, 최근 일봉·주봉 일자), 추천+신뢰도, 추세(방향/강도/MA 정렬/ADX/고점저점 구조), 가격 레벨(지지/저항/진입/손절/목표/손익비), 거래량(추세/스파이크/평균비율), 캔들 패턴(패턴/날짜/봉 위치), 지표 신호(지표/값/해석), 윈도우·봉 종류·리포트 상태·스냅샷, 템플릿 분석(룰 기반) vs LLM 분석(AI 생성) 5개 섹션(추세, 지지/저항, 진입 계획, 근거, 리스크) |
| LLM 요청 응답 상태 | queued / pending / already_done / not_found / not_popular (일부는 안내 문구 존재) |
| API | GET `/api-collector/market/collected-symbols?source`, POST `/api-collector/collect/{daily\|weekly}`, GET `/api-research/research/results/{symbol}`, POST `/api-research/research/run/{symbol}`, POST `/api-ai/chart-analysis/{symbol}/report?window&interval`, SSE `/api-ai/chart-analysis/notifications` |
| 6상태 | 수집 종목: 로딩 O / 빈 O. 분석 결과: 로딩 O. 분석·LLM 요청 진행 중 문구 O, LLM 생성 중 표시 O. 결과 없음 안내 O. 분석 결과 조회 에러 처리 미확인. 수집 결과 요약은 «아직 실행된 수집 작업이 없습니다» 빈 문구 확인 |

#### 미정의 경로 `*`
`/` 로 리다이렉트(replace).

## 3. 공통 요소

| 요소 | 내용 |
|---|---|
| 레이아웃 | 좌측 사이드바 + 상단 바 + 본문. 기준폭 1024px 이상이면 사이드바 상시 표시, 미만이면 서랍(drawer) |
| 사이드바 | 앱 이름 «Paper Trading» / «운영 워크스테이션», 내비 그룹 4개, 하단 버전 문구 «Trading Hub v1.2.0». 모바일: 열림 시 본문 비활성(inert), ESC 닫기, 포커스 가둠, 링크 클릭 시 닫힘, 배경 클릭 닫힘 |
| 내비 그룹 | 개요(홈, 계좌·포지션) / 트레이딩(주문 관리, 포트폴리오 차트, 거래 일지, 세금 요약) / 시장 데이터(실시간 데이터, 과거 시세 수집, 거시경제 지표, 통합 시세 차트) / 분석(차트 분석) |
| 상단 바 | 모바일에서 메뉴 버튼, 앱 이름 표기, 알림 벨(미읽음 수 배지, 9 초과 시 «9+») |
| 알림 | LLM 분석 완료 SSE 이벤트를 앱 전체 수명 동안 구독. 벨 클릭 시 전체 읽음 처리 + 드롭다운. 항목: 종목코드·이름, 윈도우, 일봉/주봉, 소스, 시각. 최대 20건 보관, 모두 읽음/지우기, 바깥 클릭 닫힘. 메모리 저장(새로고침 시 소실) |
| 체결 토스트 | 체결 SSE(`/api/v1/executions/stream`) 수신 시 토스트. 매수 체결/매도 체결, 종목명(없으면 코드) 수량 @ 가격, 주문 번호. 최대 5개, 4.5초 후 자동 닫힘, 수동 닫기. KRW는 «원», 그 외는 «$» 표기 |
| 페이지 헤더 | 모든 화면 공통: eyebrow(그룹명) + 제목 + 설명 |
| 라우트 에러 | 라우트 단위 에러 바운더리(이동 시 초기화) + 페이지 로딩 스켈레톤(lazy 로드) |
| 계좌 선택 | 전역 «현재 계좌» 개념 없음. 화면마다 독립 상태: 계좌·포지션(카드 선택), 주문(드롭다운, 비활성 계좌 선택 불가, 거래 모드 병기), 포트폴리오·세금(드롭다운), 거래 일지(첫 계좌 고정). 주문 화면 외에는 미선택 시 첫 계좌 자동 선택 |
| 서버 데이터 | TanStack Query 사용. 계좌 목록 staleTime 30초 |
| 상태 표시 한 줄 | 시장 데이터 관리 패널들(KIS/Upbit/FRED/Catalog)은 작업 결과를 «준비 완료/요청 실패/…완료» 같은 한 줄 문구로 표시 |
| 숫자 포맷 | 금액 `ko-KR` 소수 2자리 고정(통화 코드 병기 가능), 수익률 소수 2자리 + `%`, null은 «-» |
| 시간 표기 | 주문일시는 `ko-KR` 로케일 문자열, 알림 시각은 `ko-KR` 시:분:초 |

## 4. 도메인 용어집 (코드에서 확인한 값)

| 개념 | 값 | 비고 |
|---|---|---|
| 주문 상태 OrderStatus | PENDING, PARTIAL, FILLED, CANCELLED, REJECTED | 취소 가능: PENDING, PARTIAL |
| 매매 방향 OrderSide / ExecutionSide | BUY, SELL | 화면 문구: 매수/매도, 체결 토스트는 «매수 체결/매도 체결» |
| 주문 유형 OrderType | MARKET(시장가), LIMIT(지정가) | |
| 주문 조건 OrderCondition | DAY, GTC, IOC, FOK, GTD | GTD만 만료일시 필요 |
| 시장 구분 MarketType (주문) | KOSPI, KOSDAQ, NASDAQ, NYSE, CRYPTO | 계좌 모델에서는 string 허용 |
| 거래 모드 TradingMode | 계좌: LOCAL, KIS_PAPER, KIS_LIVE / 주문 모델: 추가로 UPBIT_LIVE | 화면 라벨: LOCAL, «KIS 모의», «KIS 실전» |
| 계좌 유형 AccountType | PAPER, LIVE, VIRTUAL | |
| 가격 소스 PriceSource | REDIS, NONE (string 허용) | 포지션 표에 그대로 표시 |
| 세금 요약 상태 | READY(준비됨), RUNNING(계산 중), FAILED(실패) | |
| 일지 심리 JournalSentiment | BULLISH, BEARISH, NEUTRAL, REFLECTIVE | journalType은 문자열이며 허용 값 미확인 |
| KIS 모드 / 채널 | 모드: paper(모의투자), live(실전투자) / 채널: ws(실시간), rest(일반) | |
| 데이터 소스 | pykrx(국내), yfinance(해외), kis(차트 분석 수집에서만) | |
| 통합 차트 주기 | 1m, 5m, 10m, 1d, 1w | |
| 분석 봉 종류 | D(일봉), 그 외는 «주봉»으로 표기 | 실제 값은 코드에 정의 없음 |
| 분석 윈도우 | 1M, 3M, 6M, 1Y, 2Y, MAX | |
| 분석 추천 | BUY / SELL / HOLD | 도움말 문구 기준 |
| 구독 상태 필터 | SUBSCRIBED, UNSUBSCRIBED | |
| 멱등키 | idempotencyKey = 브라우저 `crypto.randomUUID()` | 주문 요청마다 새로 생성 |

## 5. 기능 제약 (색 값 제외)

| 구분 | 규칙 | 근거 |
|---|---|---|
| 금액·수량 표현 | 서버 응답의 금액·수량·가격·수수료는 문자열(BigDecimal 직렬화). 주문 요청의 수량·지정가도 문자열로 전송 | 주문/계좌/세금 타입 |
| 현재 코드의 예외 | 표시용 포맷에 `parseFloat`, 포트폴리오 API는 문자열을 `Number`로 변환해 계산, 통합 차트·투자자수급도 숫자 변환. 신규 구현에서 금융 값을 숫자로 바꾸지 않는 규칙을 적용할지 결정 필요 | formatAmount, portfolioApi, marketUnifiedApi |
| 수량 입력 | 소수 허용(`step=any`), 0 초과 | 주문 폼 |
| 세율 표기 | 문자열을 소수 둘째 자리까지 수동 반올림(부동소수점 회피) | TaxSummarySummaryPanel |
| 매수/매도 구분 | 매수와 매도는 시각적으로 구분해야 함(주문 표, 체결 토스트에서 별도 톤). 텍스트(BUY/SELL, 매수/매도)도 병기 | OrderTable, ToastItem |
| 손익 구분 | 평가손익·수익률은 양수/음수 톤 구분, 값 없으면 중립. 0은 양수 취급. 양수에는 «+» 부호(손익 금액) | PositionTable |
| 손익 톤과 매매 톤 분리 | 코드는 매수/매도 톤과 손익(상승/하락) 톤을 서로 다른 토큰으로 분리 | OrderTable, PositionTable |
| 주문 상태 구분 | 상태 5종이 서로 구분 가능해야 함(대기=경고, 부분=기본, 체결=긍정, 취소=중립, 거부=위험 성격) | OrderTable |
| 모드 구분 | 거래 모드(LOCAL/모의/실전)와 계좌 유형(PAPER/LIVE/VIRTUAL)을 배지로 구분. 실전(KIS 실전, LIVE)은 위험 성격으로 강조 | AccountCard |
| 파괴적 동작 | 주문 취소만 브라우저 확인창으로 한 번 확인. 구독 해지·일지 저장·재계산은 확인 단계 없음 | OrderTablePanel 외 |
| 중복 제출 방지 | 제출/재계산/저장 중 버튼 비활성. 주문은 멱등키 사용 | OrderFormPanel, Tax, Journal |
| 갱신 | 주문 내역 5초, KIS 연결 상태 10초 자동 갱신. 포지션은 수동 새로고침 | 각 패널 |
| 에러 메시지 | 서버 응답 본문을 `{status} {statusText}: {body}` 형태로 그대로 노출하는 곳 있음(주문 실패/취소 실패). 세금 화면은 상태코드별 안내 문구 | fetchJson, 세금 화면 |
| 접근성 | 모바일 서랍 포커스 가둠·ESC, 아이콘 버튼에 aria-label, 로딩 스켈레톤에 aria-label, 선택 토글에 aria-pressed | Sidebar, 각 화면 |

## 6. 불확실하거나 코드로 확인하지 못한 항목 (총 15건)

| # | 항목 | 상태 |
|---|---|---|
| 1 | 인증/로그인/사용자 역할 | 코드에 흐름 없음. 사용자 유형(1장)은 화면 성격에서 나눈 분류 |
| 2 | 시장 데이터 4개 패널(KIS/Upbit/FRED/Catalog)의 로딩·에러 표시 세부 | 일부만 확인(상태 한 줄 문구 위주) |
| 3 | Upbit 패널 표 컬럼, 검색 UI 세부 | 요약 수준으로만 확인 |
| 4 | FRED 상세정보·관측치 조회 결과가 화면에 값으로 보이는지 | 건수·성공 배지만 확인 |
| 5 | 통합 시세 차트의 투자자 수급 API 실패 처리, 그리기 선 영속 여부, 분봉 API가 소스(pykrx/yfinance)에 종속되는지 | 미확인 |
| 6 | RSI/MACD/볼린저 계산 위치(클라이언트 계산 여부) | `features/unified-chart-indicators/model/indicators.ts` 존재로 클라이언트 계산으로 보이나 미확인 |
| 7 | 차트 분석 화면의 분석 결과 에러 처리, 종목별 수집·`fetchAnalyzedSymbols`·`fetchCatalogSymbols` 실제 사용 여부 | 정의만 확인, 사용처 미확인 |
| 8 | 분석 봉 종류의 주봉 값, FRED 주기 전체 선택지, journalType 허용 값 | 코드에 정의 없음 |
| 9 | 주문 모델의 `UPBIT_LIVE` 모드가 쓰이는 화면 | 타입에만 존재 |
| 10 | API 경로 접두사 혼재(`/api/v1`, `/api`, `/api-ai`, `/api-research`, `/api-collector`, 8000 포트)의 실제 백엔드 대응·프록시 설정 | vite/배포 설정 미조사 |
| 11 | 체결·분석 알림 SSE 연결 실패/재연결 시 사용자 안내 | 자동 재연결에 맡김, UI 표시 없음(주석 확인) |
| 12 | 포트폴리오 «차트»가 표로만 구현된 것이 의도인지 | 현 코드는 표 |
| 13 | 거래 일지의 생성 경로(주문 연동 자동 생성 여부), 페이지네이션 UI | 생성 화면 없음, 0페이지·20건 고정 |
| 14 | `docs/UI_GUIDE.md` «화면 구성»과의 대조 | 읽지 않음(코드만 근거) |
| 15 | 테스트 파일(`*.test.tsx`)이 정의하는 추가 동작 요구 | 읽지 않음 |
