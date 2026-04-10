package com.papertrading.api.infrastructure.kis

import com.fasterxml.jackson.annotation.JsonProperty
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.model.Order
import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * KIS 주문 REST 클라이언트
 * 주문 접수 / 취소 / 체결 조회 API 호출.
 * Account.externalAccountId 형식: "XXXXXXXX-XX" (계좌번호 8자리 + "-" + 상품코드 2자리)
 */
@Component
class KisOrderRestClient(
    private val properties: KisOrderProperties,
    private val tokenManager: KisTokenManager,
    private val restTemplate: RestTemplate,
) {
    private val log = KotlinLogging.logger {}

    // TR_ID 매핑
    private fun buyTrId(mode: String) = if (mode == "paper") "VTTC0802U" else "TTTC0802U"
    private fun sellTrId(mode: String) = if (mode == "paper") "VTTC0801U" else "TTTC0801U"
    private fun cancelTrId(mode: String) = if (mode == "paper") "VTTT1004U" else "TTTT1004U"
    private fun ccldTrId(mode: String) = if (mode == "paper") "VTTC8001R" else "TTTC8001R"

    /** 주문 접수 → 외부 주문번호(ORNO) 반환 */
    fun placeOrder(order: Order, mode: String): String {
        val (cano, acntPrdtCd) = parseExternalAccountId(order)
        val trId = if (order.orderSide == OrderSide.BUY) buyTrId(mode) else sellTrId(mode)
        val ordDvsn = if (order.orderType == OrderType.MARKET) "01" else "00"
        val ordUnpr = if (order.orderType == OrderType.MARKET) "0"
                      else (order.limitPrice ?: BigDecimal.ZERO).toPlainString()

        val body = mapOf(
            "CANO" to cano,
            "ACNT_PRDT_CD" to acntPrdtCd,
            "PDNO" to (order.ticker ?: ""),
            "ORD_DVSN" to ordDvsn,
            "ORD_QTY" to order.quantity.toPlainString(),
            "ORD_UNPR" to ordUnpr,
        )

        val headers = kisHeaders(mode, trId)
        val response = restTemplate.exchange(
            "${properties.restBaseUrl(mode)}/uapi/domestic-stock/v1/trading/order-cash",
            HttpMethod.POST,
            HttpEntity(body, headers),
            OrderResponse::class.java,
        ).body

        check(response?.rtCd == "0") { "KIS 주문 접수 실패: ${response?.msg}" }
        return requireNotNull(response?.output?.orno) { "ORNO(주문번호) null" }
    }

    /** 주문 취소 */
    fun cancelOrder(order: Order, mode: String) {
        val (cano, acntPrdtCd) = parseExternalAccountId(order)
        val externalOrderId = requireNotNull(order.externalOrderId) { "externalOrderId null" }

        val body = mapOf(
            "CANO" to cano,
            "ACNT_PRDT_CD" to acntPrdtCd,
            "KRX_FWDG_ORD_ORGNO" to "",
            "ORGN_ORNO" to externalOrderId,
            "ORD_DVSN" to "00",
            "RVSE_CNCL_DVSN_CD" to "02", // 02 = 취소
            "ORD_QTY" to order.quantity.subtract(order.filledQuantity).toPlainString(),
            "ORD_UNPR" to "0",
            "QTY_ALL_ORD_YN" to "Y",
        )

        val trId = cancelTrId(mode)
        val response = restTemplate.exchange(
            "${properties.restBaseUrl(mode)}/uapi/domestic-stock/v1/trading/order-rvsecncl",
            HttpMethod.POST,
            HttpEntity(body, kisHeaders(mode, trId)),
            OrderResponse::class.java,
        ).body

        check(response?.rtCd == "0") { "KIS 주문 취소 실패: ${response?.msg}" }
    }

    /** 체결 조회 → 오늘 날짜 체결 내역 반환 */
    fun inquireFills(order: Order, mode: String): List<FillResult> {
        val (cano, acntPrdtCd) = parseExternalAccountId(order)
        val externalOrderId = order.externalOrderId ?: return emptyList()
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)

        val url = UriComponentsBuilder
            .fromHttpUrl("${properties.restBaseUrl(mode)}/uapi/domestic-stock/v1/trading/inquire-daily-ccld")
            .queryParam("CANO", cano)
            .queryParam("ACNT_PRDT_CD", acntPrdtCd)
            .queryParam("INQR_STRT_DT", today)
            .queryParam("INQR_END_DT", today)
            .queryParam("SLL_BUY_DVSN_CD", "00") // 00 = 전체
            .queryParam("INQR_DVSN", "00")
            .queryParam("PDNO", "")
            .queryParam("CCLD_DVSN", "01") // 01 = 체결
            .queryParam("ORD_GNO_BRNO", "")
            .queryParam("ODNO", externalOrderId)
            .queryParam("INQR_DVSN_3", "00")
            .queryParam("INQR_DVSN_1", "")
            .queryParam("CTX_AREA_FK100", "")
            .queryParam("CTX_AREA_NK100", "")
            .toUriString()

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<Void>(kisHeaders(mode, ccldTrId(mode))),
            FillInquiryResponse::class.java,
        ).body

        if (response?.rtCd != "0") return emptyList()

        return (response.output1 ?: emptyList()).map { item ->
            FillResult(
                externalOrderId = item.orno ?: "",
                executedQty = item.totCcldQty?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                executedPrice = item.avgPrvs?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                isFullyFilled = (item.psblQty?.toBigDecimalOrNull() ?: BigDecimal.ONE) == BigDecimal.ZERO,
            )
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

    private fun parseExternalAccountId(order: Order): Pair<String, String> {
        val raw = requireNotNull(order.account?.externalAccountId) {
            "account.externalAccountId is null (orderId=${order.id})"
        }
        // 형식: "XXXXXXXX-XX"
        val parts = raw.split("-")
        require(parts.size == 2) {
            "externalAccountId 형식이 잘못되었습니다. expected: XXXXXXXX-XX, actual: $raw (orderId=${order.id})"
        }
        return Pair(parts[0], parts[1])
    }

    // DTO

    data class FillResult(
        val externalOrderId: String,
        val executedQty: BigDecimal,
        val executedPrice: BigDecimal,
        val isFullyFilled: Boolean,
    )

    private data class OrderResponse(
        @JsonProperty("rt_cd") val rtCd: String?,
        @JsonProperty("msg1") val msg: String?,
        @JsonProperty("output") val output: OrderOutput?,
    )

    private data class OrderOutput(
        @JsonProperty("ORNO") val orno: String?,
    )

    private data class FillInquiryResponse(
        @JsonProperty("rt_cd") val rtCd: String?,
        @JsonProperty("output1") val output1: List<FillItem>?,
    )

    private data class FillItem(
        @JsonProperty("ODNO") val orno: String?,
        @JsonProperty("TOT_CCLD_QTY") val totCcldQty: String?,
        @JsonProperty("AVG_PRVS") val avgPrvs: String?,
        @JsonProperty("PSBL_QTY") val psblQty: String?,
    )
}