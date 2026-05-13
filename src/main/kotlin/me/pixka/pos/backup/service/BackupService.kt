package me.pixka.pos.backup.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import me.pixka.pos.backup.api.BackupImportResponse
import me.pixka.pos.food.model.Food
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.repository.KitchenRepository
import me.pixka.pos.order.model.OrderLine
import me.pixka.pos.order.model.OrderLineStatus
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.printer.model.Printer
import me.pixka.pos.printer.repository.PrinterRepository
import me.pixka.pos.table.model.PosTable
import me.pixka.pos.table.repository.TableRepository
import me.pixka.pos.zone.model.Zone
import me.pixka.pos.zone.repository.ZoneRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class BackupService(
    private val zoneRepository: ZoneRepository,
    private val tableRepository: TableRepository,
    private val kitchenRepository: KitchenRepository,
    private val printerRepository: PrinterRepository,
    private val foodCategoryRepository: FoodCategoryRepository,
    private val foodRepository: FoodRepository,
    private val orderRepository: OrderRepository,
    @Value("\${app.backup-dir:\${user.dir}/data/backups}")
    private val backupDir: String,
) {
    private val tsFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    private val jsonMapper = ObjectMapper()

    @Transactional(readOnly = true)
    fun exportAllRecords(): Path {
        val now = LocalDateTime.now()
        val fileName = "backup-${now.format(tsFormat)}.json"
        val dir = Path.of(backupDir)
        Files.createDirectories(dir)
        val file = dir.resolve(fileName)

        val snapshot = mapOf(
            "createdAt" to now.toString(),
            "zones" to zoneRepository.findAll(Sort.by("id")).map { z ->
                mapOf(
                    "id" to z.id,
                    "code" to z.code,
                    "name" to z.name,
                    "pictureExtension" to z.pictureExtension,
                    "version" to z.version,
                )
            },
            "tables" to tableRepository.findAll(Sort.by("id")).map { t ->
                mapOf(
                    "id" to t.id,
                    "code" to t.code,
                    "basePrice" to t.basePrice,
                    "zoneId" to t.zone?.id,
                    "version" to t.version,
                )
            },
            "printers" to printerRepository.findAll(Sort.by("id")).map { p ->
                mapOf(
                    "id" to p.id,
                    "code" to p.code,
                    "name" to p.name,
                    "host" to p.host,
                    "port" to p.port,
                    "enabled" to p.enabled,
                    "connectTimeoutMs" to p.connectTimeoutMs,
                    "readTimeoutMs" to p.readTimeoutMs,
                    "version" to p.version,
                )
            },
            "kitchens" to kitchenRepository.findAll(Sort.by("id")).map { k ->
                mapOf(
                    "id" to k.id,
                    "code" to k.code,
                    "name" to k.name,
                    "printerId" to k.printer?.id,
                    "version" to k.version,
                )
            },
            "foodCategories" to foodCategoryRepository.findAll(Sort.by("id")).map { c ->
                mapOf(
                    "id" to c.id,
                    "code" to c.code,
                    "name" to c.name,
                    "version" to c.version,
                )
            },
            "foods" to foodRepository.findAll(Sort.by("id")).map { f ->
                mapOf(
                    "id" to f.id,
                    "code" to f.code,
                    "name" to f.name,
                    "basePrice" to f.basePrice,
                    "kitchenId" to f.kitchen?.id,
                    "foodCategoryId" to f.foodCategory?.id,
                    "pictureExtension" to f.pictureExtension,
                    "version" to f.version,
                )
            },
            "orders" to orderRepository.findAll(Sort.by("id")).map { o ->
                mapOf(
                    "id" to o.id,
                    "orderNo" to o.orderNo,
                    "paidPrice" to o.paidPrice,
                    "change" to o.change,
                    "tableId" to o.table?.id,
                    "orderDate" to o.orderDate?.toString(),
                    "complateOrder" to o.complateOrder,
                    "complateOrderDate" to o.complateOrderDate?.toString(),
                    "cancel" to o.cancel,
                    "paid" to o.paid,
                    "paidAt" to o.paidAt?.toString(),
                    "version" to o.version,
                    "lines" to o.lines.map { l ->
                        mapOf(
                            "id" to l.id,
                            "foodId" to l.food?.id,
                            "quantity" to l.quantity,
                            "note" to l.note,
                            "unitPrice" to l.unitPrice,
                            "status" to l.status.name,
                        )
                    },
                )
            },
        )

        Files.writeString(file, toJson(snapshot))
        return file
    }

    /** Reads a backup file from [backupDir] only (no path traversal). */
    fun readBackupFile(fileName: String): ByteArray {
        val name = fileName.trim()
        require(name.isNotEmpty()) { "fileName is required" }
        require(!name.contains("..")) { "invalid fileName" }
        require(!name.contains('/') && !name.contains('\\')) { "fileName must be a simple file name" }
        val root = Path.of(backupDir).normalize().toAbsolutePath()
        val path = root.resolve(name).normalize().toAbsolutePath()
        require(path.startsWith(root)) { "invalid fileName" }
        require(Files.isRegularFile(path)) { "backup file not found: $name" }
        return Files.readAllBytes(path)
    }

    /**
     * Replaces all POS data with the content of a backup JSON (same shape as export).
     * Caller must pass [confirm]=true from the API layer.
     */
    @Transactional
    fun importFromJson(json: String, confirm: Boolean): BackupImportResponse {
        require(confirm) { "confirm must be true to import and replace all data" }
        val root = jsonMapper.readTree(json)
        clearAllData()

        val zoneIdMap = mutableMapOf<Long, Long>()
        val printerIdMap = mutableMapOf<Long, Long>()
        val foodCategoryIdMap = mutableMapOf<Long, Long>()
        val kitchenIdMap = mutableMapOf<Long, Long>()
        val tableIdMap = mutableMapOf<Long, Long>()
        val foodIdMap = mutableMapOf<Long, Long>()

        var zonesCount = 0
        for (n in root.arraySortedById("zones")) {
            val z = Zone(
                code = n.reqText("code"),
                name = n.reqText("name"),
                pictureExtension = n.optText("pictureExtension"),
            )
            z.version = n.optInt("version") ?: 0
            val saved = zoneRepository.save(z)
            zoneIdMap[n.reqLong("id")] = saved.id!!
            zonesCount++
        }

        var printersCount = 0
        for (n in root.arraySortedById("printers")) {
            val p = Printer(
                code = n.reqText("code"),
                name = n.reqText("name"),
                host = n.reqText("host"),
                port = n.optInt("port") ?: 9100,
                enabled = n.optBoolean("enabled") ?: true,
                connectTimeoutMs = n.optInt("connectTimeoutMs") ?: 5000,
                readTimeoutMs = n.optInt("readTimeoutMs") ?: 5000,
            )
            p.version = n.optInt("version") ?: 0
            val saved = printerRepository.save(p)
            printerIdMap[n.reqLong("id")] = saved.id!!
            printersCount++
        }

        var categoriesCount = 0
        for (n in root.arraySortedById("foodCategories")) {
            val c = FoodCategory(
                code = n.reqText("code"),
                name = n.optText("name"),
            )
            c.version = n.optInt("version") ?: 0
            val saved = foodCategoryRepository.save(c)
            foodCategoryIdMap[n.reqLong("id")] = saved.id!!
            categoriesCount++
        }

        var kitchensCount = 0
        for (n in root.arraySortedById("kitchens")) {
            val printerOld = n.optLong("printerId")
            val k = Kitchen(
                code = n.reqText("code"),
                name = n.reqText("name"),
                printer = printerOld?.let { old ->
                    val newId = printerIdMap[old] ?: throw IllegalArgumentException("unknown printerId in kitchen: $old")
                    printerRepository.getReferenceById(newId)
                },
            )
            k.version = n.optInt("version") ?: 0
            val saved = kitchenRepository.save(k)
            kitchenIdMap[n.reqLong("id")] = saved.id!!
            kitchensCount++
        }

        var tablesCount = 0
        for (n in root.arraySortedById("tables")) {
            val zoneOld = n.reqLong("zoneId")
            val t = PosTable(
                code = n.reqText("code"),
                basePrice = n.optDouble("basePrice") ?: 0.0,
                zone = zoneRepository.getReferenceById(
                    zoneIdMap[zoneOld] ?: throw IllegalArgumentException("unknown zoneId in table: $zoneOld"),
                ),
            )
            t.version = n.optInt("version") ?: 0
            val saved = tableRepository.save(t)
            tableIdMap[n.reqLong("id")] = saved.id!!
            tablesCount++
        }

        var foodsCount = 0
        for (n in root.arraySortedById("foods")) {
            val kitchenOld = n.reqLong("kitchenId")
            val catOld = n.reqLong("foodCategoryId")
            val f = Food(
                code = n.reqText("code"),
                name = n.reqText("name"),
                basePrice = n.optDouble("basePrice") ?: 0.0,
                kitchen = kitchenRepository.getReferenceById(
                    kitchenIdMap[kitchenOld] ?: throw IllegalArgumentException("unknown kitchenId in food: $kitchenOld"),
                ),
                foodCategory = foodCategoryRepository.getReferenceById(
                    foodCategoryIdMap[catOld] ?: throw IllegalArgumentException("unknown foodCategoryId in food: $catOld"),
                ),
                pictureExtension = n.optText("pictureExtension"),
            )
            f.version = n.optInt("version") ?: 0
            val saved = foodRepository.save(f)
            foodIdMap[n.reqLong("id")] = saved.id!!
            foodsCount++
        }

        var ordersCount = 0
        for (n in root.arraySortedById("orders")) {
            val tableOld = n.reqLong("tableId")
            val order = PosOrder(
                orderNo = n.reqText("orderNo"),
                paidPrice = n.optDouble("paidPrice") ?: 0.0,
                change = n.optDouble("change") ?: 0.0,
                table = tableRepository.getReferenceById(
                    tableIdMap[tableOld] ?: throw IllegalArgumentException("unknown tableId in order: $tableOld"),
                ),
                orderDate = parseDateTime(n, "orderDate"),
                complateOrder = n.optBoolean("complateOrder") ?: false,
                complateOrderDate = parseDateTimeNullable(n, "complateOrderDate"),
                cancel = n.optBoolean("cancel") ?: false,
                paid = n.optBoolean("paid") ?: false,
                paidAt = parseDateTimeNullable(n, "paidAt"),
            )
            order.version = n.optInt("version") ?: 0
            val linesNode = n["lines"]
            if (linesNode != null && linesNode.isArray) {
                for (ln in linesNode) {
                    val foodOld = ln.reqLong("foodId")
                    order.lines.add(
                        OrderLine(
                            order = order,
                            food = foodRepository.getReferenceById(
                                foodIdMap[foodOld] ?: throw IllegalArgumentException("unknown foodId in order line: $foodOld"),
                            ),
                            quantity = ln.optInt("quantity") ?: 1,
                            note = ln.optText("note"),
                            unitPrice = ln.optDouble("unitPrice") ?: 0.0,
                            status = parseLineStatus(ln),
                        ),
                    )
                }
            }
            orderRepository.save(order)
            ordersCount++
        }

        return BackupImportResponse(
            message = "Import completed; previous data was replaced.",
            zonesRestored = zonesCount,
            printersRestored = printersCount,
            foodCategoriesRestored = categoriesCount,
            kitchensRestored = kitchensCount,
            tablesRestored = tablesCount,
            foodsRestored = foodsCount,
            ordersRestored = ordersCount,
        )
    }

    private fun clearAllData() {
        orderRepository.deleteAll(orderRepository.findAll())
        foodRepository.deleteAll(foodRepository.findAll())
        tableRepository.deleteAll(tableRepository.findAll())
        kitchenRepository.deleteAll(kitchenRepository.findAll())
        foodCategoryRepository.deleteAll(foodCategoryRepository.findAll())
        printerRepository.deleteAll(printerRepository.findAll())
        zoneRepository.deleteAll(zoneRepository.findAll())
    }

    private fun parseLineStatus(ln: JsonNode): OrderLineStatus {
        val raw = ln.optText("status") ?: return OrderLineStatus.WAIT
        return try {
            OrderLineStatus.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            OrderLineStatus.WAIT
        }
    }

    private fun parseDateTime(n: JsonNode, field: String): LocalDateTime {
        val s = n.optText(field) ?: throw IllegalArgumentException("missing $field on order")
        return try {
            LocalDateTime.parse(s)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("invalid $field datetime: $s")
        }
    }

    private fun parseDateTimeNullable(n: JsonNode, field: String): LocalDateTime? {
        val s = n.optText(field) ?: return null
        return try {
            LocalDateTime.parse(s)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun JsonNode.arraySortedById(field: String): List<JsonNode> {
        val arr = this[field] ?: return emptyList()
        if (!arr.isArray) return emptyList()
        return arr.sortedBy { node ->
            if (node.has("id") && !node["id"].isNull) node["id"].asLong() else 0L
        }
    }

    private fun JsonNode.reqText(field: String): String =
        optText(field) ?: throw IllegalArgumentException("missing $field")

    private fun JsonNode.optText(field: String): String? =
        this[field]?.takeUnless { it.isNull || it.isMissingNode }?.asText()

    private fun JsonNode.reqLong(field: String): Long =
        optLong(field) ?: throw IllegalArgumentException("missing $field")

    private fun JsonNode.optLong(field: String): Long? =
        this[field]?.takeUnless { it.isNull || it.isMissingNode }?.asLong()

    private fun JsonNode.optInt(field: String): Int? =
        this[field]?.takeUnless { it.isNull || it.isMissingNode }?.asInt()

    private fun JsonNode.optBoolean(field: String): Boolean? =
        this[field]?.takeUnless { it.isNull || it.isMissingNode }?.asBoolean()

    private fun JsonNode.optDouble(field: String): Double? =
        this[field]?.takeUnless { it.isNull || it.isMissingNode }?.asDouble()

    private fun toJson(value: Any?, indent: Int = 0): String {
        return when (value) {
            null -> "null"
            is String -> "\"${escape(value)}\""
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> {
                if (value.isEmpty()) return "{}"
                val pad = "  ".repeat(indent)
                val nextPad = "  ".repeat(indent + 1)
                value.entries.joinToString(prefix = "{\n", postfix = "\n$pad}", separator = ",\n") { (k, v) ->
                    "$nextPad\"${escape(k.toString())}\": ${toJson(v, indent + 1)}"
                }
            }
            is Iterable<*> -> {
                val list = value.toList()
                if (list.isEmpty()) return "[]"
                val pad = "  ".repeat(indent)
                val nextPad = "  ".repeat(indent + 1)
                list.joinToString(prefix = "[\n", postfix = "\n$pad]", separator = ",\n") { item ->
                    "$nextPad${toJson(item, indent + 1)}"
                }
            }
            else -> "\"${escape(value.toString())}\""
        }
    }

    private fun escape(s: String): String {
        val out = StringBuilder(s.length + 8)
        for (ch in s) {
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> out.append(ch)
            }
        }
        return out.toString()
    }
}
