package me.pixka.pos.table.exception

class TableNotFoundException(id: Long) : RuntimeException("Table id=$id not found")
