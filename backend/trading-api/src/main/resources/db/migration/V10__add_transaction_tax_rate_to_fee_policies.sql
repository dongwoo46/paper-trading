ALTER TABLE fee_policies ADD COLUMN transaction_tax_rate NUMERIC(10,6) NOT NULL DEFAULT 0;

-- LOCAL 모드: 한국 거래소 세율 설정
-- KOSPI/KOSDAQ: 0.18% (증권거래세)
UPDATE fee_policies
SET transaction_tax_rate = 0.001800
WHERE trading_mode = 'LOCAL' AND market_type IN ('KOSPI', 'KOSDAQ');

-- LOCAL 모드: 해외 시장 (NASDAQ, NYSE 등)은 한국 증권거래세 없음
UPDATE fee_policies
SET transaction_tax_rate = 0.000000
WHERE trading_mode = 'LOCAL' AND market_type NOT IN ('KOSPI', 'KOSDAQ');

-- KIS_PAPER / KIS_LIVE: KIS 측에서 세금 처리하므로 0
UPDATE fee_policies
SET transaction_tax_rate = 0.000000
WHERE trading_mode IN ('KIS_PAPER', 'KIS_LIVE');