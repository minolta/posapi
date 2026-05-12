package me.pixka.pos.zone.exception

class ZoneNotFoundException(id: Long) : RuntimeException("Zone id=$id not found")
