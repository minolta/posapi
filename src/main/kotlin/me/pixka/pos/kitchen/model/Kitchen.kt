package me.pixka.pos.kitchen.model

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
import me.pixka.pos.printer.model.Printer

@Entity
@Table(name = "kitchens")
class Kitchen(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(nullable = false)
    var name: String = "",

    /** When set, kitchen tickets for foods in this kitchen go to this TCP printer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printer_id")
    var printer: Printer? = null,

    @Version
    @Column(nullable = false)
    var version: Int = 0
)
