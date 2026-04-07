# 재발 방지 규칙 — collector-worker

## 규칙 1: PostgreSQL 환경변수 미설정

- **증상**: `main.py` 실행 시 `PG_HOST` 관련 런타임 에러 발생
- **재현 조건**: DB 환경변수 없이 배치 실행
- **원인**: 필수 환경변수(`PG_HOST`, `PG_DATABASE`, `PG_USER`, `PG_PASSWORD`) 미설정
- **해결**: `.env` 파일 또는 환경변수 사전 설정
- **재발 방지 체크리스트**:
  - [ ] 배치 실행 전 환경변수 유효성 검사 코드 추가
  - [ ] `.env.example` 파일에 필수 변수 모두 기재

---

## 규칙 2: yfinance AttributeError (date 타입 중복 호출)

- **증상**: yfinance 수집 중 `AttributeError` 발생
- **재현 조건**: `datetime.date` 객체에 `.date()` 중복 호출
- **원인**: `date` 타입 연산 시 타입 혼용
- **해결**: `datetime.timedelta` 사용, 타입 명시
- **재발 방지 체크리스트**:
  - [ ] date/datetime 타입 변환 시 타입 assertion 추가
  - [ ] 날짜 계산 로직에 단위 테스트 추가

---

## 규칙 3: yfinance MultiIndex 컬럼 처리 누락

- **증상**: yfinance 백필 시 `date` 키 오류로 수집 실패
- **재현 조건**: 다중 종목 동시 조회 시 MultiIndex 컬럼 반환
- **원인**: yfinance 응답이 MultiIndex 컬럼일 수 있는데 단일 컬럼만 가정
- **해결**: 응답 정규화 단계에서 MultiIndex 컬럼을 먼저 평탄화(flatten)
- **재발 방지 체크리스트**:
  - [ ] yfinance 응답 정규화 함수에 MultiIndex 처리 로직 추가
  - [ ] 단일/다중 종목 조회 시나리오 테스트 추가