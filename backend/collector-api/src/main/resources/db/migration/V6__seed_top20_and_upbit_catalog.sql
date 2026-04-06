-- pykrx: catalog는 전체(kr_symbol 기반), 기본 20개만 활성화
INSERT INTO pykrx_symbol_catalog (symbol, name, market, enabled, is_default)
SELECT
    k.symbol,
    k.name,
    k.market,
    FALSE,
    FALSE
FROM kr_symbol k
ON CONFLICT (symbol) DO UPDATE SET
    name = EXCLUDED.name,
    market = EXCLUDED.market,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO pykrx_symbol_catalog (symbol, name, market, enabled, is_default) VALUES
    ('005930', '삼성전자', 'KOSPI', TRUE, TRUE),
    ('000660', 'SK하이닉스', 'KOSPI', TRUE, TRUE),
    ('373220', 'LG에너지솔루션', 'KOSPI', TRUE, TRUE),
    ('207940', '삼성바이오로직스', 'KOSPI', TRUE, TRUE),
    ('005380', '현대차', 'KOSPI', TRUE, TRUE),
    ('000270', '기아', 'KOSPI', TRUE, TRUE),
    ('035420', 'NAVER', 'KOSPI', TRUE, TRUE),
    ('005490', 'POSCO홀딩스', 'KOSPI', TRUE, TRUE),
    ('051910', 'LG화학', 'KOSPI', TRUE, TRUE),
    ('006400', '삼성SDI', 'KOSPI', TRUE, TRUE),
    ('035720', '카카오', 'KOSPI', TRUE, TRUE),
    ('068270', '셀트리온', 'KOSPI', TRUE, TRUE),
    ('105560', 'KB금융', 'KOSPI', TRUE, TRUE),
    ('055550', '신한지주', 'KOSPI', TRUE, TRUE),
    ('086790', '하나금융지주', 'KOSPI', TRUE, TRUE),
    ('247540', '에코프로비엠', 'KOSDAQ', TRUE, TRUE),
    ('086520', '에코프로', 'KOSDAQ', TRUE, TRUE),
    ('196170', '알테오젠', 'KOSDAQ', TRUE, TRUE),
    ('293490', '카카오게임즈', 'KOSDAQ', TRUE, TRUE),
    ('122630', 'KODEX 레버리지', 'ETF', TRUE, TRUE)
ON CONFLICT (symbol) DO UPDATE SET
    name = EXCLUDED.name,
    market = EXCLUDED.market,
    enabled = TRUE,
    is_default = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- yfinance: catalog seed + 기본 20개만 활성화
INSERT INTO yfinance_symbol_catalog (ticker, name, market, enabled, is_default) VALUES
    ('AAPL', '애플', 'US', TRUE, TRUE),
    ('MSFT', '마이크로소프트', 'US', TRUE, TRUE),
    ('NVDA', '엔비디아', 'US', TRUE, TRUE),
    ('AMZN', '아마존', 'US', TRUE, TRUE),
    ('GOOGL', '알파벳 A', 'US', TRUE, TRUE),
    ('META', '메타 플랫폼스', 'US', TRUE, TRUE),
    ('TSLA', '테슬라', 'US', TRUE, TRUE),
    ('BRK-B', '버크셔 해서웨이 B', 'US', TRUE, TRUE),
    ('JPM', 'JP모건체이스', 'US', TRUE, TRUE),
    ('V', '비자', 'US', TRUE, TRUE),
    ('LLY', '일라이 릴리', 'US', TRUE, TRUE),
    ('AVGO', '브로드컴', 'US', TRUE, TRUE),
    ('WMT', '월마트', 'US', TRUE, TRUE),
    ('XOM', '엑슨모빌', 'US', TRUE, TRUE),
    ('UNH', '유나이티드헬스 그룹', 'US', TRUE, TRUE),
    ('SPY', 'SPDR S&P 500 ETF', 'US', TRUE, TRUE),
    ('QQQ', '인베스코 QQQ ETF', 'US', TRUE, TRUE),
    ('IWM', '아이셰어즈 러셀2000 ETF', 'US', TRUE, TRUE),
    ('^GSPC', 'S&P 500 지수', 'INDEX', TRUE, TRUE),
    ('^IXIC', '나스닥 종합지수', 'INDEX', TRUE, TRUE)
ON CONFLICT (ticker) DO UPDATE SET
    name = EXCLUDED.name,
    market = EXCLUDED.market,
    enabled = TRUE,
    is_default = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- Upbit catalog (검색 + 구독 선택/해제용)
CREATE TABLE IF NOT EXISTS upbit_market_catalog (
    id BIGSERIAL PRIMARY KEY,
    market VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    market_group VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (market)
);

CREATE INDEX IF NOT EXISTS idx_upbit_market_catalog_group ON upbit_market_catalog (market_group);
CREATE INDEX IF NOT EXISTS idx_upbit_market_catalog_enabled ON upbit_market_catalog (enabled);

INSERT INTO upbit_market_catalog (market, name, market_group, enabled, is_default) VALUES
    ('KRW-BTC', '비트코인', 'KRW', TRUE, TRUE),
    ('KRW-ETH', '이더리움', 'KRW', TRUE, TRUE),
    ('KRW-XRP', '리플', 'KRW', TRUE, TRUE),
    ('KRW-SOL', '솔라나', 'KRW', TRUE, TRUE),
    ('KRW-ADA', '에이다', 'KRW', TRUE, TRUE),
    ('KRW-DOGE', '도지코인', 'KRW', TRUE, TRUE),
    ('KRW-AVAX', '아발란체', 'KRW', TRUE, TRUE),
    ('KRW-DOT', '폴카닷', 'KRW', TRUE, TRUE),
    ('KRW-LINK', '체인링크', 'KRW', TRUE, TRUE),
    ('KRW-TRX', '트론', 'KRW', TRUE, TRUE),
    ('KRW-ATOM', '코스모스', 'KRW', TRUE, TRUE),
    ('KRW-MATIC', '폴리곤', 'KRW', TRUE, TRUE),
    ('KRW-NEAR', '니어프로토콜', 'KRW', TRUE, TRUE),
    ('KRW-APT', '앱토스', 'KRW', TRUE, TRUE),
    ('KRW-ARB', '아비트럼', 'KRW', TRUE, TRUE),
    ('KRW-OP', '옵티미즘', 'KRW', TRUE, TRUE),
    ('KRW-SUI', '수이', 'KRW', TRUE, TRUE),
    ('KRW-ETC', '이더리움클래식', 'KRW', TRUE, TRUE),
    ('KRW-BCH', '비트코인캐시', 'KRW', TRUE, TRUE),
    ('KRW-AAVE', '에이브', 'KRW', TRUE, TRUE)
ON CONFLICT (market) DO UPDATE SET
    name = EXCLUDED.name,
    market_group = EXCLUDED.market_group,
    enabled = TRUE,
    is_default = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- KIS: 기본 20개 종목을 paper/live에 seed (수동 관리 시작점)
WITH defaults(symbol) AS (
    VALUES
    ('005930'), ('000660'), ('373220'), ('207940'), ('005380'),
    ('000270'), ('035420'), ('005490'), ('051910'), ('006400'),
    ('035720'), ('068270'), ('105560'), ('055550'), ('086790'),
    ('028260'), ('066570'), ('012330'), ('003670'), ('259960')
),
modes(mode) AS (
    VALUES ('paper'), ('live')
)
INSERT INTO kis_ws_subscriptions (mode, symbol)
SELECT m.mode, d.symbol
FROM modes m
CROSS JOIN defaults d
ON CONFLICT (mode, symbol) DO NOTHING;

WITH defaults(symbol) AS (
    VALUES
    ('005930'), ('000660'), ('373220'), ('207940'), ('005380'),
    ('000270'), ('035420'), ('005490'), ('051910'), ('006400'),
    ('035720'), ('068270'), ('105560'), ('055550'), ('086790'),
    ('028260'), ('066570'), ('012330'), ('003670'), ('259960')
),
modes(mode) AS (
    VALUES ('paper'), ('live')
)
INSERT INTO kis_rest_watchlist (mode, symbol)
SELECT m.mode, d.symbol
FROM modes m
CROSS JOIN defaults d
ON CONFLICT (mode, symbol) DO NOTHING;
