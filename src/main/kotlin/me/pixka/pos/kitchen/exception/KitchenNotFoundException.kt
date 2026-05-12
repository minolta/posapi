package me.pixka.pos.kitchen.exception

class KitchenNotFoundException(id: Long) : RuntimeException("Kitchen id=$id not found")
