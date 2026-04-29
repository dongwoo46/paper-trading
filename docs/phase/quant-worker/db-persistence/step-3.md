# step-3: 빌드 검증

## 목표
모든 변경 파일 syntax 오류 없음 확인 + 테스트 최종 Green 확인.

## 검증 명령 (quant-worker 디렉토리 기준)

```bash
# 구문 검증
python -m py_compile src/repositories/market_daily_ohlcv_repository.py
python -m py_compile src/jobs/catalog_daily_fetch_job.py
python -m py_compile src/application/daily_fetch_service.py
python -m py_compile src/interfaces/api/app.py

# 테스트 전체 실행
python -m pytest tests/ -v
```

## 완료 조건
- 구문 오류 0
- 테스트 전체 PASSED
- 실패 시 완료 선언 금지
