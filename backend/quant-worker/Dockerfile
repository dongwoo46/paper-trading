FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src src/
COPY api_main.py main.py ./
EXPOSE 8082
CMD ["uvicorn", "api_main:app", "--host", "0.0.0.0", "--port", "8082"]
