package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable

public interface ObservableObserver<T> {

    public fun onSubscribe(d: Disposable)

    public fun onNext(item: T)

    public fun onComplete()

    public fun onError(e: Throwable)
}