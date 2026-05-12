package me.pixka.pos.foodcategory.repository

import me.pixka.pos.foodcategory.model.FoodCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FoodCategoryRepository : JpaRepository<FoodCategory, Long> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findByCodeIgnoreCase(code: String): FoodCategory?

    @Query(
        """
        SELECT c FROM FoodCategory c
        WHERE LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY c.code ASC
        """
    )
    fun searchByCodeOrNameContaining(@Param("q") q: String): List<FoodCategory>
}
