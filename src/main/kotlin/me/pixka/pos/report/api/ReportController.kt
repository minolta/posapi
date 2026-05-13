package me.pixka.pos.report.api

import me.pixka.pos.report.service.DailyReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val dailyReportService: DailyReportService,
) {
    /**
     * Single day: `?date=yyyy-MM-dd`. Inclusive range: `?startDate=&endDate=`.
     * When only one of `startDate` / `endDate` is sent, that day is used for both ends.
     */
    @GetMapping("/daily")
    fun daily(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?,
    ): DailyReportResponse {
        val (start, end) = resolveRange(date = date, startDate = startDate, endDate = endDate)
        return dailyReportService.buildDailyCashReport(start, end)
    }

    companion object {
        fun resolveRange(
            date: LocalDate?,
            startDate: LocalDate?,
            endDate: LocalDate?,
        ): Pair<LocalDate, LocalDate> =
            when {
                date != null -> Pair(date, date)
                startDate != null && endDate != null ->
                    if (endDate.isBefore(startDate)) {
                        Pair(endDate, startDate)
                    } else {
                        Pair(startDate, endDate)
                    }

                startDate != null -> Pair(startDate, startDate)
                endDate != null -> Pair(endDate, endDate)
                else -> Pair(LocalDate.now(), LocalDate.now())
            }
    }
}
