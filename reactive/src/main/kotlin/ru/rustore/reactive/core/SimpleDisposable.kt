package ru.rustore.reactive.core

internal class SimpleDisposable : Disposable {

    @Volatile
    private var disposed: Boolean = false

    override fun isDisposed(): Boolean = disposed

    override fun dispose() {
        disposed = true
    }
}