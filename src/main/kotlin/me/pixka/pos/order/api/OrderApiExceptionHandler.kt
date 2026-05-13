package me.pixka.pos.order.api

import me.pixka.pos.order.exception.OrderAlreadyPaidException
import me.pixka.pos.order.exception.OrderNotFoundException
import me.pixka.pos.order.exception.ReceiptPrinterDisabledException
import me.pixka.pos.order.exception.ReceiptPrinterIOException
import me.pixka.pos.order.exception.ReceiptPrinterMisconfiguredException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class OrderApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(ex: IllegalArgumentException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(OrderNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleOrderNotFound(ex: OrderNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(OrderAlreadyPaidException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleOrderAlreadyPaid(ex: OrderAlreadyPaidException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(ReceiptPrinterDisabledException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handlePrinterDisabled(ex: ReceiptPrinterDisabledException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(ReceiptPrinterMisconfiguredException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handlePrinterMisconfigured(ex: ReceiptPrinterMisconfiguredException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(ReceiptPrinterIOException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handlePrinterIo(ex: ReceiptPrinterIOException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }
}
