package me.pixka.pos.printer.api

import me.pixka.pos.printer.exception.PrinterInUseException
import me.pixka.pos.printer.exception.PrinterNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PrinterApiExceptionHandler {
    @ExceptionHandler(PrinterNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: PrinterNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(PrinterInUseException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleInUse(ex: PrinterInUseException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }
}
