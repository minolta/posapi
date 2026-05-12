package me.pixka.pos.food.exception

class FoodNotFoundException(id: Long) : RuntimeException("Food id=$id not found")
