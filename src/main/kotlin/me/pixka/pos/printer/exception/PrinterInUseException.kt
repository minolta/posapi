package me.pixka.pos.printer.exception

class PrinterInUseException(id: Long) :
    RuntimeException("Printer $id is assigned to one or more kitchens; clear kitchen printer first.")
