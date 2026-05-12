package me.pixka.pos.table.api

import me.pixka.pos.table.exception.TableNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class TableApiExceptionHandler {
    @ExceptionHandler(TableNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleTableNotFound(ex: TableNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }
}
