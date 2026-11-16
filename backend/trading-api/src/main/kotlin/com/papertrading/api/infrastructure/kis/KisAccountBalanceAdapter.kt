package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.account.port.KisAccountBalancePort
import com.papertrading.api.application.account.port.KisBalancePosition
import com.papertrading.api.application.account.port.KisBalanceSnapshot
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.common.exception.KisAuthFailedException
import com.papertrading.api.common.exception.KisPermissionDeniedException
import com.papertrading.api.common.exception.KisRateLimitedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.KisUnavailableException
import com.papertrading.api.infrastructure.persistence.AccountRepository
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class KisAccountBalanceAdapter(
    private val accountRepository: AccountRepository,
    private val properties: KisOrderProperties,
    private val tokenManager: KisTokenManager,
    private val restTemplate: RestTemplate,
) : KisAccountBalancePort {
    override fun fetchBalance(accountId: Long, trId: String): KisBalanceSnapshot {
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }
        val externalAccountId = account.externalAccountId ?: throw KisAccountNotLinkedException()
        val (cano, acntPrdtCd) = parseExternalAccountId(externalAccountId)
        val mode = if (trId == PAPER_TR_ID) "paper" else "live"

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
        } catch (ex: ResourceAccessException) {
            throw KisTimeoutException(ex)
        } catch (ex: RestClientException) {
            throw KisUnavailableException(ex)
        }

        ensureSuccess(response)
        val positions = outputList(response, "output1").mapNotNull { positionFrom(it) }
        val summary = outputMap(response, "output2")

        val marketValue = decimal(summary, "scts_evlu_amt")
        val unrealizedPnl = decimal(summary, "evlu_pfls_smtl_amt")
        val cashBalance = decimal(summary, "dnca_tot_amt")
        val returnRate = calculateReturnRate(summary, marketValue, unrealizedPnl)

        return KisBalanceSnapshot(
            asOf = OffsetDateTime.now(ZoneOffset.UTC),
            cashBalance = cashBalance,
            marketValue = marketValue,
            unrealizedPnl = unrealizedPnl,
            returnRate = returnRate,
            positions = positions
        )
    }

    private fun ensureSuccess(response: Map<*, *>?) {
        val rtCd = response?.get("rt_cd")?.toString()
        if (rtCd == "0") return
        val message = response?.get("msg1")?.toString().orEmpty()
        throw when {
            rtCd == "1" || message.contains("인증") || message.contains("token", ignoreCase = true) ->
                KisAuthFailedException(KisApiException(KisErrorCode.KIS_AUTH_FAILED, rtCd, message))
            rtCd == "7" || message.contains("권한") ->
                KisPermissionDeniedException(KisApiException(KisErrorCode.KIS_AUTH_FAILED, rtCd, message))
            // 가정 D28: 한도 문구만 KIS_RATE_LIMITED, 나머지 rt_cd 실패는 KIS_UNAVAILABLE
            else -> KisApiException(mapKisErrorCode(rtCd, message), rtCd, message).let { cause ->
                if (cause.code == KisErrorCode.KIS_RATE_LIMITED) {
                    KisRateLimitedException(cause)
                } else {
                    KisUnavailableException(cause)
                }
            }
        }
    }

    private fun kisHeaders(mode: String, trId: String): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("authorization", "Bearer ${tokenManager.getToken(mode)}")
        set("appkey", properties.appKey(mode))
        set("appsecret", properties.appSecret(mode))
        set("tr_id", trId)
        set("custtype", "P")
    }

    private fun parseExternalAccountId(value: String): Pair<String, String> {
        val parts = value.split("-")
        if (!(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank())) {
            throw KisAccountNotLinkedException()
        }
        return parts[0] to parts[1]
    }

    private fun outputList(response: Map<*, *>?, key: String): List<Map<*, *>> =
        (response?.get(key) as? List<*>)?.filterIsInstance<Map<*, *>>().orEmpty()

    private fun outputMap(response: Map<*, *>?, key: String): Map<*, *> =
        (response?.get(key) as? List<*>)?.firstOrNull() as? Map<*, *> ?: emptyMap<String, Any>()

    private fun decimal(map: Map<*, *>, key: String): BigDecimal =
        map[key]?.toString()?.replace(",", "")?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun positionFrom(item: Map<*, *>): KisBalancePosition? {
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

        return KisBalancePosition(
            ticker = ticker,
            quantity = quantity,
            avgPrice = avgPrice,
            currentPrice = currentPrice,
            marketValue = marketValue,
            unrealizedPnl = unrealizedPnl,
            returnRate = returnRate
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
    }
}
