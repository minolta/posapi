package me.pixka.pos.auth.exception

class UserAlreadyExistsException(username: String) : RuntimeException("Username '$username' already exists")
