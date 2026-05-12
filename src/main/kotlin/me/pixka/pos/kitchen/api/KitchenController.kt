package me.pixka.pos.kitchen.api

import jakarta.validation.Valid
import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.service.KitchenService
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
@RequestMapping("/api/kitchens")
class KitchenController(
    private val kitchenService: KitchenService
) {
    /** `GET /api/kitchens` — list or search (`q` matches code or name substring). */
    @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(@RequestParam(required = false) q: String?): List<Kitchen> {
        return kitchenService.search(q)
    }

    /** `POST /api/kitchens` — collection create (same contract as foods). */
    @PostMapping(value = [""], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: KitchenRequest): Kitchen {
        return kitchenService.create(request)
    }

    /** Legacy alias: `POST /api/kitchens/new`. */
    @PostMapping("/new", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createNew(@Valid @RequestBody request: KitchenRequest): Kitchen {
        return kitchenService.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: KitchenRequest
    ): Kitchen {
        return kitchenService.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        kitchenService.delete(id)
    }
}
