# FRED 선택 기본 후보 지표 추가

## 목적/배경
- 필수 코어 지표 외에 전략 확장 시 자주 쓰는 지표를 catalog 기본 후보로 제공하기 위함.
- 기본 후보로만 넣고, 자동 구독은 하지 않기 위함.

## 변경 범위
- DB:
  - `V12__add_fred_optional_defaults.sql` 추가
  - 대상 지표:
    - `TEDRATE`, `DGS3MO`, `RECPROUSM156N`, `USSLIND`
    - `DEXKOUS`, `DEXJPUS`
    - `DCOILWTICO`, `MHHNGSP`
    - `MORTGAGE30US`, `DRCCLACBS`, `EXCSRESNS`
  - 정책:
    - 신규 insert 시 `enabled=false`, `is_default=true`
    - 기존 데이터는 `is_default=true`만 업데이트(구독 상태 유지)

## 검증
- 실행 명령: `.\gradlew.bat compileKotlin`
- 결과: 성공

## 후속 TODO
- 프론트에서 `is_default` 필터(기본 후보만 보기) 추가
- 코어/선택 후보 구분 배지 표시

