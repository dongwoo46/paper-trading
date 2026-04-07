# FRED 핵심 구독 세트 고정

## 목적/배경
- 투자용 핵심 매크로 지표만 우선 수집 대상으로 고정하고, 이후 필요 시 수동으로 확장/축소하기 위함.

## 변경 범위
- DB:
  - `V11__set_fred_core_default_subscriptions.sql` 추가
  - 대상 23개 series를 `enabled=true`, `is_default=true`로 설정
  - catalog에 없는 series는 최소 메타로 추가
- API:
  - 기존 `selections/subscriptions` 추가/삭제 API를 그대로 사용하여 수동 커스터마이징 가능

## 검증
- 실행 명령: `.\gradlew.bat compileKotlin`
- 결과: 성공

## 후속 TODO
- FRED 카탈로그 동기화 후 title/category/frequency/units를 원본 메타로 정규화 확인
- 프론트에서 기본세트/사용자세트 구분 표시 추가

