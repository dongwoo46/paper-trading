-- V1__create_investor_flow_tables.sql
-- 투자자별 매매동향, 공매도, 프로그램 매매, 외국인 보유현황 테이블 생성

-- 1. investor_flow: 투자자별 매매동향
CREATE TABLE IF NOT EXISTS investor_flow (
    id                          BIGSERIAL PRIMARY KEY,
    trade_date                  DATE NOT NULL,
    symbol                      VARCHAR(20) NOT NULL,
    market                      VARCHAR(10) NOT NULL,

    -- 외국인
    foreign_buy_volume          BIGINT NOT NULL,
    foreign_sell_volume         BIGINT NOT NULL,
    foreign_net_volume          BIGINT NOT NULL,
    foreign_buy_amount          NUMERIC(20,0) NOT NULL,
    foreign_sell_amount         NUMERIC(20,0) NOT NULL,
    foreign_net_amount          NUMERIC(20,0) NOT NULL,

    -- 기관 합계
    institution_buy_volume      BIGINT NOT NULL,
    institution_sell_volume     BIGINT NOT NULL,
    institution_net_volume      BIGINT NOT NULL,
    institution_buy_amount      NUMERIC(20,0) NOT NULL,
    institution_sell_amount     NUMERIC(20,0) NOT NULL,
    institution_net_amount      NUMERIC(20,0) NOT NULL,

    -- 금융투자
    finance_invest_buy_volume   BIGINT NOT NULL,
    finance_invest_sell_volume  BIGINT NOT NULL,
    finance_invest_net_volume   BIGINT NOT NULL,
    finance_invest_buy_amount   NUMERIC(20,0) NOT NULL,
    finance_invest_sell_amount  NUMERIC(20,0) NOT NULL,
    finance_invest_net_amount   NUMERIC(20,0) NOT NULL,

    -- 보험
    insurance_buy_volume        BIGINT NOT NULL,
    insurance_sell_volume       BIGINT NOT NULL,
    insurance_net_volume        BIGINT NOT NULL,
    insurance_buy_amount        NUMERIC(20,0) NOT NULL,
    insurance_sell_amount       NUMERIC(20,0) NOT NULL,
    insurance_net_amount        NUMERIC(20,0) NOT NULL,

    -- 투신
    trust_buy_volume            BIGINT NOT NULL,
    trust_sell_volume           BIGINT NOT NULL,
    trust_net_volume            BIGINT NOT NULL,
    trust_buy_amount            NUMERIC(20,0) NOT NULL,
    trust_sell_amount           NUMERIC(20,0) NOT NULL,
    trust_net_amount            NUMERIC(20,0) NOT NULL,

    -- 사모
    private_equity_buy_volume   BIGINT NOT NULL,
    private_equity_sell_volume  BIGINT NOT NULL,
    private_equity_net_volume   BIGINT NOT NULL,
    private_equity_buy_amount   NUMERIC(20,0) NOT NULL,
    private_equity_sell_amount  NUMERIC(20,0) NOT NULL,
    private_equity_net_amount   NUMERIC(20,0) NOT NULL,

    -- 은행
    bank_buy_volume             BIGINT NOT NULL,
    bank_sell_volume            BIGINT NOT NULL,
    bank_net_volume             BIGINT NOT NULL,
    bank_buy_amount             NUMERIC(20,0) NOT NULL,
    bank_sell_amount            NUMERIC(20,0) NOT NULL,
    bank_net_amount             NUMERIC(20,0) NOT NULL,

    -- 기타금융
    other_finance_buy_volume    BIGINT NOT NULL,
    other_finance_sell_volume   BIGINT NOT NULL,
    other_finance_net_volume    BIGINT NOT NULL,
    other_finance_buy_amount    NUMERIC(20,0) NOT NULL,
    other_finance_sell_amount   NUMERIC(20,0) NOT NULL,
    other_finance_net_amount    NUMERIC(20,0) NOT NULL,

    -- 연기금
    pension_buy_volume          BIGINT NOT NULL,
    pension_sell_volume         BIGINT NOT NULL,
    pension_net_volume          BIGINT NOT NULL,
    pension_buy_amount          NUMERIC(20,0) NOT NULL,
    pension_sell_amount         NUMERIC(20,0) NOT NULL,
    pension_net_amount          NUMERIC(20,0) NOT NULL,

    -- 개인
    individual_buy_volume       BIGINT NOT NULL,
    individual_sell_volume      BIGINT NOT NULL,
    individual_net_volume       BIGINT NOT NULL,
    individual_buy_amount       NUMERIC(20,0) NOT NULL,
    individual_sell_amount      NUMERIC(20,0) NOT NULL,
    individual_net_amount       NUMERIC(20,0) NOT NULL,

    collected_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_investor_flow UNIQUE (trade_date, symbol, market)
);

CREATE INDEX IF NOT EXISTS idx_investor_flow_symbol_date
    ON investor_flow (symbol, trade_date DESC);

CREATE INDEX IF NOT EXISTS idx_investor_flow_date_market
    ON investor_flow (trade_date, market);


-- 2. short_selling: 공매도 거래 현황
CREATE TABLE IF NOT EXISTS short_selling (
    id                      BIGSERIAL PRIMARY KEY,
    trade_date              DATE NOT NULL,
    symbol                  VARCHAR(20) NOT NULL,
    market                  VARCHAR(10) NOT NULL,
    short_sell_volume       BIGINT NOT NULL,
    short_sell_amount       NUMERIC(20,0) NOT NULL,
    short_sell_ratio        NUMERIC(8,4) NOT NULL,
    short_balance_volume    BIGINT NOT NULL,
    short_balance_amount    NUMERIC(20,0) NOT NULL,
    collected_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_short_selling UNIQUE (trade_date, symbol, market)
);

CREATE INDEX IF NOT EXISTS idx_short_selling_symbol_date
    ON short_selling (symbol, trade_date DESC);

CREATE INDEX IF NOT EXISTS idx_short_selling_date_market
    ON short_selling (trade_date, market);


-- 3. program_trading: 프로그램 매매 현황
CREATE TABLE IF NOT EXISTS program_trading (
    id                      BIGSERIAL PRIMARY KEY,
    trade_date              DATE NOT NULL,
    symbol                  VARCHAR(20) NOT NULL,
    market                  VARCHAR(10) NOT NULL,
    arb_buy_volume          BIGINT NOT NULL,
    arb_sell_volume         BIGINT NOT NULL,
    arb_net_volume          BIGINT NOT NULL,
    arb_buy_amount          NUMERIC(20,0) NOT NULL,
    arb_sell_amount         NUMERIC(20,0) NOT NULL,
    arb_net_amount          NUMERIC(20,0) NOT NULL,
    non_arb_buy_volume      BIGINT NOT NULL,
    non_arb_sell_volume     BIGINT NOT NULL,
    non_arb_net_volume      BIGINT NOT NULL,
    non_arb_buy_amount      NUMERIC(20,0) NOT NULL,
    non_arb_sell_amount     NUMERIC(20,0) NOT NULL,
    non_arb_net_amount      NUMERIC(20,0) NOT NULL,
    collected_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_program_trading UNIQUE (trade_date, symbol, market)
);

CREATE INDEX IF NOT EXISTS idx_program_trading_symbol_date
    ON program_trading (symbol, trade_date DESC);

CREATE INDEX IF NOT EXISTS idx_program_trading_date_market
    ON program_trading (trade_date, market);


-- 4. foreign_holding: 외국인 보유 한도 및 소진율
CREATE TABLE IF NOT EXISTS foreign_holding (
    id                  BIGSERIAL PRIMARY KEY,
    trade_date          DATE NOT NULL,
    symbol              VARCHAR(20) NOT NULL,
    market              VARCHAR(10) NOT NULL,
    holding_limit       BIGINT NOT NULL,
    holding_volume      BIGINT NOT NULL,
    exhaustion_ratio    NUMERIC(8,4) NOT NULL,
    collected_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_foreign_holding UNIQUE (trade_date, symbol, market)
);

CREATE INDEX IF NOT EXISTS idx_foreign_holding_symbol_date
    ON foreign_holding (symbol, trade_date DESC);

CREATE INDEX IF NOT EXISTS idx_foreign_holding_date_market
    ON foreign_holding (trade_date, market);
