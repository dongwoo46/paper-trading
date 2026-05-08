package com.papertrading.collector.application.market.service

class SymbolNotFoundOrNoBarsException : RuntimeException("SYMBOL_NOT_FOUND_OR_NO_BARS")

class InsufficientBarsForRequestedRangeException : RuntimeException("INSUFFICIENT_BARS_FOR_REQUESTED_RANGE")

