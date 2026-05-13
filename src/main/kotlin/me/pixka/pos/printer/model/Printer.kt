package me.pixka.pos.printer.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "printers")
class Printer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(nullable = false)
    var name: String = "",

    /** TCP host (printer IP or hostname). */
    @Column(nullable = false, length = 255)
    var host: String = "",

    @Column(nullable = false)
    var port: Int = 9100,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "connect_timeout_ms", nullable = false)
    var connectTimeoutMs: Int = 5000,

    @Column(name = "read_timeout_ms", nullable = false)
    var readTimeoutMs: Int = 5000,

    @Version
    @Column(nullable = false)
    var version: Int = 0,
)
