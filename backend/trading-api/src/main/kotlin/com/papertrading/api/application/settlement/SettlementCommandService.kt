package com.papertrading.api.application.settlement

import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SettlementCommandService(
    private val pendingSettlementRepository: PendingSettlementRepository,
    private val settlementProcessor: SettlementProcessor,
) {
    private val log = KotlinLogging.logger {}

    /**
     * targetDate 이하 PENDING 정산을 일괄 처리한다.
     *
     * @Transactional 제거: 각 건은 SettlementProcessor.processOne 이 REQUIRES_NEW 로 처리.
     * 루프 자체에는 트랜잭션이 불필요하며, 공유 트랜잭션이 존재하면 한 건의 DB 예외가
     * EntityManager를 rollback-only 오염시켜 배치 전체를 롤백시키는 문제를 방지한다.
     *
     * @return 성공 처리 건수
     */
    fun processSettlements(targetDate: LocalDate): Int {
        val pendings = pendingSettlementRepository
            .findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)

        var successCount = 0
        for (ps in pendings) {
            runCatching { settlementProcessor.processOne(ps) }
                .onSuccess { successCount++ }
                .onFailure { ex ->
                    log.warn { "정산 처리 실패: pendingSettlementId=${ps.id}, reason=${ex.message}" }
                }
        }
        return successCount
    }

    /**
     * 단건 정산 재처리. 예외는 호출자에게 전파된다.
     */
    @Transactional
    fun processSettlement(pendingSettlementId: Long) {
        val ps = pendingSettlementRepository.findById(pendingSettlementId)
            .orElseThrow { NoSuchElementException("PendingSettlement을 찾을 수 없습니다. id=$pendingSettlementId") }
        settlementProcessor.processOne(ps)
    }
}
