package me.pixka.pos.food.repository

import me.pixka.pos.food.model.Food
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FoodRepository : JpaRepository<Food, Long> {
    fun existsByCodeIgnoreCase(code: String): Boolean

    @Query(
        """
        SELECT f FROM Food f
        WHERE LOWER(f.code) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY f.code ASC
        """
    )
    fun searchByCodeOrNameContaining(@Param("q") q: String): List<Food>
}
