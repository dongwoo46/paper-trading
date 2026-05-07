package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 트레이딩 기록 (매매 일지)
 * 주문·종목에 대한 사용자 메모·감상·매매 근거를 자유 형식으로 기록.
 * sentiment: BULLISH | BEARISH | NEUTRAL — 당시 심리 태그.
 */
@Entity
@Table(name = "trading_journals")
class TradingJournal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "order_id")
    var orderId: Long? = null,

    @Column(name = "journal_type", nullable = false, length = 30)
    var journalType: String? = null,

    @Column(name = "ticker", length = 20)
    var ticker: String? = null,

    @Column(name = "title", nullable = false, length = 200)
    var title: String? = null,

    @Column(name = "content", nullable = false, columnDefinition = "text")
    var content: String? = null,

    @Column(name = "sentiment", length = 20)
    var sentiment: String? = null
) : BaseAuditEntity()

{
    fun update(
        title: String,
        content: String,
        sentiment: String?
    ) {
        require(title.isNotBlank()) { "title은 비어 있을 수 없습니다." }
        require(content.isNotBlank()) { "content는 비어 있을 수 없습니다." }
        this.title = title
        this.content = content
        this.sentiment = sentiment?.uppercase()
    }

    fun belongsTo(accountId: Long): Boolean = account?.id == accountId

    companion object {
        fun create(
            account: Account,
            journalType: String,
            title: String,
            content: String,
            orderId: Long?,
            ticker: String?,
            sentiment: String?
        ): TradingJournal {
            require(journalType.isNotBlank()) { "journalType은 비어 있을 수 없습니다." }
            require(title.isNotBlank()) { "title은 비어 있을 수 없습니다." }
            require(content.isNotBlank()) { "content는 비어 있을 수 없습니다." }
            return TradingJournal(
                account = account,
                orderId = orderId,
                journalType = journalType.trim().uppercase(),
                ticker = ticker?.trim()?.uppercase(),
                title = title.trim(),
                content = content.trim(),
                sentiment = sentiment?.trim()?.uppercase()
            )
        }
    }
}
