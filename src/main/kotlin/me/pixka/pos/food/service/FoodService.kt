package me.pixka.pos.food.service

import me.pixka.pos.food.api.FoodRequest
import me.pixka.pos.food.exception.FoodNotFoundException
import me.pixka.pos.common.exception.PictureValidationException
import me.pixka.pos.food.model.Food
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.foodcategory.exception.FoodCategoryNotFoundException
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import me.pixka.pos.kitchen.exception.KitchenNotFoundException
import me.pixka.pos.kitchen.repository.KitchenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@Service
class FoodService(
    private val foodRepository: FoodRepository,
    private val kitchenRepository: KitchenRepository,
    private val foodCategoryRepository: FoodCategoryRepository,
    private val pictureStorage: FoodPictureStorage,
    @Value("\${app.food-picture-max-bytes:5242880}") private val maxPictureBytes: Long,
) {
    fun create(request: FoodRequest): Food {
        val kitchen = kitchenRepository.findById(request.kitchenId).orElseThrow { KitchenNotFoundException(request.kitchenId) }
        val foodCategory = foodCategoryRepository.findById(request.foodCategoryId)
            .orElseThrow { FoodCategoryNotFoundException(request.foodCategoryId) }
        val food = Food(
            code = request.code,
            name = request.name,
            basePrice = request.basePrice,
            kitchen = kitchen,
            foodCategory = foodCategory,
            blockOrderLine = request.blockOrderLine ?: false,
        )
        return foodRepository.save(food)
    }

    fun update(id: Long, request: FoodRequest): Food {
        val food = foodRepository.findById(id).orElseThrow { FoodNotFoundException(id) }
        val kitchen = kitchenRepository.findById(request.kitchenId).orElseThrow { KitchenNotFoundException(request.kitchenId) }
        val foodCategory = foodCategoryRepository.findById(request.foodCategoryId)
            .orElseThrow { FoodCategoryNotFoundException(request.foodCategoryId) }
        food.code = request.code
        food.name = request.name
        food.basePrice = request.basePrice
        food.kitchen = kitchen
        food.foodCategory = foodCategory
        if (request.blockOrderLine != null) {
            food.blockOrderLine = request.blockOrderLine
        }
        return foodRepository.save(food)
    }

    fun delete(id: Long) {
        if (!foodRepository.existsById(id)) {
            throw FoodNotFoundException(id)
        }
        pictureStorage.deleteAllForFood(id)
        foodRepository.deleteById(id)
    }

    fun search(q: String?): List<Food> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            foodRepository.findAll()
        } else {
            foodRepository.searchByCodeOrNameContaining(trimmed)
        }
    }

    fun savePicture(id: Long, file: MultipartFile): Food {
        if (file.isEmpty) {
            throw PictureValidationException("Choose a non-empty image file.")
        }
        if (file.size > maxPictureBytes) {
            val mb = maxPictureBytes / 1024 / 1024
            throw PictureValidationException("Image is too large (max $mb MB).")
        }
        val food = foodRepository.findById(id).orElseThrow { FoodNotFoundException(id) }
        val ext = pictureStorage.resolveExtension(file.contentType, file.originalFilename)
        pictureStorage.deleteAllForFood(id)
        file.inputStream.use { pictureStorage.replaceFromStream(id, ext, it) }
        food.pictureExtension = ext
        return foodRepository.save(food)
    }

    /**
     * Serves the image file. If the DB says there is a picture but the file is missing (wrong cwd,
     * deleted `data/`, etc.), clears [Food.pictureExtension] so list JSON stops advertising [Food.getPictureUrl].
     */
    @Transactional
    fun loadPicture(id: Long): Pair<Resource, MediaType>? {
        val food = foodRepository.findById(id).orElseThrow { FoodNotFoundException(id) }
        val ext = food.pictureExtension ?: return null
        val fid = food.id ?: return null
        val path = pictureStorage.fileFor(fid, ext)
        if (!Files.exists(path)) {
            food.pictureExtension = null
            foodRepository.save(food)
            return null
        }
        val resource: Resource = FileSystemResource(path.toFile())
        return resource to pictureStorage.mimeTypeForExtension(ext)
    }
}
