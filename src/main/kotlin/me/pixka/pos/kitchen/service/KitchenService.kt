package me.pixka.pos.kitchen.service

import me.pixka.pos.kitchen.api.KitchenRequest
import me.pixka.pos.kitchen.exception.KitchenNotFoundException
import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.repository.KitchenRepository
import me.pixka.pos.printer.exception.PrinterNotFoundException
import me.pixka.pos.printer.repository.PrinterRepository
import org.springframework.stereotype.Service

@Service
class KitchenService(
    private val kitchenRepository: KitchenRepository,
    private val printerRepository: PrinterRepository,
) {
    fun create(request: KitchenRequest): Kitchen {
        val kitchen = Kitchen(
            code = request.code.trim(),
            name = request.name.trim(),
            printer = resolvePrinter(request.printerId),
        )
        return kitchenRepository.save(kitchen)
    }

    fun update(id: Long, request: KitchenRequest): Kitchen {
        val kitchen = kitchenRepository.findById(id).orElseThrow { KitchenNotFoundException(id) }
        kitchen.code = request.code.trim()
        kitchen.name = request.name.trim()
        kitchen.printer = resolvePrinter(request.printerId)
        return kitchenRepository.save(kitchen)
    }

    fun delete(id: Long) {
        if (!kitchenRepository.existsById(id)) {
            throw KitchenNotFoundException(id)
        }
        kitchenRepository.deleteById(id)
    }

    fun search(q: String?): List<Kitchen> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            kitchenRepository.findAll()
        } else {
            kitchenRepository.searchByCodeOrNameContaining(trimmed)
        }
    }

    private fun resolvePrinter(printerId: Long?) =
        printerId?.let { id ->
            printerRepository.findById(id).orElseThrow { PrinterNotFoundException(id) }
        }
}
