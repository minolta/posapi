package me.pixka.pos.foodcategory.exception

class FoodCategoryNotFoundException(id: Long) : RuntimeException("FoodCategory id=$id not found")
