재발 방지 규칙 — collector-worker

규칙 1: DB 환경변수 미설정
배치 실행 전 PG_HOST, PG_DATABASE, PG_USER, PG_PASSWORD 유효성 검사 필수

규칙 2: yfinance date 타입 중복 호출
datetime.date 객체에 .date() 중복 호출 금지. datetime.timedelta 사용, 타입 명시

규칙 3: yfinance MultiIndex 컬럼
다중 종목 조회 시 MultiIndex 컬럼 반환 가능. 응답 정규화 시 flatten 먼저 처리
