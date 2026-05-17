package com.papertrading.api.domain.enums

enum class TriggerSkipReason {
    POSITION_CLOSED,
    SELL_ALREADY_LOCKED,
    NO_ORDERABLE_QUANTITY,
}
