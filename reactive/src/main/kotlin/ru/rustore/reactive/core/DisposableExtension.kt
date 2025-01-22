package ru.rustore.reactive.core

internal inline fun Disposable.ifNotDisposed(block: () -> Unit) {
    if (!isDisposed()) {
        block()
    }
}