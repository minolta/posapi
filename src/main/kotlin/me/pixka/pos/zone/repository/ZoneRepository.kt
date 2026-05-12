package me.pixka.pos.zone.repository

import me.pixka.pos.zone.model.Zone
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ZoneRepository : JpaRepository<Zone, Long> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findByCodeIgnoreCase(code: String): Zone?

    @Query(
        """
        SELECT z FROM Zone z
        WHERE LOWER(z.code) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(z.name) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY z.code ASC
        """
    )
    fun searchByCodeOrNameContaining(@Param("q") q: String): List<Zone>
}
