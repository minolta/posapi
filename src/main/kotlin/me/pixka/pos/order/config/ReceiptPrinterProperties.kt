package me.pixka.pos.order.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.receipt-printer")
data class ReceiptPrinterProperties(
    /** When false, POST …/receipt/print returns 503. */
    val enabled: Boolean = false,
    /** Epson Ethernet module / printer IP (e.g. 192.168.192.168). */
    val host: String = "",
    /** Raw JetDirect-style port (Epson default is usually 9100). */
    val port: Int = 9100,
    val connectTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 5000,
)
