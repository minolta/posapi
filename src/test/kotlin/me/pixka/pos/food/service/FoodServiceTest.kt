package me.pixka.pos.food.service

import me.pixka.pos.food.api.FoodRequest
import me.pixka.pos.food.exception.FoodNotFoundException
import me.pixka.pos.food.model.Food
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.repository.KitchenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files

@SpringBootTest
class FoodServiceTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerUploadDir(registry: DynamicPropertyRegistry) {
            val dir = Files.createTempDirectory("pos-food-test-upload-")
            registry.add("app.food-picture-upload-dir") { dir.toAbsolutePath().toString() }
        }
    }

    @Autowired
    private lateinit var foodService: FoodService

    @Autowired
    private lateinit var foodRepository: FoodRepository

    @Autowired
    private lateinit var kitchenRepository: KitchenRepository

    @Autowired
    private lateinit var foodCategoryRepository: FoodCategoryRepository

    private lateinit var kitchen: Kitchen
    private lateinit var foodCategory: FoodCategory

    @BeforeEach
    fun clearData() {
        foodRepository.deleteAll()
        kitchenRepository.deleteAll()
        foodCategoryRepository.deleteAll()
        kitchen = kitchenRepository.save(
            Kitchen(code = "KT-001", name = "Test kitchen")
        )
        foodCategory = foodCategoryRepository.save(
            FoodCategory(code = "CAT-001")
        )
    }

    @Test
    fun `create should save food`() {
        val request = FoodRequest(
            code = "FD-001",
            name = "Noodle soup",
            basePrice = 99.0,
            kitchenId = kitchen.id!!,
            foodCategoryId = foodCategory.id!!,
            version = 1
        )

        val created = foodService.create(request)

        assertEquals("FD-001", created.code)
        assertEquals("Noodle soup", created.name)
        assertEquals(99.0, created.basePrice)
        assertEquals(0, created.version)
        assertEquals(1, foodRepository.count())
    }

    @Test
    fun `update should change existing food`() {
        val existing = foodRepository.save(
            Food(
                code = "FD-OLD",
                name = "Old dish",
                basePrice = 50.0,
                kitchen = kitchen,
                foodCategory = foodCategory,
                version = 1
            )
        )
        val request = FoodRequest(
            code = "FD-NEW",
            name = "New dish",
            basePrice = 120.0,
            kitchenId = kitchen.id!!,
            foodCategoryId = foodCategory.id!!,
            version = 0
        )

        val updated = foodService.update(existing.id!!, request)

        assertEquals(existing.id, updated.id)
        assertEquals("FD-NEW", updated.code)
        assertEquals("New dish", updated.name)
        assertEquals(120.0, updated.basePrice)
        assertTrue(updated.version > existing.version)
    }

    @Test
    fun `delete should remove food`() {
        val existing = foodRepository.save(
            Food(
                code = "FD-DEL",
                name = "To delete",
                basePrice = 70.0,
                kitchen = kitchen,
                foodCategory = foodCategory,
                version = 1
            )
        )

        foodService.delete(existing.id!!)

        assertFalse(foodRepository.existsById(existing.id!!))
    }

    @Test
    fun `update should throw when food not found`() {
        val request = FoodRequest(
            code = "FD-404",
            name = "Missing",
            basePrice = 10.0,
            kitchenId = kitchen.id!!,
            foodCategoryId = foodCategory.id!!,
            version = 1
        )

        assertThrows(FoodNotFoundException::class.java) {
            foodService.update(999999, request)
        }
    }

    @Test
    fun `delete should throw when food not found`() {
        assertThrows(FoodNotFoundException::class.java) {
            foodService.delete(999999)
        }
    }

    @Test
    fun `savePicture should persist file and bump version`() {
        val created = foodService.create(
            FoodRequest(
                code = "FD-PIC",
                name = "Photo dish",
                basePrice = 1.0,
                kitchenId = kitchen.id!!,
                foodCategoryId = foodCategory.id!!,
                version = 0,
            ),
        )
        val id = created.id!!
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val file = MockMultipartFile("file", "dish.png", "image/png", pngHeader)
        val updated = foodService.savePicture(id, file)
        assertEquals("png", updated.pictureExtension)
        assertTrue(updated.version > created.version)
        assertNotNull(foodService.loadPicture(id))
    }

    @Test
    fun `create one hundred foods for load testing`() {
        repeat(100) { i ->
            val n = i + 1
            foodService.create(
                FoodRequest(
                    code = "TST-BULK-%03d".format(n),
                    name = "Bulk test dish $n",
                    basePrice = (n % 50 + 1).toDouble(),
                    kitchenId = kitchen.id!!,
                    foodCategoryId = foodCategory.id!!,
                    version = 0,
                ),
            )
        }
        assertEquals(100, foodRepository.count())
    }
}
