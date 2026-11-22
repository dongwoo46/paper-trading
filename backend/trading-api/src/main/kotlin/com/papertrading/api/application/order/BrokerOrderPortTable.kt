package com.papertrading.api.application.order

import com.papertrading.api.application.order.port.BrokerOrderPort
import com.papertrading.api.domain.enums.TradingMode

/**
 * 모드 → 주문 포트 표 (D25), 포트의 supportedModes 로 만들고 한 모드에 구현이 둘이면 만들 때 실패
 * 잔고 쪽 BrokerBalancePortTable 과 같은 모양, 모드 이름을 메시지에 담아 기동 로그에서 어긋난 모드가 바로 보임
 */
class BrokerOrderPortTable(ports: List<BrokerOrderPort>) {
    private val byMode: Map<TradingMode, BrokerOrderPort>

    init {
        val owners = ports
            .flatMap { port -> port.supportedModes.map { it to port } }
            .groupBy({ it.first }, { it.second })
        val duplicated = owners.filterValues { it.size > 1 }.keys
        check(duplicated.isEmpty()) { "한 모드에 주문 포트 구현이 둘 이상입니다: ${duplicated.joinToString { it.name }}" }
        byMode = owners.mapValues { it.value.single() }
    }

    val modes: Set<TradingMode> get() = byMode.keys

    fun portFor(mode: TradingMode) = checkNotNull(byMode[mode]) { "주문 포트 구현이 없습니다: ${mode.name}" }
}
