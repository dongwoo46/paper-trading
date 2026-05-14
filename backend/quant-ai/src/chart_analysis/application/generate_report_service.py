"""GenerateReportService — SSE 스트림 + Redis Job + LLM 어댑터 호출 Application Service."""
from __future__ import annotations

import logging
from typing import AsyncIterator, Callable, Optional

from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from src.chart_analysis.domain.ports import LlmReportGenerator
from src.chart_analysis.domain.value_objects import NarrativeReport

logger = logging.getLogger(__name__)


class GenerateReportService:
    """LLM 자연어 리포트 SSE 스트림 서비스.

    흐름:
    1. DB에서 분석 결과 조회
    2. LLM 리포트 이미 있으면 즉시 반환
    3. 인기 종목이면 LLM 호출 (Redis 락으로 dedup)
    4. 비인기 종목이면 status:none 반환
    5. LLM 예외 → 룰 템플릿 폴백 + Slack failure 알림
    """

    def __init__(
        self,
        chart_analysis_repository,
        llm_report_generator: LlmReportGenerator,
        redis_job_store,
        popular_symbols_fn: Callable[[str], bool],
        slack_notifier,
        rule_template_generator: Optional[LlmReportGenerator] = None,
    ) -> None:
        self._repo = chart_analysis_repository
        self._llm = llm_report_generator
        self._redis = redis_job_store
        self._is_popular = popular_symbols_fn
        self._slack = slack_notifier
        if rule_template_generator is None:
            from src.chart_analysis.infrastructure.rule_template_report_generator import (
                RuleTemplateReportGenerator,
            )
            rule_template_generator = RuleTemplateReportGenerator()
        self._rule_template = rule_template_generator

    async def stream(
        self, symbol: str, window: str, interval: str
    ) -> AsyncIterator[dict]:
        """SSE 이벤트 비동기 제너레이터."""
        result = self._repo.find_one(symbol, window, interval)
        if result is None:
            yield {"event": "status", "data": {"window": window, "interval": interval, "stage": "none"}}
            yield {"event": "end", "data": {}}
            return

        # 시나리오 1: DB에 LLM 리포트 이미 존재
        if result.report is not None:
            yield {"event": "status", "data": {"window": window, "interval": interval, "stage": "running"}}
            yield {"event": "report", "data": _serialize_report(result.report, window, interval)}
            yield {"event": "end", "data": {}}
            return

        # 시나리오 3: 비인기 종목
        if not self._is_popular(symbol):
            yield {"event": "status", "data": {"window": window, "interval": interval, "stage": "none"}}
            yield {"event": "end", "data": {}}
            return

        # 시나리오 2 / 4: 인기 종목 + 리포트 없음 → LLM 호출 시도 (Redis 락)
        snapshot_hash = result.snapshot_hash
        lock_acquired = self._redis.acquire_lock(snapshot_hash, ttl=600)

        if not lock_acquired:
            # 시나리오 4: 다른 요청이 락 보유 → pending 상태로 end
            yield {"event": "status", "data": {"window": window, "interval": interval, "stage": "pending"}}
            yield {"event": "end", "data": {}}
            return

        try:
            yield {"event": "status", "data": {"window": window, "interval": interval, "stage": "running"}}

            report, llm_succeeded = await self._invoke_llm(result, symbol, window, interval)
            source = report.source.value

            # DB 저장
            updated = result.with_report(report, report.source)
            self._repo.upsert(updated)

            yield {"event": "report", "data": _serialize_report(report, window, interval)}
            yield {"event": "end", "data": {}}

            # Slack 성공 알림 (LLM 성공 시에만)
            if llm_succeeded:
                await self._slack.notify_analysis_success(symbol, window, source=source)

        finally:
            self._redis.release_lock(snapshot_hash)

    async def _invoke_llm(
        self,
        result: ChartAnalysisResult,
        symbol: str,
        window: str,
        interval: str,
    ) -> tuple[NarrativeReport, bool]:
        """LLM 호출, 실패 시 룰 템플릿 폴백.

        Returns:
            (report, llm_succeeded) — llm_succeeded=False이면 폴백 사용
        """
        try:
            report = self._llm.generate(snapshot=None, result=result)
            return report, True
        except Exception as exc:  # noqa: BLE001
            logger.warning(
                "generate_report:llm_error symbol=%s window=%s error=%s",
                symbol, window, str(exc),
            )
            # Slack 실패 알림
            await self._slack.notify_analysis_failure(symbol, window, error=str(exc))
            # 룰 템플릿 폴백
            fallback = self._rule_template.generate(snapshot=None, result=result)
            return fallback, False


def _serialize_report(report: NarrativeReport, window: str, interval: str) -> dict:
    return {
        "window": window,
        "interval": interval,
        "source": report.source.value,
        "narrative": {
            "trend_section": report.trend_section,
            "support_resistance_section": report.support_resistance_section,
            "entry_plan_section": report.entry_plan_section,
            "signal_evidence_section": report.signal_evidence_section,
            "risk_section": report.risk_section,
        },
    }