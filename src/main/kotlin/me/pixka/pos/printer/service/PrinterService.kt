package me.pixka.pos.printer.service

import me.pixka.pos.kitchen.repository.KitchenRepository
import me.pixka.pos.printer.api.PrinterRequest
import me.pixka.pos.printer.exception.PrinterInUseException
import me.pixka.pos.printer.exception.PrinterNotFoundException
import me.pixka.pos.printer.model.Printer
import me.pixka.pos.printer.repository.PrinterRepository
import org.springframework.stereotype.Service

@Service
class PrinterService(
    private val printerRepository: PrinterRepository,
    private val kitchenRepository: KitchenRepository,
) {
    fun create(request: PrinterRequest): Printer {
        val printer = Printer(
            code = request.code.trim(),
            name = request.name.trim(),
            host = request.host.trim(),
            port = request.port,
            enabled = request.enabled,
            connectTimeoutMs = request.connectTimeoutMs,
            readTimeoutMs = request.readTimeoutMs,
        )
        return printerRepository.save(printer)
    }

    fun update(id: Long, request: PrinterRequest): Printer {
        val printer = printerRepository.findById(id).orElseThrow { PrinterNotFoundException(id) }
        printer.code = request.code.trim()
        printer.name = request.name.trim()
        printer.host = request.host.trim()
        printer.port = request.port
        printer.enabled = request.enabled
        printer.connectTimeoutMs = request.connectTimeoutMs
        printer.readTimeoutMs = request.readTimeoutMs
        return printerRepository.save(printer)
    }

    fun delete(id: Long) {
        if (!printerRepository.existsById(id)) {
            throw PrinterNotFoundException(id)
        }
        if (kitchenRepository.countByPrinterId(id) > 0) {
            throw PrinterInUseException(id)
        }
        printerRepository.deleteById(id)
    }

    fun search(q: String?): List<Printer> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            printerRepository.findAll()
        } else {
            printerRepository.searchByCodeOrNameOrHostContaining(trimmed)
        }
    }
}
