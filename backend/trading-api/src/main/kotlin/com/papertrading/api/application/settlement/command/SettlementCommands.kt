package com.papertrading.api.application.settlement.command

import java.time.LocalDate

data class ProcessSettlementsCommand(
    val targetDate: LocalDate,
)

data class ProcessSettlementCommand(
    val ReceivableSettlementId: Long,
)


