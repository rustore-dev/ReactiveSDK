package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable

public interface SingleObserver<T> {

    public fun onSubscribe(d: Disposable)

    public fun onSuccess(item: T)

    public fun onError(e: Throwable)
}