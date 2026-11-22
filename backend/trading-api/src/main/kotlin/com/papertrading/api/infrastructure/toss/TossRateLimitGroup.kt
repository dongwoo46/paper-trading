package com.papertrading.api.infrastructure.toss

/**
 * 토스 호출 한도 그룹, 이름은 공식 명세의 `Rate Limits Group` 값
 * 초당 한도 수치는 설정(trading.toss.rate-limits.<그룹>)이고 코드에 두지 않음
 */
enum class TossRateLimitGroup {
    AUTH,
    ACCOUNT,
    ASSET,
    ORDER_INFO,
    MARKET_DATA,
    MARKET_DATA_CHART,
}
