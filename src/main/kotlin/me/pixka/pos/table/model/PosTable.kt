package me.pixka.pos.table.model

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
import me.pixka.pos.zone.model.Zone

@Entity
@Table(name = "tables")
class PosTable(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0,

    @Version
    @Column(nullable = false)
    var version: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    var zone: Zone? = null
)
