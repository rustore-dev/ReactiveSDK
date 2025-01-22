package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable

public fun <T, R> Single<T>.map(mapper: (T) -> R): Single<R> =
    SingleMap(this, mapper)

private class SingleMap<T, R>(private val upstream: Single<T>, private val mapper: (T) -> R) : Single<R>() {

    override fun subscribe(downstream: SingleObserver<R>) {
        val wrappedObserver = object : SingleObserver<T> {

            override fun onSubscribe(d: Disposable) {
                downstream.onSubscribe(d)
            }

            override fun onError(e: Throwable) {
                downstream.onError(e)
            }

            override fun onSuccess(item: T) {
                runCatching { mapper(item) }
                    .onSuccess {
                        downstream.onSuccess(it)
                    }
                    .onFailure {
                        downstream.onError(it)
                    }
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}