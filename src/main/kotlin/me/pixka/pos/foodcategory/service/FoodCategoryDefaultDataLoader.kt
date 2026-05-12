package me.pixka.pos.foodcategory.service

import me.pixka.pos.foodcategory.model.FoodCategory
import me.pixka.pos.foodcategory.repository.FoodCategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class FoodCategoryDefaultDataLoader(
    private val foodCategoryRepository: FoodCategoryRepository,
    private val resourceLoader: ResourceLoader,
    @Value("\${app.food-category-default-file:classpath:defaults/food-categories.txt}")
    private val defaultFile: String
) {
    private val log = LoggerFactory.getLogger(FoodCategoryDefaultDataLoader::class.java)

    @Bean
    fun loadDefaultFoodCategoriesOnStartup(): ApplicationRunner = ApplicationRunner {
        val resource = resourceLoader.getResource(defaultFile)
        if (!resource.exists()) {
            log.info("Food category default file not found at {}", defaultFile)
            return@ApplicationRunner
        }

        val lines = resource.inputStream.bufferedReader().use { it.readLines() }
        var inserted = 0
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split("|", limit = 2).map { it.trim() }
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                log.warn("Skip invalid food category default line: {}", raw)
                continue
            }

            val code = parts[0]
            val name = parts[1]
            if (!foodCategoryRepository.existsByCodeIgnoreCase(code)) {
                foodCategoryRepository.save(FoodCategory(code = code, name = name))
                inserted++
            }
        }
        log.info("Food category default seed complete. Inserted {} record(s).", inserted)
    }
}
