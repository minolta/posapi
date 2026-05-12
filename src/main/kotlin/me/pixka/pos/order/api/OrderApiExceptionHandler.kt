package me.pixka.pos.order.api

import me.pixka.pos.order.exception.OrderAlreadyPaidException
import me.pixka.pos.order.exception.OrderNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class OrderApiExceptionHandler {
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
}
