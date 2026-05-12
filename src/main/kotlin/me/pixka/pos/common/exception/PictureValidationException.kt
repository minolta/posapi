package me.pixka.pos.common.exception

/** Bad or unsupported image upload (foods, zones, etc.). */
class PictureValidationException(message: String) : RuntimeException(message)
