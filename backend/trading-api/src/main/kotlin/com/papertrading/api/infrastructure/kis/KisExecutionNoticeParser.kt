package com.papertrading.api.infrastructure.kis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.TradingMode
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class KisExecutionNoticeParser {
    private val objectMapper = jacksonObjectMapper()
    private val kisDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")

    fun parse(rawMessage: String, mode: String): KisExecutionNotice? = runCatching {
        val fields = parseFields(rawMessage) ?: return null
        val channelId = fields["tr_id"] ?: fields["TR_ID"] ?: fields["channelId"] ?: return null
        val tradingMode = tradingMode(mode) ?: return null
        if (!isExpectedChannel(channelId, tradingMode)) return null

        val externalOrderId = fields.firstValue("ODNO", "odno", "ORNO") ?: return null
        val executionNo = fields.firstValue("CCLD_NO", "ccld_no", "EXECUTION_NO") ?: return null
        val ticker = fields.firstValue("PDNO", "pdno") ?: return null
        val qty = fields.firstValue("CCLD_QTY", "ccld_qty")?.toBigDecimalOrNull() ?: return null
        val price = fields.firstValue("CCLD_UNPR", "ccld_unpr", "CCLD_PRICE")?.toBigDecimalOrNull() ?: return null
        val sideCode = fields.firstValue("SLL_BUY_DVSN_CD", "sll_buy_dvsn_cd") ?: return null
        val executedAt = fields.firstValue("CCLD_DTTM", "ccld_dttm")
            ?.let(::parseExecutedAt) ?: return null
        val accountNumber = fields.firstValue("CANO", "cano")
        val accountProductCode = fields.firstValue("ACNT_PRDT_CD", "acnt_prdt_cd")

        KisExecutionNotice(
            mode = tradingMode,
            channelId = channelId,
            externalOrderId = externalOrderId,
            externalExecutionId = buildExternalExecutionId(
                mode = tradingMode,
                externalOrderId = externalOrderId,
                executionNo = executionNo,
                accountNumber = accountNumber,
                accountProductCode = accountProductCode,
            ),
            ticker = ticker,
            side = parseSide(sideCode) ?: return null,
            executedQty = qty,
            executedPrice = price,
            executedAt = executedAt,
            accountNumber = accountNumber,
            accountProductCode = accountProductCode,
        )
    }.getOrNull()

    private fun parseFields(rawMessage: String): Map<String, String>? {
        val trimmed = rawMessage.trim()
        if (trimmed.startsWith("{")) {
            return objectMapper.readValue<Map<String, Any?>>(trimmed)
                .mapValues { (_, value) -> value?.toString().orEmpty() }
        }

        val parts = trimmed.split("|", limit = 4)
        if (parts.size != 4) return null
        val channelId = parts[1]
        val body = parts[3]
        val fields = body.split("^")
            .mapNotNull {
                val keyValue = it.split("=", limit = 2)
                if (keyValue.size == 2) keyValue[0] to keyValue[1] else null
            }
            .toMap()
            .toMutableMap()
        fields["tr_id"] = channelId
        return fields
    }

    private fun tradingMode(mode: String): TradingMode? = when (mode.lowercase()) {
        "paper", "kis_paper" -> TradingMode.KIS_PAPER
        "live", "kis_live" -> TradingMode.KIS_LIVE
        else -> null
    }

    private fun isExpectedChannel(channelId: String, mode: TradingMode): Boolean =
        (mode == TradingMode.KIS_PAPER && channelId == "H0STCNI9") ||
            (mode == TradingMode.KIS_LIVE && channelId == "H0STCNI0")

    private fun parseSide(code: String): OrderSide? = when (code) {
        "02", "BUY" -> OrderSide.BUY
        "01", "SELL" -> OrderSide.SELL
        else -> null
    }

    private fun parseExecutedAt(raw: String): Instant {
        val local = LocalDateTime.parse(raw, kisDateTimeFormatter)
        return local.atZone(ZoneId.of("Asia/Seoul")).toInstant()
    }

    private fun buildExternalExecutionId(
        mode: TradingMode,
        externalOrderId: String,
        executionNo: String,
        accountNumber: String?,
        accountProductCode: String?,
    ): String {
        val accountScope = if (!accountNumber.isNullOrBlank() && !accountProductCode.isNullOrBlank()) {
            "$accountNumber-$accountProductCode"
        } else {
            null
        }
        return listOfNotNull(mode.name, accountScope, externalOrderId, executionNo).joinToString(":")
    }

    private fun Map<String, String>.firstValue(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { this[it]?.takeIf(String::isNotBlank) }
}
