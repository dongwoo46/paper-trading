package com.papertrading.api.domain.enums

enum class AccountSource {
    KIS,
    LOCAL,
    TOSS,
    UPBIT,
    ;

    companion object {
        // 거래 모드 → 응답 출처의 유일한 표, else 없이 전수로 둠(새 모드는 컴파일이 알림)
        fun fromTradingMode(mode: TradingMode): AccountSource = when (mode) {
            TradingMode.LOCAL -> LOCAL
            TradingMode.KIS_PAPER, TradingMode.KIS_LIVE -> KIS
            TradingMode.TOSS_LIVE -> TOSS
            TradingMode.UPBIT_LIVE -> UPBIT
        }
    }
}
