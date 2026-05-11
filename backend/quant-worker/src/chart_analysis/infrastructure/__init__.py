"""차트 분석 인프라 계층 — pandas-ta/scipy 기반 계산 모듈.

각 클래스는 개별 모듈에서 직접 임포트하거나 여기서 임포트한다.
"""

__all__ = [
    "PandasTaIndicatorCalculator",
    "ScipyPeakSupportResistanceFinder",
    "RuleBasedPatternDetector",
    "MaAdxTrendClassifier",
    "WeightedRuleConfidenceScorer",
]


def __getattr__(name: str):
    """지연 임포트 — 순환 임포트 방지."""
    _map = {
        "PandasTaIndicatorCalculator": (
            ".indicator_calculator", "PandasTaIndicatorCalculator"
        ),
        "ScipyPeakSupportResistanceFinder": (
            ".support_resistance_finder", "ScipyPeakSupportResistanceFinder"
        ),
        "RuleBasedPatternDetector": (
            ".pattern_detector", "RuleBasedPatternDetector"
        ),
        "MaAdxTrendClassifier": (
            ".trend_classifier", "MaAdxTrendClassifier"
        ),
        "WeightedRuleConfidenceScorer": (
            ".confidence_scorer", "WeightedRuleConfidenceScorer"
        ),
    }
    if name in _map:
        module_path, class_name = _map[name]
        import importlib
        mod = importlib.import_module(module_path, package=__name__)
        return getattr(mod, class_name)
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
