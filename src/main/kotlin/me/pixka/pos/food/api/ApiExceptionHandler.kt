package me.pixka.pos.food.api

import me.pixka.pos.food.exception.FoodNotFoundException
import me.pixka.pos.common.exception.PictureValidationException
import me.pixka.pos.foodcategory.exception.FoodCategoryNotFoundException
import me.pixka.pos.kitchen.exception.KitchenNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(FoodNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleFoodNotFound(ex: FoodNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(KitchenNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleKitchenNotFound(ex: KitchenNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(FoodCategoryNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleFoodCategoryNotFound(ex: FoodCategoryNotFoundException): Map<String, String> {
        return mapOf("message" to ex.message.orEmpty())
    }

    @ExceptionHandler(PictureValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlePictureValidation(ex: PictureValidationException): Map<String, String> {
        return mapOf("message" to (ex.message ?: "Invalid image upload"))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): Map<String, String> {
        // FK / unique violations from any entity (foods→category, tables→zone, foods→kitchen, etc.)
        return mapOf(
            "message" to
                "This record is still referenced elsewhere. Remove or reassign those dependent records first.",
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): Map<String, String> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation failed"
        return mapOf("message" to message)
    }
}
