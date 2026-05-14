package me.pixka.pos.order.model

import jakarta.persistence.*
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

    @Column(name = "paidprice", nullable = false)
    var paidPrice: Double = 0.0,
    @Column(name = "change", nullable = false)
    var change: Double = 0.0,

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

    /** True when payment was confirmed via scanned QR (e.g. PromptPay / mobile wallet). */
    @Column(name = "paid_by_qr_scan", nullable = false)
    var paidByQrScan: Boolean = false,

    /** Raw string read from the QR (optional; for audit / reconciliation). */
    @Column(name = "qr_scan_payload", length = 1024)
    var qrScanPayload: String? = null,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var lines: MutableList<OrderLine> = mutableListOf(),

    @Version
    @Column(nullable = false)
    var version: Int = 0
)
