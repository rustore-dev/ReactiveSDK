package ru.rustore.reactive.single

public interface SingleEmitter<T> {

    public fun onFinish(block: () -> Unit)

    public fun isDisposed(): Boolean

    public fun success(value: T)

    public fun error(error: Throwable)
}