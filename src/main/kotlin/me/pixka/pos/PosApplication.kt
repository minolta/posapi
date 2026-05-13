package me.pixka.pos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import me.pixka.pos.order.config.ReceiptPrinterProperties

@SpringBootApplication
@EnableConfigurationProperties(ReceiptPrinterProperties::class)
class PosApplication

fun main(args: Array<String>) {
	runApplication<PosApplication>(*args)
}
