package me.pixka.pos.order.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import me.pixka.pos.table.model.PosTable
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class PosOrder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "order_no", nullable = false, unique = true)
    var orderNo: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    var table: PosTable? = null,

    @Column(name = "order_date", nullable = false)
    var orderDate: LocalDateTime? = null,

    @Column(name = "complate_order", nullable = false)
    var complateOrder: Boolean = false,

    @Column(name = "complate_order_date")
    var complateOrderDate: LocalDateTime? = null,

    @Column(nullable = false)
    var cancel: Boolean = false,

    /** When true, bill is settled; lines must not change; start a new order for the next sale. */
    @Column(nullable = false)
    var paid: Boolean = false,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var lines: MutableList<OrderLine> = mutableListOf(),

    @Version
    @Column(nullable = false)
    var version: Int = 0
)
