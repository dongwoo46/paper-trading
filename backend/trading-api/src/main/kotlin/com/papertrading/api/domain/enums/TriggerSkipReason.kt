package com.papertrading.api.domain.enums

enum class TriggerSkipReason {
    POSITION_CLOSED,
    LOCK_CONFLICT,
    MANUAL_SELL_CONFLICT,
    SELL_ALREADY_LOCKED,
    NO_ORDERABLE_QUANTITY,
}
