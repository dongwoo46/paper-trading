package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.port.BrokerOrderPort
import com.papertrading.api.common.exception.BrokerOrderNotSupportedException
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.TradingMode
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 계좌 모드로 주문 포트를 골라 브로커에 접수하고 외부 주문번호 기록, 로그, Slack 알림 (D26, 옛 KisPaperOrderExecutor·KisLiveOrderExecutor)
 * 어느 모드를 포트·501·내부 오류로 보낼지는 전수 when, 생성 때 그 결과와 구현 표를 맞춰 보아 어긋나면 기동 실패 (D25)
 * 로그·Slack 문구는 옛 실행기와 같은 글자(모드 이름만 치환, 특성화 시험이 단언), 실패 예외는 그대로 올려 주문 트랜잭션 롤백
 */
@Service
class BrokerOrderSubmitter(
    orderPorts: List<BrokerOrderPort>,
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val log = KotlinLogging.logger {}
    private val portTable = BrokerOrderPortTable(orderPorts)

    init {
        val routedToPort = TradingMode.entries.filter { routeOf(it) == Route.BROKER_PORT }.toSet()
        val missing = routedToPort - portTable.modes
        val unrouted = portTable.modes - routedToPort
        check(missing.isEmpty()) { "포트로 보내는 모드에 주문 포트 구현이 없습니다: ${missing.names()}" }
        check(unrouted.isEmpty()) { "포트로 보내지 않는 모드를 주문 포트 구현이 지원합니다: ${unrouted.names()}" }
    }

    // 옛 실행기 submit 과 같은 경계 (호출하는 주문 트랜잭션에 참여)
    @Transactional
    fun submit(order: Order) {
        val mode = order.account.tradingMode
        when (routeOf(mode)) {
            Route.BROKER_PORT -> submitToBroker(mode, order)

            Route.NOT_SUPPORTED -> throw BrokerOrderNotSupportedException(mode.name)

            Route.NOT_BROKER_ORDER -> throw InternalErrorException(
                "BROKER_ORDER_MODE_INVALID",
                "브로커 주문 대상이 아닌 모드: ${mode.name}",
            )
        }
    }

    private fun submitToBroker(mode: TradingMode, order: Order) {
        val orderId = order.id ?: return
        val accountId = order.account.id ?: return
        val ticker = order.ticker ?: "UNKNOWN"
        runCatching {
            val orno = portTable.portFor(mode).submit(order).externalOrderId
            order.assignExternalOrderId(orno)
            log.info { "${mode.name} 주문 접수: orderId=$orderId, orno=$orno, ticker=${order.ticker}" }
            slackNotificationEventPublisher.publishOrderCreated(
                accountId = accountId,
                orderId = orderId,
                message = "${mode.name} order created: orderId=$orderId, ticker=$ticker, externalOrderId=$orno",
            )
        }.onFailure {
            log.error(it) { "${mode.name} 주문 접수 실패: orderId=$orderId, reason=${it.message}" }
            throw it
        }
    }

    private fun Set<TradingMode>.names(): String = joinToString { it.name }

    private enum class Route { BROKER_PORT, NOT_SUPPORTED, NOT_BROKER_ORDER }

    // 모드 분기의 유일한 자리, else 없이 전수로 둠
    private fun routeOf(mode: TradingMode): Route = when (mode) {
        TradingMode.KIS_PAPER, TradingMode.KIS_LIVE -> Route.BROKER_PORT
        TradingMode.TOSS_LIVE, TradingMode.UPBIT_LIVE -> Route.NOT_SUPPORTED
        TradingMode.LOCAL -> Route.NOT_BROKER_ORDER
    }
}
