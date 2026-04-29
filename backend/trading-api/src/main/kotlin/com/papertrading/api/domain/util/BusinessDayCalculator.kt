package com.papertrading.api.domain.util

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 영업일 계산 유틸리티
 * MVP: 주말(토·일)만 스킵. 공휴일은 미지원.
 * TODO: 공휴일 캘린더 연동 확장 포인트 (KRX 공휴일 API 또는 DB 관리)
 */
object BusinessDayCalculator {

    /**
     * 주어진 날짜에서 [days] 영업일(주말 제외) 후의 날짜를 반환한다.
     * @param date 기준 날짜
     * @param days 추가할 영업일 수 (0 이상)
     * @throws IllegalArgumentException days < 0
     */
    fun addBusinessDays(date: LocalDate, days: Int): LocalDate {
        require(days >= 0) { "days must be >= 0, but was: $days" }

        var result = date
        var remaining = days

        while (remaining > 0) {
            result = result.plusDays(1)
            if (result.dayOfWeek != DayOfWeek.SATURDAY && result.dayOfWeek != DayOfWeek.SUNDAY) {
                remaining--
            }
        }

        return result
    }
}
