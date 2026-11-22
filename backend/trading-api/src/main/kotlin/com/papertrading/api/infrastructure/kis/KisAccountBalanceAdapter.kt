package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerBalanceSnapshot
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.domain.enums.TradingMode
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * KIS 국내주식 잔고 어댑터 (D22): 공통 잔고 포트의 KIS 구현, trId 와 REST 주소 모드는 ref.tradingMode 가 정함
 * 계좌 연결 검사는 호출 서비스가 먼저 하고 여기서는 형식이 틀린 ref 만 막음, 응답 해석은 누락 시 0 채움 그대로 (가정 3)
 * 통화는 KRW 한 줄이고 매수 가능 금액은 이 API 에 없어 null
 */
@Component
class KisAccountBalanceAdapter(
    private val properties: KisOrderProperties,
    private val tokenManager: KisTokenManager,
    private val restTemplate: RestTemplate,
    private val clock: Clock = Clock.systemUTC(),
) : BrokerAccountBalancePort {
    override val supportedModes: Set<TradingMode> = setOf(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)

    override fun fetchBalance(account: BrokerAccountRef): BrokerBalanceSnapshot {
        val (mode, trId) = when (account.tradingMode) {
            TradingMode.KIS_PAPER -> "paper" to PAPER_TR_ID

            TradingMode.KIS_LIVE -> "live" to LIVE_TR_ID

            TradingMode.LOCAL, TradingMode.UPBIT_LIVE, TradingMode.TOSS_LIVE ->
                throw InternalErrorException(
                    "KIS_BALANCE_MODE_UNSUPPORTED",
                    "KIS 잔고 어댑터가 지원하지 않는 모드: ${account.tradingMode.name}",
                )
        }
        val (cano, acntPrdtCd) = kisAccountNumberParts(account.externalAccountId)

        val url = UriComponentsBuilder
            .fromHttpUrl("${properties.restBaseUrl(mode)}/uapi/domestic-stock/v1/trading/inquire-balance")
            .queryParam("CANO", cano)
            .queryParam("ACNT_PRDT_CD", acntPrdtCd)
            .queryParam("AFHR_FLPR_YN", "N")
            .queryParam("OFL_YN", "")
            .queryParam("INQR_DVSN", "01")
            .queryParam("UNPR_DVSN", "01")
            .queryParam("FUND_STTL_ICLD_YN", "N")
            .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
            .queryParam("PRCS_DVSN", "00")
            .queryParam("CTX_AREA_FK100", "")
            .queryParam("CTX_AREA_NK100", "")
            .toUriString()

        val response = try {
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Void>(kisHeaders(mode, trId)),
                Map::class.java,
            ).body
        } catch (ex: RestClientException) {
            throw KisFailureClassifier.transportFailure(ex)
        }

        ensureSuccess(response)
        val positions = outputList(response, "output1").mapNotNull { positionFrom(it) }
        val summary = outputMap(response, "output2")

        val marketValue = decimal(summary, "scts_evlu_amt")
        val unrealizedPnl = decimal(summary, "evlu_pfls_smtl_amt")
        val cashBalance = decimal(summary, "dnca_tot_amt")
        val returnRate = calculateReturnRate(summary, marketValue, unrealizedPnl)

        return BrokerBalanceSnapshot(
            asOf = LocalDateTime.ofInstant(clock.instant(), KST),
            currencyBalances = listOf(
                BrokerCurrencyBalance(
                    currency = KRW,
                    cashBalance = cashBalance,
                    cashBuyingPower = null,
                    marketValue = marketValue,
                    unrealizedPnl = unrealizedPnl,
                    returnRate = returnRate,
                ),
            ),
            positions = positions,
        )
    }

    private fun ensureSuccess(response: Map<*, *>?) {
        val rtCd = response?.get("rt_cd")?.toString()
        if (rtCd == "0") return
        val msgCd = response?.get("msg_cd")?.toString()
        throw KisFailureClassifier.balanceFailure(rtCd, msgCd, response?.get("msg1")?.toString().orEmpty())
    }

    private fun kisHeaders(mode: String, trId: String): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("authorization", "Bearer ${tokenManager.getToken(mode)}")
        set("appkey", properties.appKey(mode))
        set("appsecret", properties.appSecret(mode))
        set("tr_id", trId)
        set("custtype", "P")
    }

    private fun outputList(response: Map<*, *>?, key: String): List<Map<*, *>> =
        (response?.get(key) as? List<*>)?.filterIsInstance<Map<*, *>>().orEmpty()

    private fun outputMap(response: Map<*, *>?, key: String): Map<*, *> =
        (response?.get(key) as? List<*>)?.firstOrNull() as? Map<*, *> ?: emptyMap<String, Any>()

    private fun decimal(map: Map<*, *>, key: String): BigDecimal =
        map[key]?.toString()?.replace(",", "")?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun positionFrom(item: Map<*, *>): BrokerBalancePosition? {
        val ticker = item["pdno"]?.toString().orEmpty().trim()
        if (ticker.isBlank()) return null

        val quantity = decimal(item, "hldg_qty")
        val avgPrice = decimal(item, "pchs_avg_pric")
        val currentPrice = decimal(item, "prpr")
        val marketValue = decimal(item, "evlu_amt")
        val unrealizedPnl = decimal(item, "evlu_pfls_amt")
        val returnRate = if (item.containsKey("evlu_erng_rt")) {
            decimal(item, "evlu_erng_rt")
        } else {
            calculateReturnRate(avgPrice.multiply(quantity), marketValue, unrealizedPnl)
        }

        return BrokerBalancePosition(
            ticker = ticker,
            currency = KRW,
            quantity = quantity,
            avgPrice = avgPrice,
            currentPrice = currentPrice,
            marketValue = marketValue,
            unrealizedPnl = unrealizedPnl,
            returnRate = returnRate,
        )
    }

    private fun calculateReturnRate(summary: Map<*, *>, marketValue: BigDecimal, unrealizedPnl: BigDecimal): BigDecimal {
        val explicitRate = decimal(summary, "evlu_erng_rt")
        if (explicitRate.compareTo(BigDecimal.ZERO) != 0) return explicitRate
        return calculateReturnRate(marketValue.subtract(unrealizedPnl), marketValue, unrealizedPnl)
    }

    private fun calculateReturnRate(costBasis: BigDecimal, marketValue: BigDecimal, unrealizedPnl: BigDecimal): BigDecimal {
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return unrealizedPnl
            .divide(costBasis, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP)
    }

    companion object {
        private const val PAPER_TR_ID = "VTTC8434R"
        private const val LIVE_TR_ID = "TTTC8434R"
        private const val KRW = "KRW"
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
