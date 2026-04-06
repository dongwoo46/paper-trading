-- V11 통합본:
-- 1) 코어 23개는 기본 구독(enabled=true, is_default=true)
-- 2) 선택 기본 후보(옵션 + 침체 보강)는 기본 후보만(is_default=true), 구독 상태는 유지

-- 1) 코어 23개
INSERT INTO fred_series_catalog (series_id, title, category, frequency, units, enabled, is_default)
VALUES
    ('CPIAUCSL', '소비자물가지수(CPI)', 'inflation', 'monthly', 'index', TRUE, TRUE),
    ('CPILFESL', '근원 소비자물가지수(Core CPI)', 'inflation', 'monthly', 'index', TRUE, TRUE),
    ('FEDFUNDS', '기준금리(연방기금금리)', 'rates', 'monthly', 'percent', TRUE, TRUE),
    ('GS10', '미국 10년물 국채금리', 'rates', 'monthly', 'percent', TRUE, TRUE),
    ('GS2', '미국 2년물 국채금리', 'rates', 'monthly', 'percent', TRUE, TRUE),
    ('T10Y2Y', '장단기 스프레드(10Y-2Y)', 'rates', 'daily', 'percent', TRUE, TRUE),
    ('UNRATE', '실업률', 'labor', 'monthly', 'percent', TRUE, TRUE),
    ('PAYEMS', '비농업 고용', 'labor', 'monthly', 'thousands', TRUE, TRUE),
    ('ICSA', '신규 실업수당 청구', 'labor', 'weekly', 'count', TRUE, TRUE),
    ('GDPC1', '실질 GDP', 'growth', 'quarterly', 'index', TRUE, TRUE),
    ('INDPRO', '산업생산', 'growth', 'monthly', 'index', TRUE, TRUE),
    ('RSAFS', '소매판매', 'growth', 'monthly', 'usd', TRUE, TRUE),
    ('HOUST', '주택착공', 'growth', 'monthly', 'thousands', TRUE, TRUE),
    ('BAA10Y', '회사채 스프레드(Baa-10Y)', 'credit', 'daily', 'percent', TRUE, TRUE),
    ('BAMLH0A0HYM2', '하이일드 스프레드', 'credit', 'daily', 'percent', TRUE, TRUE),
    ('VIXCLS', 'VIX', 'risk', 'daily', 'index', TRUE, TRUE),
    ('NFCI', '금융환경지수(NFCI)', 'risk', 'weekly', 'index', TRUE, TRUE),
    ('M2SL', 'M2 통화량', 'liquidity', 'monthly', 'usd', TRUE, TRUE),
    ('WALCL', '연준 총자산', 'liquidity', 'weekly', 'usd', TRUE, TRUE),
    ('DTWEXBGS', '달러인덱스', 'fx', 'daily', 'index', TRUE, TRUE),
    ('CP', '실질 기업이익', 'profits', 'quarterly', 'usd', TRUE, TRUE),
    ('UMCSENT', '소비자심리지수', 'sentiment', 'monthly', 'index', TRUE, TRUE),
    ('ISRATIO', '재고/판매비율', 'growth', 'monthly', 'ratio', TRUE, TRUE)
ON CONFLICT (series_id) DO UPDATE SET
    enabled = TRUE,
    is_default = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- 2) 선택 기본 후보(옵션 + 침체 보강)
INSERT INTO fred_series_catalog (series_id, title, category, frequency, units, enabled, is_default)
VALUES
    ('TEDRATE', 'TED 스프레드', 'credit', 'daily', 'percent', FALSE, TRUE),
    ('DGS3MO', '미국 3개월 국채금리', 'rates', 'daily', 'percent', FALSE, TRUE),
    ('RECPROUSM156N', '미국 경기침체 확률', 'risk', 'monthly', 'percent', FALSE, TRUE),
    ('USSLIND', '미국 선행지수', 'growth', 'monthly', 'index', FALSE, TRUE),
    ('DEXKOUS', '원/달러 환율', 'fx', 'daily', 'krw_per_usd', FALSE, TRUE),
    ('DEXJPUS', '엔/달러 환율', 'fx', 'daily', 'jpy_per_usd', FALSE, TRUE),
    ('DCOILWTICO', 'WTI 유가', 'energy', 'daily', 'usd_per_barrel', FALSE, TRUE),
    ('MHHNGSP', '헨리허브 천연가스 가격', 'energy', 'daily', 'usd_per_mmbtu', FALSE, TRUE),
    ('MORTGAGE30US', '미국 30년 모기지 금리', 'housing', 'weekly', 'percent', FALSE, TRUE),
    ('DRCCLACBS', '신용카드 대출 연체율', 'credit', 'quarterly', 'percent', FALSE, TRUE),
    ('EXCSRESNS', '초과지급준비금', 'liquidity', 'weekly', 'usd', FALSE, TRUE),
    ('SAHMREALTIME', '샴 룰 경기침체 지표', 'risk', 'monthly', 'percent', FALSE, TRUE),
    ('T10Y3M', '장단기 스프레드(10Y-3M)', 'rates', 'daily', 'percent', FALSE, TRUE),
    ('CFNAIMA3', '시카고 연준 국가활동지수 3개월 평균', 'growth', 'monthly', 'index', FALSE, TRUE),
    ('KCFSI', '캔자스시티 금융스트레스지수', 'risk', 'weekly', 'index', FALSE, TRUE)
ON CONFLICT (series_id) DO UPDATE SET
    is_default = TRUE,
    updated_at = CURRENT_TIMESTAMP;

