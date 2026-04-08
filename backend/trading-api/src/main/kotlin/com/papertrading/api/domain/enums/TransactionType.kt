package com.papertrading.api.domain.enums

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    BUY_LOCK,
    BUY_UNLOCK,
    BUY_EXECUTE,
    SELL_EXECUTE,
    FEE,
    TAX,
    SETTLEMENT
}
