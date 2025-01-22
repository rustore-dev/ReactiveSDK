package ru.rustore.reactive.core

internal object EmptyDisposable : Disposable {
    override fun isDisposed(): Boolean =
        false

    override fun dispose() = Unit
}