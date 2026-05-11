"""LLM 요청 큐 처리 스크립트 — 운영자 수동 실행.

사용법:
    python -m scripts.process_llm_request_queue [--limit N]

동작:
1. analysis_request_queue에서 status='pending' row를 requested_at ASC 정렬로 N개 fetch.
2. row별 LLM 호출 → 성공 시 chart_analysis_result.llm_report 업데이트 + status='completed'.
3. 실패 시 status='failed', processed_at 기록.
"""
from __future__ import annotations

import argparse
import logging
import sys
from datetime import datetime, timezone

logger = logging.getLogger(__name__)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="LLM 요청 큐에서 pending row를 처리하여 LLM 리포트를 생성한다."
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=50,
        help="처리할 최대 row 수 (기본: 50)",
    )
    return parser.parse_args(argv)


def process_queue(
    limit: int,
    queue_repo,
    chart_repo,
    llm_generator,
) -> dict:
    """큐 처리 로직 (테스트에서 직접 호출 가능).

    Args:
        limit: 처리할 최대 row 수
        queue_repo: AnalysisRequestQueueRepository (fetch_pending, mark_processed)
        chart_repo: ChartAnalysisRepository (find_one, upsert)
        llm_generator: LlmReportGenerator (generate)

    Returns:
        {"processed": int, "completed": int, "failed": int}
    """
    items = queue_repo.fetch_pending(limit)
    completed = 0
    failed = 0

    for item in items:
        symbol = item.symbol
        window = item.window
        interval = item.interval
        item_id = item.id
        now = datetime.now(timezone.utc)

        # 선점: 다른 프로세스가 같은 row를 중복 처리하지 않도록 즉시 processing으로 갱신
        queue_repo.mark_processed(item_id, "processing")

        # 분석 결과 조회
        result = chart_repo.find_one(symbol, window, interval)
        if result is None:
            logger.warning(
                "process_queue:result_not_found id=%d symbol=%s window=%s interval=%s",
                item_id, symbol, window, interval,
            )
            queue_repo.mark_processed(item_id, "failed")
            failed += 1
            continue

        # LLM 호출
        try:
            narrative = llm_generator.generate(snapshot=None, result=result)
            updated = result.with_report(narrative, narrative.source)
            chart_repo.upsert(updated)
            queue_repo.mark_processed(item_id, "completed")
            completed += 1
            logger.info(
                "process_queue:completed id=%d symbol=%s window=%s",
                item_id, symbol, window,
            )
        except Exception as exc:  # noqa: BLE001
            logger.warning(
                "process_queue:llm_failed id=%d symbol=%s window=%s error=%s",
                item_id, symbol, window, str(exc),
            )
            queue_repo.mark_processed(item_id, "failed")
            failed += 1

    processed = completed + failed
    logger.info(
        "process_queue:summary processed=%d completed=%d failed=%d",
        processed, completed, failed,
    )
    return {"processed": processed, "completed": completed, "failed": failed}


def _build_dependencies():
    """환경변수로 어댑터 생성 (운영 실행용)."""
    from src.chart_analysis.infrastructure.analysis_request_queue_repository import (
        PostgresAnalysisRequestQueueRepository,
    )
    from src.chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from src.chart_analysis.infrastructure.langchain_ollama_report_generator import (
        LangChainOllamaReportGenerator,
    )
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    db = load_db_config_from_env()
    connect_fn = lambda: connect(db)  # noqa: E731

    return (
        PostgresAnalysisRequestQueueRepository(connect_fn),
        PostgresChartAnalysisRepository(connect_fn),
        LangChainOllamaReportGenerator(),
    )


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )
    args = parse_args(argv)
    queue_repo, chart_repo, llm_generator = _build_dependencies()
    result = process_queue(
        limit=args.limit,
        queue_repo=queue_repo,
        chart_repo=chart_repo,
        llm_generator=llm_generator,
    )
    print(
        f"processed={result['processed']} "
        f"completed={result['completed']} "
        f"failed={result['failed']}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())