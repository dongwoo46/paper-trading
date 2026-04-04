# market-collector

KIS/Upbit 기반 실시간 데이터 수집 백엔드입니다.  
현재는 KIS WebSocket/REST 구독 관리, 심볼 검색, Flyway 기반 스키마 관리까지 구성되어 있습니다.

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

기본 연결 정보:
- PostgreSQL: `localhost:5432`, DB `paper`, USER/PW `paper/paper`
- Redis: `localhost:6379`

## 주요 설정

- `spring.r2dbc.*`: 앱 R2DBC 연결
- `spring.datasource.*`: Flyway JDBC 연결
- `spring.flyway.*`: 마이그레이션 실행 설정
- `collector.source.kis.*`: KIS websocket/rest 수집 설정

중요:
- `collector.source.kis.modes`는 `paper`, `live`를 동시에 지원
- `collector.source.kis.max-realtime-registrations=41`

## API

### KIS WebSocket 구독
- `GET /api/kis/ws/subscriptions`
- `POST /api/kis/ws/subscriptions`
- `DELETE /api/kis/ws/subscriptions`

요청 바디:
```json
{ "mode": "paper", "symbol": "005930" }
```

### KIS REST 관심종목
- `GET /api/kis/rest/watchlist`
- `POST /api/kis/rest/watchlist`
- `DELETE /api/kis/rest/watchlist`

### 국내 심볼 검색
- `GET /api/symbols/kr?query=삼성&market=KOSPI&limit=50`

## DB 마이그레이션

- `V1__create_kis_subscription_tables.sql`
- `V2__create_kr_symbol_table.sql` (테이블 + 국내 심볼 seed 100개)

## 프론트엔드

통합 UI는 `frontend/trading-web`에서 운영합니다.
- WS/REST 구독 관리
- `paper/live` 분리 표시
- 국내 심볼 검색 연동

