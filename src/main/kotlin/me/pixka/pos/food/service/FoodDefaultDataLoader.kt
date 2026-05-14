package me.pixka.pos.food.service

import me.pixka.pos.food.model.Food
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import me.pixka.pos.kitchen.repository.KitchenRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class FoodDefaultDataLoader(
    private val foodRepository: FoodRepository,
    private val kitchenRepository: KitchenRepository,
    private val foodCategoryRepository: FoodCategoryRepository,
    private val resourceLoader: ResourceLoader,
    @Value("\${app.default-data-load.enabled:false}")
    private val defaultDataLoadEnabled: Boolean,
    @Value("\${app.food-default-file:classpath:defaults/foods.txt}")
    private val defaultFile: String
) {
    private val log = LoggerFactory.getLogger(FoodDefaultDataLoader::class.java)

    @Bean
    fun loadDefaultFoodsOnStartup(): ApplicationRunner = ApplicationRunner {
        if (!defaultDataLoadEnabled) {
            log.info("Default data load disabled; skip food seed (app.default-data-load.enabled=false).")
            return@ApplicationRunner
        }
        val resource = resourceLoader.getResource(defaultFile)
        if (!resource.exists()) {
            log.info("Food default file not found at {}", defaultFile)
            return@ApplicationRunner
        }

        val lines = resource.inputStream.bufferedReader().use { it.readLines() }
        var inserted = 0
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split("|", limit = 5).map { it.trim() }
            if (parts.size != 5 || parts.any { it.isEmpty() }) {
                log.warn("Skip invalid food default line: {}", raw)
                continue
            }

            val code = parts[0]
            val name = parts[1]
            val basePrice = parts[2].toDoubleOrNull()
            val kitchenCode = parts[3]
            val categoryCode = parts[4]
            if (basePrice == null) {
                log.warn("Skip food {} because basePrice is invalid in line: {}", code, raw)
                continue
            }
            if (foodRepository.existsByCodeIgnoreCase(code)) continue

            val kitchen = kitchenRepository.findByCodeIgnoreCase(kitchenCode)
            if (kitchen == null) {
                log.warn("Skip food {} because kitchen code {} not found", code, kitchenCode)
                continue
            }
            val category = foodCategoryRepository.findByCodeIgnoreCase(categoryCode)
            if (category == null) {
                log.warn("Skip food {} because category code {} not found", code, categoryCode)
                continue
            }

            foodRepository.save(
                Food(
                    code = code,
                    name = name,
                    basePrice = basePrice,
                    kitchen = kitchen,
                    foodCategory = category
                )
            )
            inserted++
        }
        log.info("Food default seed complete. Inserted {} record(s).", inserted)
    }
}
