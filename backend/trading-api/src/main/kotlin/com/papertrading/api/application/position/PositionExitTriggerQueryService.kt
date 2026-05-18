package com.papertrading.api.application.position

import com.papertrading.api.application.position.result.PositionExitTriggerListResult
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 포지션에 직접 등록된 청산 트리거 목록을 조회한다. 계정 기본값은 이 API에 병합하지 않는다.
@Service
class PositionExitTriggerQueryService(
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
) {
    @Transactional(readOnly = true)
    fun listPositionTriggers(positionId: Long): PositionExitTriggerListResult {
        if (!positionRepository.existsById(positionId)) {
            throw PositionNotFoundException(positionId = positionId)
        }
        val triggers = positionExitTriggerRepository.findAllByPositionIdOrderByIdAsc(positionId)
            .map(PositionExitTriggerResult::from)
        return PositionExitTriggerListResult(positionId, triggers)
    }
}
