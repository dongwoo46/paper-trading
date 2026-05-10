# Step 5 — Summary 작성 및 PR 생성

- 담당: orchestrator
- 상태: pending
- 전제: step-4 완료 (Must-Fix 없음) 확인 후 진행

---

## 처리 순서

### 1. 최종 빌드 검증

아래 커맨드를 순서대로 실행하고 모두 통과해야 한다.

```
cd backend/quant-worker

# 신규 파일 컴파일 확인
python -m py_compile src/collectors/investor_flow_collector.py
python -m py_compile src/repositories/investor_flow_repository.py
python -m py_compile src/application/investor_flow_fetch_service.py
python -m py_compile src/jobs/investor_flow_schedule.py

# 전체 테스트
pytest tests/ -v --tb=short

# 린트
ruff check src/collectors/investor_flow_collector.py
ruff check src/repositories/investor_flow_repository.py
ruff check src/application/investor_flow_fetch_service.py
ruff check src/jobs/investor_flow_schedule.py
```

실패 시: quant-dev에게 원인 전달 후 step-2 재실행.

---

### 2. 문서 상태 업데이트

아래 파일을 순서대로 업데이트한다.

1. `docs/phase/quant-worker/investor-flow-pipeline/index.json`
   - 모든 step의 status → completed
   - current_step → 5
   - updated → 오늘 날짜

2. `docs/done/quant-worker/investor-flow-pipeline/investor-flow-pipeline-summary.md` 작성
   - 구현 목적 (1-2문장)
   - 신규 파일 목록 (절대경로)
   - 수정 파일 목록
   - 테이블 목록 (4개)
   - 엔드포인트 목록 (4개)
   - 배치 정의 요약 (2개, 실행 시각)
   - 트레이드오프 핵심 결정 3가지

3. `docs/phase/quant-worker/investor-flow-pipeline/` 폴더를 `docs/done/quant-worker/investor-flow-pipeline/`로 이동
   - summary.md는 done 폴더 내부에 유지

4. `docs/TODO.md`에서 investor-flow-pipeline 항목 `[x]` 마킹

5. `docs/state.md` 업데이트
   - active_feature: null 또는 다음 피처로 전환

---

### 3. Git 커밋

커밋 메시지 작성 규칙: 한국어, 아래 형식

- `feat(quant-worker): 투자자별 매매동향 파이프라인 구현 (investor-flow-pipeline)`

스테이징 대상:
- `backend/quant-worker/src/collectors/investor_flow_collector.py`
- `backend/quant-worker/src/repositories/investor_flow_repository.py`
- `backend/quant-worker/src/application/investor_flow_fetch_service.py`
- `backend/quant-worker/src/jobs/investor_flow_schedule.py`
- `backend/quant-worker/src/interfaces/api/app.py`
- `backend/quant-worker/src/migrations/V{next}__create_investor_flow_tables.sql`
- `backend/quant-worker/tests/` (신규 테스트 파일)
- `docs/done/quant-worker/investor-flow-pipeline/` (summary.md 포함)
- `docs/TODO.md`
- `docs/state.md`

---

### 4. PR 생성

브랜치: `feature/quant-worker-investor-flow-pipeline` → `main`

PR 제목: `feat(quant-worker): 투자자별 매매동향/공매도/프로그램매매/외국인보유 파이프라인 추가`

PR 본문 포함 항목:
- 기능 요약 (수집 대상 4개 데이터셋)
- 신규 파일 목록
- 배치 스케줄 (19:00 KST 월-금, KOSPI/KOSDAQ 분리)
- REST API 엔드포인트 4개
- Decimal 안전성 및 upsert 멱등성 적용 사실
- 테스트 커버리지 요약

PR 생성 커맨드:
```
gh pr create \
  --title "feat(quant-worker): 투자자별 매매동향/공매도/프로그램매매/외국인보유 파이프라인 추가" \
  --body "..." \
  --base main \
  --head feature/quant-worker-investor-flow-pipeline
```

---

## 완료 기준

- 빌드·테스트·린트 모두 통과
- summary.md 작성 완료
- docs/ 상태 파일 전부 갱신
- PR 생성 완료 및 URL 반환