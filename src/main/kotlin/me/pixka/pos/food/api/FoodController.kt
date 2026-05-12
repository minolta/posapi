package me.pixka.pos.food.api

import jakarta.validation.Valid
import me.pixka.pos.food.model.Food
import me.pixka.pos.food.service.FoodService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/foods")
class FoodController(
    private val foodService: FoodService
) {
    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<Food> {
        return foodService.search(q)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: FoodRequest): Food {
        return foodService.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: FoodRequest
    ): Food {
        return foodService.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        foodService.delete(id)
    }

    @GetMapping("/{id}/picture")
    fun getPicture(@PathVariable id: Long): ResponseEntity<Resource> {
        val loaded = foodService.loadPicture(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .contentType(loaded.second)
            .body(loaded.first)
    }

    @PostMapping("/{id}/picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPicture(@PathVariable id: Long, @RequestParam("file") file: MultipartFile): Food {
        return foodService.savePicture(id, file)
    }
}
