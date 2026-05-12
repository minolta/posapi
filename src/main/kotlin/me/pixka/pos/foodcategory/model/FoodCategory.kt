package me.pixka.pos.foodcategory.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "food_categories")
class FoodCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    /** Human-readable label for menus and POS UI (distinct from stable {@code code}). */
    @Column(nullable = true)
    var name: String? = null,

    @Version
    @Column(nullable = false)
    var version: Int = 0
)
