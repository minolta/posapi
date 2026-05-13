package me.pixka.pos.printer.api

import jakarta.validation.Valid
import me.pixka.pos.printer.model.Printer
import me.pixka.pos.printer.service.PrinterService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/printers")
class PrinterController(
    private val printerService: PrinterService,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(@RequestParam(required = false) q: String?): List<Printer> {
        return printerService.search(q)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: PrinterRequest): Printer {
        return printerService.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: PrinterRequest,
    ): Printer {
        return printerService.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        printerService.delete(id)
    }
}
