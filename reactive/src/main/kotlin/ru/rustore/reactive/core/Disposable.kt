package ru.rustore.reactive.core

public interface Disposable {

    public fun isDisposed(): Boolean

    public fun dispose()
}