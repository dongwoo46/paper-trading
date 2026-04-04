# market-collector Codex Common Settings

## 현재 기준
- 백엔드: KIS websocket/rest 분리, Flyway 적용, `kr_symbol` seed 적용
- 프론트: `frontend/trading-web`에 Market Collector UI 통합

## 폴더 기준
```text
market-collector/
  .codex/
    README.md
    code/CODE_STYLE.md
    feature/collection-targets.md
  src/main/kotlin/com/papertrading/collector/
    api/
      kis/
      kis/dto/
    common/
      redis/
    source/
      kis/config/
      kis/rest/
      kis/ws/
      upbit/
    storage/
      kis/
    pipeline/
    runtime/
  src/main/resources/
    application.yaml
    db/migration/
      V1__create_kis_subscription_tables.sql
      V2__create_kr_symbol_table.sql
```

## 운영 규칙
- feature 상태는 `.codex/feature/collection-targets.md`에서 관리
- 상태 흐름: `TODO -> IN_PROGRESS -> DONE`
- 스키마 변경은 Flyway migration으로만 반영

