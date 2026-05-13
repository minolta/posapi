package me.pixka.pos.report.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReportControllerResolveRangeTest {

    @Test
    fun `date param wins when present`() {
        val day = LocalDate.of(2026, 1, 15)
        val (a, b) = ReportController.resolveRange(date = day, startDate = LocalDate.of(2020, 1, 1), endDate = LocalDate.of(2020, 1, 2))
        assertEquals(day, a)
        assertEquals(day, b)
    }

    @Test
    fun `swaps when end before start`() {
        val s = LocalDate.of(2026, 2, 10)
        val e = LocalDate.of(2026, 2, 1)
        val (a, b) = ReportController.resolveRange(date = null, startDate = s, endDate = e)
        assertEquals(e, a)
        assertEquals(s, b)
    }

    @Test
    fun `defaults to today when no params`() {
        val (a, b) = ReportController.resolveRange(date = null, startDate = null, endDate = null)
        assertEquals(LocalDate.now(), a)
        assertEquals(LocalDate.now(), b)
    }
}
