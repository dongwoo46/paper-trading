# Backend Run Guide

## collector-worker 실행

### 1) 폴더 이동
```bash
cd ~/Desktop/paper-trading/backend/collector-worker
```

### 2) 가상환경 활성화
```bash
source .venv/Scripts/activate
```

### 3) API 서버 실행 (.env 사용)
```bash
python -m uvicorn api_main:app --env-file .env --host 0.0.0.0 --port 8000
```

### 4) 서버 확인
```bash
curl http://localhost:8000/health
```

정상 응답:
```json
{"status":"ok"}
```
