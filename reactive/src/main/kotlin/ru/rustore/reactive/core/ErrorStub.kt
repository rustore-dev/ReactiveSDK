package ru.rustore.reactive.core

internal val errorStub: (Throwable) -> Unit = {
    error("Error not implemented")
}