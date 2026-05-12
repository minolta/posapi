package me.pixka.pos.food.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.kitchen.model.Kitchen

@Entity
@Table(name = "foods")
class Food(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kitchen_id", nullable = false)
    var kitchen: Kitchen? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_category_id", nullable = false)
    var foodCategory: FoodCategory? = null,

    @field:JsonIgnore
    @Column(name = "picture_ext", length = 10)
    var pictureExtension: String? = null,

    @Version
    @Column(nullable = false)
    var version: Int = 0
) {
    /** Relative URL for `<img src="{{ apiBase + pictureUrl }}">`; omitted when no picture. */
    @JsonProperty("pictureUrl")
    fun getPictureUrl(): String? =
        id?.takeIf { !pictureExtension.isNullOrBlank() }?.let { "/api/foods/$it/picture" }
}
