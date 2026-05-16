package me.pixka.pos.report.service

import me.pixka.pos.food.model.Food
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.repository.KitchenRepository
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.table.model.PosTable
import me.pixka.pos.table.repository.TableRepository
import me.pixka.pos.zone.model.Zone
import me.pixka.pos.zone.repository.ZoneRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyReportServiceTest {
    @Autowired
    private lateinit var dailyReportService: DailyReportService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var tableRepository: TableRepository

    @Autowired
    private lateinit var foodRepository: FoodRepository

    @Autowired
    private lateinit var zoneRepository: ZoneRepository

    @Autowired
    private lateinit var kitchenRepository: KitchenRepository

    @Autowired
    private lateinit var foodCategoryRepository: FoodCategoryRepository

    private lateinit var table: PosTable
    private lateinit var food: Food

    @BeforeEach
    fun clearData() {
        orderRepository.deleteAll()
        foodRepository.deleteAll()
        tableRepository.deleteAll()
        kitchenRepository.deleteAll()
        foodCategoryRepository.deleteAll()
        zoneRepository.deleteAll()

        val zone = zoneRepository.save(Zone(code = "ZN-R1", name = "Z"))
        table = tableRepository.save(PosTable(code = "TB-R1", basePrice = 0.0, zone = zone))
        val kitchen = kitchenRepository.save(Kitchen(code = "KT-R1", name = "K"))
        val cat = foodCategoryRepository.save(FoodCategory(code = "CAT-R1"))
        food = foodRepository.save(
            Food(
                code = "FD-R1",
                name = "F",
                basePrice = 10.0,
                kitchen = kitchen,
                foodCategory = cat,
            ),
        )
    }

    @Test
    fun `daily report includes paid order on settlement day and totals`() {
        val d = LocalDate.of(2026, 5, 10)
        val orderDate = d.atTime(9, 0)
        val paidAt = d.atTime(14, 30)
        val order = PosOrder(
            orderNo = "ORD-R1",
            paidPrice = 50.0,
            change = 10.0,
            table = table,
            orderDate = orderDate,
            complateOrder = true,
            complateOrderDate = paidAt,
            cancel = false,
            paid = true,
            paidAt = paidAt,
        )
        order.lines.clear()
        val line = me.pixka.pos.order.model.OrderLine(
            order = order,
            food = food,
            quantity = 2,
            note = null,
            unitPrice = 10.0,
            status = me.pixka.pos.order.model.OrderLineStatus.COMPLETE,
        )
        order.lines.add(line)
        orderRepository.save(order)

        val report = dailyReportService.buildDailyCashReport(d, d)

        assertEquals(d, report.startDate)
        assertEquals(d, report.endDate)
        assertEquals(1, report.paidOrderCount)
        assertEquals(0, report.paidByQrScanOrderCount)
        assertEquals(0, report.paidByCreditOrderCount)
        assertEquals(20.0, report.totalSales)
        assertEquals(50.0, report.totalCashReceived)
        assertEquals(10.0, report.totalChange)
        assertEquals(1, report.rows.size)
        assertEquals("ORD-R1", report.rows[0].orderNo)
        assertEquals(false, report.rows[0].paidByQrScan)
        assertEquals(false, report.rows[0].paidByCredit)
        assertEquals(1, report.foods.size)
        assertEquals("FD-R1", report.foods[0].foodCode)
        assertEquals(2, report.foods[0].quantity)
        assertEquals(20.0, report.foods[0].total)
    }

    @Test
    fun `daily report excludes paid order paid on another day`() {
        val day1 = LocalDate.of(2026, 6, 1)
        val day2 = LocalDate.of(2026, 6, 2)
        val order = PosOrder(
            orderNo = "ORD-R2",
            paidPrice = 10.0,
            change = 0.0,
            table = table,
            orderDate = day1.atTime(12, 0),
            complateOrder = true,
            complateOrderDate = day2.atTime(8, 0),
            cancel = false,
            paid = true,
            paidAt = day2.atTime(8, 0),
        )
        order.lines.clear()
        order.lines.add(
            me.pixka.pos.order.model.OrderLine(
                order = order,
                food = food,
                quantity = 1,
                unitPrice = 10.0,
                status = me.pixka.pos.order.model.OrderLineStatus.COMPLETE,
            ),
        )
        orderRepository.save(order)

        val r1 = dailyReportService.buildDailyCashReport(day1, day1)
        assertEquals(0, r1.paidOrderCount)
        assertEquals(0, r1.rows.size)
        assertEquals(0, r1.foods.size)

        val r2 = dailyReportService.buildDailyCashReport(day1, day2)
        assertEquals(1, r2.paidOrderCount)
        assertEquals(10.0, r2.totalSales)
        assertEquals(1, r2.foods.size)
        assertEquals(10.0, r2.foods[0].total)
    }

    @Test
    fun `daily report flags paid by QR scan`() {
        val d = LocalDate.of(2026, 5, 20)
        val order = PosOrder(
            orderNo = "ORD-QR",
            paidPrice = 100.0,
            change = 0.0,
            table = table,
            orderDate = d.atTime(10, 0),
            complateOrder = true,
            complateOrderDate = d.atTime(15, 0),
            cancel = false,
            paid = true,
            paidAt = d.atTime(15, 0),
            paidByQrScan = true,
        )
        order.lines.clear()
        order.lines.add(
            me.pixka.pos.order.model.OrderLine(
                order = order,
                food = food,
                quantity = 1,
                unitPrice = 100.0,
                status = me.pixka.pos.order.model.OrderLineStatus.COMPLETE,
            ),
        )
        orderRepository.save(order)

        val report = dailyReportService.buildDailyCashReport(d, d)
        assertEquals(1, report.paidOrderCount)
        assertEquals(1, report.paidByQrScanOrderCount)
        assertEquals(0, report.paidByCreditOrderCount)
        assertEquals(1, report.rows.size)
        assertEquals(true, report.rows[0].paidByQrScan)
        assertEquals(false, report.rows[0].paidByCredit)
    }

    @Test
    fun `daily report flags paid by credit and omits from cash totals`() {
        val d = LocalDate.of(2026, 5, 21)
        val order = PosOrder(
            orderNo = "ORD-CR",
            paidPrice = 100.0,
            change = 0.0,
            table = table,
            orderDate = d.atTime(10, 0),
            complateOrder = true,
            complateOrderDate = d.atTime(16, 0),
            cancel = false,
            paid = true,
            paidAt = d.atTime(16, 0),
            paidByCredit = true,
        )
        order.lines.clear()
        order.lines.add(
            me.pixka.pos.order.model.OrderLine(
                order = order,
                food = food,
                quantity = 1,
                unitPrice = 100.0,
                status = me.pixka.pos.order.model.OrderLineStatus.COMPLETE,
            ),
        )
        orderRepository.save(order)

        val report = dailyReportService.buildDailyCashReport(d, d)
        assertEquals(1, report.paidOrderCount)
        assertEquals(0, report.paidByQrScanOrderCount)
        assertEquals(1, report.paidByCreditOrderCount)
        assertEquals(100.0, report.totalSales)
        assertEquals(0.0, report.totalCashReceived)
        assertEquals(0.0, report.totalChange)
        assertEquals(true, report.rows[0].paidByCredit)
        assertEquals(false, report.rows[0].paidByQrScan)
    }
}
