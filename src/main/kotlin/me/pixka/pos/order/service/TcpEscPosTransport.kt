package me.pixka.pos.order.service

import me.pixka.pos.order.exception.ReceiptPrinterIOException
import me.pixka.pos.order.exception.ReceiptPrinterMisconfiguredException
import java.net.InetSocketAddress
import java.net.Socket

object TcpEscPosTransport {
    fun send(
        host: String,
        port: Int,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        payload: ByteArray,
    ) {
        val h = host.trim()
        if (h.isEmpty()) {
            throw ReceiptPrinterMisconfiguredException("Printer host is empty.")
        }
        try {
            Socket().use { socket ->
                socket.soTimeout = readTimeoutMs
                socket.connect(InetSocketAddress(h, port), connectTimeoutMs)
                socket.getOutputStream().use { os ->
                    os.write(payload)
                    os.flush()
                }
            }
        } catch (e: Exception) {
            throw ReceiptPrinterIOException(
                "Could not send to $h:$port: ${e.message}",
                e,
            )
        }
    }
}
