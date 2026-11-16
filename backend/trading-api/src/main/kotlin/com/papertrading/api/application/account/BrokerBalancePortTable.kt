package com.papertrading.api.application.account

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.domain.enums.TradingMode

/**
 * 모드 → 잔고 포트 표 (D25), 포트의 supportedModes 로 만들고 한 모드에 구현이 둘이면 만들 때 실패
 * 모드 이름을 메시지에 담아 기동 로그에서 어느 모드가 어긋났는지 바로 보이게 함
 */
class BrokerBalancePortTable(ports: List<BrokerAccountBalancePort>) {
    private val byMode: Map<TradingMode, BrokerAccountBalancePort>

    init {
        val owners = ports
            .flatMap { port -> port.supportedModes.map { it to port } }
            .groupBy({ it.first }, { it.second })
        val duplicated = owners.filterValues { it.size > 1 }.keys
        check(duplicated.isEmpty()) { "한 모드에 잔고 포트 구현이 둘 이상입니다: ${duplicated.joinToString { it.name }}" }
        byMode = owners.mapValues { it.value.single() }
    }

    val modes: Set<TradingMode> get() = byMode.keys

    fun portFor(mode: TradingMode) = checkNotNull(byMode[mode]) { "잔고 포트 구현이 없습니다: ${mode.name}" }
}
