package me.pixka.pos.kitchen.repository

import me.pixka.pos.kitchen.model.Kitchen
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface KitchenRepository : JpaRepository<Kitchen, Long> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findByCodeIgnoreCase(code: String): Kitchen?

    fun countByPrinterId(printerId: Long): Long

    @Query(
        """
        SELECT k FROM Kitchen k
        WHERE LOWER(k.code) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(k.name) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY k.code ASC
        """
    )
    fun searchByCodeOrNameContaining(@Param("q") q: String): List<Kitchen>
}
