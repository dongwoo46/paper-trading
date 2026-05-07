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
class TradingJournal protected constructor() : BaseAuditEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "order_id")
    var orderId: Long? = null
        private set

    @Column(name = "journal_type", nullable = false, length = 30)
    lateinit var journalType: String
        private set

    @Column(name = "ticker", length = 20)
    var ticker: String? = null
        private set

    @Column(name = "title", nullable = false, length = 200)
    lateinit var title: String
        private set

    @Column(name = "content", nullable = false, columnDefinition = "text")
    lateinit var content: String
        private set

    @Column(name = "sentiment", length = 20)
    var sentiment: String? = null
        private set

    fun updateContent(title: String, content: String) {
        require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
        require(content.isNotBlank()) { "내용은 비어 있을 수 없습니다." }
        this.title = title
        this.content = content
    }

    fun updateSentiment(sentiment: String?) {
        this.sentiment = sentiment
    }

    companion object {
        fun create(
            account: Account,
            journalType: String,
            title: String,
            content: String,
            orderId: Long? = null,
            ticker: String? = null,
            sentiment: String? = null
        ): TradingJournal = TradingJournal().apply {
            this.account = account
            this.orderId = orderId
            this.journalType = journalType
            this.ticker = ticker
            this.title = title
            this.content = content
            this.sentiment = sentiment
        }
    }
}
