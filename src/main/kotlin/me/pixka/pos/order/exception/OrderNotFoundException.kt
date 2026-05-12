package me.pixka.pos.order.exception

class OrderNotFoundException(id: Long) : RuntimeException("Order id=$id not found")
