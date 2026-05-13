package me.pixka.pos.printer.repository

import me.pixka.pos.printer.model.Printer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PrinterRepository : JpaRepository<Printer, Long> {
    fun existsByCodeIgnoreCase(code: String): Boolean

    @Query(
        """
        SELECT p FROM Printer p
        WHERE LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.host) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY p.code ASC
        """
    )
    fun searchByCodeOrNameOrHostContaining(@Param("q") q: String): List<Printer>
}
