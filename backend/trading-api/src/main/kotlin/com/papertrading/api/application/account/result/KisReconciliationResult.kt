package com.papertrading.api.application.account.result

data class KisReconciliationResult(
    val missingInLocal: List<String>,
    val missingInKis: List<String>,
    val quantityMismatch: List<String>
)
