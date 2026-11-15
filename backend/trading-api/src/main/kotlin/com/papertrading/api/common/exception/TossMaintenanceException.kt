package com.papertrading.api.common.exception

/** J — 토스가 점검 중(`maintenance`), 재시도하지 않고 점검 종료 후 다시 호출 */
class TossMaintenanceException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_MAINTENANCE",
        "토스가 점검 중입니다. 사유=$reason",
        cause,
    )
