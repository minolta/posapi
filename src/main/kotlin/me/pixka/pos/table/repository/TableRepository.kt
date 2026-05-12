package me.pixka.pos.table.repository

import me.pixka.pos.table.model.PosTable
import org.springframework.data.jpa.repository.JpaRepository

interface TableRepository : JpaRepository<PosTable, Long> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findByCodeContainingIgnoreCaseOrderByCodeAsc(code: String): List<PosTable>
}
