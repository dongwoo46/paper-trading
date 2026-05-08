# PR 준비 체크리스트 - market-indicators-api

## 스펙 준수 테이블
| 항목 | 요구사항 | 결과 |
|---|---|---|
| Intervals | `1m/5m/10m/1d/1w` | PASS |
| Period modes | `limit` 또는 `from~to`, 미입력 시 `limit=200` | PASS |
| Indicators | `bb,rsi,macd` 조합 지원 | PASS |
| Missing policy | `null_until_window_ready` | PASS |
| Error contract | `400/404/422` + stable codes | PASS |
| Weekly fallback | `1w` 소스 부재 시 daily compose | PASS |

## 검증 커맨드 결과
- `cd backend/collector-api && ./gradlew.bat test` -> BUILD SUCCESSFUL
- `cd backend/collector-api && ./gradlew.bat compileKotlin` -> BUILD SUCCESSFUL

## 롤백 계획
- 문제 발생 시 feature branch를 배포 대상에서 제외하고 이전 collector-api 배포 아티팩트로 롤백
- DB 스키마 변경 없음(읽기 계산형 API)으로 데이터 마이그레이션 롤백 불필요

## 운영 리스크 노트
- Redis intraday bar 희소 구간에서 422 비율이 증가할 수 있으므로 API 에러율 모니터링 필요
- weekly fallback 로직 경로는 동작하나 `1w + limit` 분기 전용 테스트를 후속 보강 권장
- Gradle 9 호환성 관련 deprecation 경고가 있어 빌드 스크립트 추후 정리 필요

