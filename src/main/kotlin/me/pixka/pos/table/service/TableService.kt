package me.pixka.pos.table.service

import me.pixka.pos.table.api.TableRequest
import me.pixka.pos.table.exception.TableNotFoundException
import me.pixka.pos.table.model.PosTable
import me.pixka.pos.table.repository.TableRepository
import me.pixka.pos.zone.exception.ZoneNotFoundException
import me.pixka.pos.zone.repository.ZoneRepository
import org.springframework.stereotype.Service

@Service
class TableService(
    private val tableRepository: TableRepository,
    private val zoneRepository: ZoneRepository
) {
    fun create(request: TableRequest): PosTable {
        val zone = zoneRepository.findById(request.zoneId).orElseThrow { ZoneNotFoundException(request.zoneId) }
        val table = PosTable(
            code = request.code,
            basePrice = request.basePrice,
            zone = zone
        )
        return tableRepository.save(table)
    }

    fun update(id: Long, request: TableRequest): PosTable {
        val table = tableRepository.findById(id).orElseThrow { TableNotFoundException(id) }
        val zone = zoneRepository.findById(request.zoneId).orElseThrow { ZoneNotFoundException(request.zoneId) }

        table.code = request.code
        table.basePrice = request.basePrice
        table.zone = zone
        return tableRepository.save(table)
    }

    fun delete(id: Long) {
        if (!tableRepository.existsById(id)) {
            throw TableNotFoundException(id)
        }
        tableRepository.deleteById(id)
    }

    fun search(q: String?): List<PosTable> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            tableRepository.findAll()
        } else {
            tableRepository.findByCodeContainingIgnoreCaseOrderByCodeAsc(trimmed)
        }
    }
}
