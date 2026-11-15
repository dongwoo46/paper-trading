-- 돈 열 확장: NUMERIC(20,4) -> NUMERIC(28,12)
-- 근거: 수량 scale 8 x 가격 scale 4 = scale 12, 정수 자리 16 유지(20-4 = 28-12)라 기존 값은 잘리지 않음
-- 적용: 한 트랜잭션으로(psql -1 -f). 열 타입 변경은 테이블을 다시 쓰므로 점검 시간대에 실행
-- 주의: 이 저장소는 Flyway 를 쓰지 않고 JPA ddl-auto 로 스키마를 만든다. 이 파일은 기존 DB 에 수동 적용하는 스크립트이고
--       엔티티 @Column(precision = 28, scale = 12) 과 같은 정의를 유지한다

ALTER TABLE accounts
    ALTER COLUMN deposit TYPE NUMERIC(28, 12),
    ALTER COLUMN available_deposit TYPE NUMERIC(28, 12),
    ALTER COLUMN locked_deposit TYPE NUMERIC(28, 12);

ALTER TABLE account_ledger
    ALTER COLUMN amount TYPE NUMERIC(28, 12),
    ALTER COLUMN balance_after TYPE NUMERIC(28, 12);

ALTER TABLE orders
    ALTER COLUMN total_amount TYPE NUMERIC(28, 12),
    ALTER COLUMN locked_amount TYPE NUMERIC(28, 12);

ALTER TABLE settlements
    ALTER COLUMN realized_pnl TYPE NUMERIC(28, 12),
    ALTER COLUMN net_pnl TYPE NUMERIC(28, 12),
    ALTER COLUMN krw_net_pnl TYPE NUMERIC(28, 12);

ALTER TABLE positions
    ALTER COLUMN total_buy_amount TYPE NUMERIC(28, 12);
