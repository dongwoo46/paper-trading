package com.papertrading.api.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class BusinessDayCalculatorTest {

    @Test
    fun `월요일에서 +2 영업일 = 수요일`() {
        val monday = LocalDate.of(2024, 1, 8) // Monday
        val result = BusinessDayCalculator.addBusinessDays(monday, 2)
        assertEquals(LocalDate.of(2024, 1, 10), result) // Wednesday
    }

    @Test
    fun `금요일에서 +2 영업일 = 다음주 화요일`() {
        val friday = LocalDate.of(2024, 1, 12) // Friday
        val result = BusinessDayCalculator.addBusinessDays(friday, 2)
        assertEquals(LocalDate.of(2024, 1, 16), result) // Next Tuesday
    }

    @Test
    fun `목요일에서 +2 영업일 = 다음주 월요일`() {
        val thursday = LocalDate.of(2024, 1, 11) // Thursday
        val result = BusinessDayCalculator.addBusinessDays(thursday, 2)
        assertEquals(LocalDate.of(2024, 1, 15), result) // Next Monday
    }

    @Test
    fun `0일 추가 시 같은 날짜 반환`() {
        val date = LocalDate.of(2024, 1, 10) // Wednesday
        val result = BusinessDayCalculator.addBusinessDays(date, 0)
        assertEquals(date, result)
    }

    @Test
    fun `음수 days 입력 시 IllegalArgumentException 발생`() {
        val date = LocalDate.of(2024, 1, 10)
        assertThrows<IllegalArgumentException> {
            BusinessDayCalculator.addBusinessDays(date, -1)
        }
    }
}
