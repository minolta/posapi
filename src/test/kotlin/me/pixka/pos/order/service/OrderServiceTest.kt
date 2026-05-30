package me.pixka.pos.order.service

import me.pixka.pos.auth.model.User
import me.pixka.pos.auth.model.UserRole
import me.pixka.pos.auth.repository.UserRepository
import me.pixka.pos.food.model.Food
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.repository.KitchenRepository
import me.pixka.pos.order.api.OrderLineRequest
import me.pixka.pos.order.api.OrderRequest
import me.pixka.pos.order.api.PayOrderRequest
import me.pixka.pos.order.exception.OrderAlreadyPaidException
import me.pixka.pos.order.exception.OrderNotFoundException
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.table.model.PosTable
import me.pixka.pos.table.repository.TableRepository
import me.pixka.pos.zone.model.Zone
import me.pixka.pos.zone.repository.ZoneRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {
    @Autowired
    private lateinit var orderService: OrderService

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

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var table: PosTable
    private lateinit var food: Food
    private lateinit var drink: Food
    private lateinit var staffUser: User

    @BeforeEach
    fun clearData() {
        orderRepository.deleteAll()
        foodRepository.deleteAll()
        tableRepository.deleteAll()
        kitchenRepository.deleteAll()
        foodCategoryRepository.deleteAll()
        zoneRepository.deleteAll()
        userRepository.deleteAll()

        val zone = zoneRepository.save(
            Zone(
                code = "ZN-001",
                name = "Test zone"
            )
        )
        table = tableRepository.save(
            PosTable(
                code = "TB-001",
                basePrice = 0.0,
                zone = zone
            )
        )
        val kitchen = kitchenRepository.save(
            Kitchen(code = "KT-001", name = "Test kitchen")
        )
        val foodCategory = foodCategoryRepository.save(
            FoodCategory(code = "CAT-001")
        )
        food = foodRepository.save(
            Food(
                code = "FD-001",
                name = "Test food",
                basePrice = 10.0,
                kitchen = kitchen,
                foodCategory = foodCategory
            )
        )
        drink = foodRepository.save(
            Food(
                code = "DR-001",
                name = "Test drink",
                basePrice = 3.0,
                kitchen = kitchen,
                foodCategory = foodCategory
            )
        )
        staffUser = userRepository.save(
            User(
                username = "cashier1",
                passwordHash = "hash",
                role = UserRole.STAFF,
            )
        )
    }

    private fun singleLine(foodId: Long, qty: Int = 1) =
        listOf(OrderLineRequest(foodId = foodId, quantity = qty))

    @Test
    fun `create should save order with lines and cancel`() {
        val request = OrderRequest(
            orderNo = "ORD-001",
            tableId = table.id!!,
            orderDate = LocalDateTime.now(),
            complateOrder = false,
            complateOrderDate = null,
            cancel = true,
            paidPrice = 100.0,
            change = 5.0,
            lines = listOf(
                OrderLineRequest(food.id!!, 2),
                OrderLineRequest(drink.id!!, 1)
            ),
            version = 0
        )

        val created = orderService.create(request)

        assertEquals("ORD-001", created.orderNo)
        assertEquals(table.id, created.table?.id)
        assertEquals(true, created.cancel)
        assertEquals(100.0, created.paidPrice)
        assertEquals(5.0, created.change)
        assertFalse(created.paid)
        assertEquals(2, created.lines.size)
        assertEquals(2, created.lines[0].quantity)
        assertEquals(10.0, created.lines[0].unitPrice)
        assertEquals(food.id, created.lines[0].food?.id)
        assertEquals(1, orderRepository.count())
    }

    @Test
    fun `create should persist userId on order and lines`() {
        val uid = staffUser.id!!
        val request = OrderRequest(
            orderNo = "ORD-USER",
            tableId = table.id!!,
            orderDate = LocalDateTime.now(),
            complateOrder = false,
            complateOrderDate = null,
            cancel = false,
            paidPrice = 0.0,
            change = 0.0,
            userId = uid,
            lines = listOf(
                OrderLineRequest(foodId = food.id!!, quantity = 1, userId = uid),
                OrderLineRequest(foodId = drink.id!!, quantity = 2),
            ),
            version = 0,
        )

        val created = orderService.create(request)

        assertEquals(uid, created.userId)
        assertEquals(uid, created.lines[0].userId)
        assertEquals(uid, created.lines[1].userId)
    }

    @Test
    fun `create should generate orderNo when null or blank`() {
        val day = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0)
        val fromBlank = orderService.create(
            OrderRequest(
                orderNo = "   ",
                tableId = table.id!!,
                orderDate = day,
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0
            )
        )
        assertTrue(fromBlank.orderNo.matches(Regex("""\d{8}-\d{3,}""")))

        val fromNull = orderService.create(
            OrderRequest(
                orderNo = null,
                tableId = table.id!!,
                orderDate = day,
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0
            )
        )
        assertEquals("001", fromBlank.orderNo.takeLast(3))
        assertEquals("002", fromNull.orderNo.takeLast(3))
        assertEquals(fromBlank.orderNo.take(8), fromNull.orderNo.take(8))
        assertNotEquals(fromBlank.orderNo, fromNull.orderNo)
    }

    @Test
    fun `update should change order when not paid`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-OLD",
                tableId = table.id!!,
                orderDate = LocalDateTime.now().minusHours(1),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0
            )
        )

        val request = OrderRequest(
            orderNo = "ORD-NEW",
            tableId = table.id!!,
            orderDate = LocalDateTime.now(),
            complateOrder = true,
            complateOrderDate = LocalDateTime.now(),
            cancel = true,
            paidPrice = 200.0,
            change = 15.0,
            lines = singleLine(drink.id!!, 3),
            version = 0
        )

        val updated = orderService.update(existing.id!!, request)

        assertEquals(existing.id, updated.id)
        assertEquals("ORD-NEW", updated.orderNo)
        assertEquals(true, updated.complateOrder)
        assertEquals(true, updated.cancel)
        assertEquals(1, updated.lines.size)
        assertEquals(drink.id, updated.lines[0].food?.id)
        assertEquals(3, updated.lines[0].quantity)
        assertEquals(200.0, updated.paidPrice)
        assertEquals(15.0, updated.change)
    }

    @Test
    fun `pay should close order`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-PAY",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = listOf(
                    OrderLineRequest(food.id!!, 1),
                    OrderLineRequest(drink.id!!, 2)
                ),
                version = 0
            )
        )

        val paid = orderService.pay(existing.id!!)

        assertTrue(paid.paid)
        assertNotNull(paid.paidAt)
        assertTrue(paid.complateOrder)
        assertNotNull(paid.complateOrderDate)
    }

    @Test
    fun `pay with QR scan sets flag and optional payload`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-QR",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 50.0,
                change = 5.0,
                lines = singleLine(food.id!!),
                version = 0,
            ),
        )
        val payload = "00020101021238570013A000000000000000"
        val paid = orderService.pay(
            existing.id!!,
            PayOrderRequest(
                paidPrice = 50.0,
                change = 5.0,
                paidByQrScan = true,
                qrScanPayload = payload,
            ),
        )
        assertTrue(paid.paid)
        assertTrue(paid.paidByQrScan)
        assertEquals(payload, paid.qrScanPayload)
        val receipt = orderService.receipt(existing.id!!)
        assertTrue(receipt.paidByQrScan)
        assertEquals(payload, receipt.qrScanPayload)
    }

    @Test
    fun `pay infers QR payment when qrScanPayload is set and paidByQrScan omitted`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-QR-INF",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 10.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0,
            ),
        )
        val payload = "EMVCO|000201010212"
        val paid = orderService.pay(
            existing.id!!,
            PayOrderRequest(
                paidPrice = 10.0,
                change = 0.0,
                paidByQrScan = null,
                qrScanPayload = payload,
            ),
        )
        assertTrue(paid.paidByQrScan)
        assertEquals(payload, paid.qrScanPayload)
    }

    @Test
    fun `pay with credit sets flag and clears QR fields`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-CC",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 10.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0,
            ),
        )
        val paid = orderService.pay(
            existing.id!!,
            PayOrderRequest(
                paidPrice = 10.0,
                change = 0.0,
                paidByCredit = true,
                paidByQrScan = true,
                qrScanPayload = "should-be-ignored",
            ),
        )
        assertTrue(paid.paid)
        assertTrue(paid.paidByCredit)
        assertFalse(paid.paidByQrScan)
        assertNull(paid.qrScanPayload)
        val receipt = orderService.receipt(existing.id!!)
        assertTrue(receipt.paidByCredit)
        assertFalse(receipt.paidByQrScan)
    }

    @Test
    fun `pay treats as cash when paidByQrScan false even if qrScanPayload sent`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-QR-CASH",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 10.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0,
            ),
        )
        val paid = orderService.pay(
            existing.id!!,
            PayOrderRequest(
                paidPrice = 10.0,
                change = 0.0,
                paidByQrScan = false,
                qrScanPayload = "ignored-payload",
            ),
        )
        assertFalse(paid.paidByQrScan)
        assertNull(paid.qrScanPayload)
    }

    @Test
    fun `pay twice should fail`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-2PAY",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0
            )
        )
        orderService.pay(existing.id!!)

        assertThrows(OrderAlreadyPaidException::class.java) {
            orderService.pay(existing.id!!)
        }
    }

    @Test
    fun `update should fail when order already paid`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-LOCK",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0
            )
        )
        orderService.pay(existing.id!!)

        val request = OrderRequest(
            orderNo = "ORD-X",
            tableId = table.id!!,
            orderDate = LocalDateTime.now(),
            complateOrder = false,
            complateOrderDate = null,
            cancel = false,
            paidPrice = 0.0,
            change = 0.0,
            lines = singleLine(food.id!!),
            version = 0
        )

        assertThrows(OrderAlreadyPaidException::class.java) {
            orderService.update(existing.id!!, request)
        }
    }

    @Test
    fun `delete should remove order`() {
        val existing = orderService.create(
            OrderRequest(
                orderNo = "ORD-DEL",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = singleLine(food.id!!),
                version = 0
            )
        )

        orderService.delete(existing.id!!)

        assertFalse(orderRepository.existsById(existing.id!!))
    }

    @Test
    fun `update should throw when order not found`() {
        val request = OrderRequest(
            orderNo = "ORD-404",
            tableId = table.id!!,
            orderDate = LocalDateTime.now(),
            complateOrder = false,
            complateOrderDate = null,
            cancel = false,
            paidPrice = 0.0,
            change = 0.0,
            lines = singleLine(food.id!!),
            version = 0
        )

        assertThrows(OrderNotFoundException::class.java) {
            orderService.update(999999, request)
        }
    }

    @Test
    fun `delete should throw when order not found`() {
        assertThrows(OrderNotFoundException::class.java) {
            orderService.delete(999999)
        }
    }

    @Test
    fun `receipt should return line totals and subtotal`() {
        val created = orderService.create(
            OrderRequest(
                orderNo = "ORD-RCP",
                tableId = table.id!!,
                orderDate = LocalDateTime.now(),
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 30.0,
                change = 7.0,
                lines = listOf(
                    OrderLineRequest(food.id!!, 2),
                    OrderLineRequest(drink.id!!, 1),
                ),
                version = 0,
            )
        )

        val r = orderService.receipt(created.id!!)

        assertEquals(created.id, r.orderId)
        assertEquals("ORD-RCP", r.orderNo)
        assertEquals("TB-001", r.tableCode)
        assertEquals("Test zone", r.zoneName)
        assertEquals(2, r.lines.size)
        assertEquals("FD-001", r.lines[0].foodCode)
        assertEquals(20.0, r.lines[0].lineTotal)
        assertEquals(3.0, r.lines[1].lineTotal)
        assertEquals(23.0, r.subtotal)
        assertEquals(30.0, r.paidPrice)
        assertEquals(7.0, r.change)
        assertFalse(r.paid)
        assertFalse(r.paidByQrScan)
        assertFalse(r.paidByCredit)
        assertNull(r.qrScanPayload)
    }

    @Test
    fun `receipt should throw when order not found`() {
        assertThrows(OrderNotFoundException::class.java) {
            orderService.receipt(999999)
        }
    }

    @Test
    fun `report should support date range with daily and item totals`() {
        val d1 = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
        val d2 = LocalDateTime.now().withHour(11).withMinute(0).withSecond(0).withNano(0)

        val paidOrder = orderService.create(
            OrderRequest(
                orderNo = "RP-001",
                tableId = table.id!!,
                orderDate = d1,
                complateOrder = false,
                complateOrderDate = null,
                cancel = false,
                paidPrice = 0.0,
                change = 0.0,
                lines = listOf(
                    OrderLineRequest(food.id!!, 2),
                    OrderLineRequest(drink.id!!, 1),
                ),
                version = 0,
            )
        )
        orderService.pay(paidOrder.id!!)

        orderService.create(
            OrderRequest(
                orderNo = "RP-002",
                tableId = table.id!!,
                orderDate = d2,
                complateOrder = false,
                complateOrderDate = null,
                cancel = true,
                paidPrice = 0.0,
                change = 0.0,
                lines = listOf(OrderLineRequest(food.id!!, 1)),
                version = 0,
            )
        )

        val report = orderService.report(d1.toLocalDate(), d2.toLocalDate())

        assertEquals(2, report.orderCount)
        assertEquals(1, report.paidOrderCount)
        assertEquals(1, report.cancelledOrderCount)
        assertEquals(23.0, report.grossSales)
        assertEquals(2, report.daily.size)
        assertEquals(2, report.items.size)
        assertEquals("DR-001", report.items[0].foodCode)
        assertEquals(1, report.items[0].quantity)
        assertEquals(3.0, report.items[0].total)
        assertEquals("FD-001", report.items[1].foodCode)
        assertEquals(2, report.items[1].quantity)
        assertEquals(20.0, report.items[1].total)
    }
}
