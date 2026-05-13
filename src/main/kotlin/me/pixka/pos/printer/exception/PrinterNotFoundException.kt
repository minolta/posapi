package me.pixka.pos.printer.exception

class PrinterNotFoundException(id: Long) : RuntimeException("Printer not found: $id")
