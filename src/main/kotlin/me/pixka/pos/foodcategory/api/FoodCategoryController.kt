package me.pixka.pos.foodcategory.api

import jakarta.validation.Valid
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.service.FoodCategoryService
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
@RequestMapping("/api/food-categories")
class FoodCategoryController(
    private val foodCategoryService: FoodCategoryService,
) {
    /** `GET /api/food-categories` — list or search (`q` matches code or name substring). */
    @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(@RequestParam(required = false) q: String?): List<FoodCategory> {
        return foodCategoryService.search(q)
    }

    /** `POST /api/food-categories` — create (`FoodCategoryRequest`; new rows use `version: 0`). */
    @PostMapping(value = [""], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: FoodCategoryRequest): FoodCategory {
        return foodCategoryService.create(request)
    }

    /** Legacy alias: `POST /api/food-categories/new`. */
    @PostMapping("/new", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createNew(@Valid @RequestBody request: FoodCategoryRequest): FoodCategory {
        return foodCategoryService.create(request)
    }

    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: FoodCategoryRequest,
    ): FoodCategory {
        return foodCategoryService.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        foodCategoryService.delete(id)
    }
}
