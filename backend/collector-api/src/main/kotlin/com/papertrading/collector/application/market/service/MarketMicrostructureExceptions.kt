package com.papertrading.collector.application.market.service

class SymbolNotFoundOrNoDataException : RuntimeException("SYMBOL_NOT_FOUND_OR_NO_DATA")

class InsufficientDataForRsException : RuntimeException("INSUFFICIENT_DATA_FOR_RS")
