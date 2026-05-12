package me.pixka.pos.order.exception

class OrderAlreadyPaidException(id: Long) : RuntimeException("Order id=$id is already paid and cannot be modified")
