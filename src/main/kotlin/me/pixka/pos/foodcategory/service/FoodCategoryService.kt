package me.pixka.pos.foodcategory.service

import me.pixka.pos.foodcategory.api.FoodCategoryRequest
import me.pixka.pos.foodcategory.exception.FoodCategoryNotFoundException
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import org.springframework.stereotype.Service

@Service
class FoodCategoryService(
    private val foodCategoryRepository: FoodCategoryRepository,
) {
    fun search(q: String?): List<FoodCategory> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            foodCategoryRepository.findAll().sortedBy { it.code }
        } else {
            foodCategoryRepository.searchByCodeOrNameContaining(trimmed)
        }
    }

    fun create(request: FoodCategoryRequest): FoodCategory {
        val foodCategory = FoodCategory(
            code = request.code.trim(),
            name = normalizeName(request.name),
        )
        return foodCategoryRepository.save(foodCategory)
    }

    fun update(id: Long, request: FoodCategoryRequest): FoodCategory {
        val foodCategory = foodCategoryRepository.findById(id).orElseThrow { FoodCategoryNotFoundException(id) }
        foodCategory.code = request.code.trim()
        foodCategory.name = normalizeName(request.name)
        return foodCategoryRepository.save(foodCategory)
    }

    fun delete(id: Long) {
        if (!foodCategoryRepository.existsById(id)) {
            throw FoodCategoryNotFoundException(id)
        }
        foodCategoryRepository.deleteById(id)
    }

    private fun normalizeName(name: String?): String? =
        name?.trim()?.takeIf { it.isNotEmpty() }
}
