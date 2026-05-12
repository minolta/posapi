package me.pixka.pos.order.api

import jakarta.validation.Valid
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.service.OrderService
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<PosOrder> {
        return orderService.search(q)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: OrderRequest): PosOrder {
        return orderService.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: OrderRequest
    ): PosOrder {
        return orderService.update(id, request)
    }

    @PostMapping("/{id}/pay")
    fun pay(@PathVariable id: Long): PosOrder {
        return orderService.pay(id)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        orderService.delete(id)
    }
}
