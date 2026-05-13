package me.pixka.pos.order.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import me.pixka.pos.food.model.Food

@Entity
@Table(name = "order_lines")
class OrderLine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: PosOrder? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    var food: Food? = null,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(length = 255)
    var note: String? = null,

    /** Unit price at time of order (snapshot from {@link Food#basePrice}). */
    @Column(name = "unit_price", nullable = false)
    var unitPrice: Double = 0.0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderLineStatus = OrderLineStatus.WAIT
)
