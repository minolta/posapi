package me.pixka.pos.zone.api

import me.pixka.pos.zone.exception.ZoneNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ZoneApiExceptionHandler {
    @ExceptionHandler(ZoneNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleZoneNotFound(ex: ZoneNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }
}
