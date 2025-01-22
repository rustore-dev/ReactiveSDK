package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable

public fun <T> Single<T>.doOnSuccess(block: (T) -> Unit): Single<T> =
    SingleDoOnSuccess(this, block)

private class SingleDoOnSuccess<T>(private val upstream: Single<T>, private val block: (T) -> Unit) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T> {

            override fun onSubscribe(d: Disposable) {
                downstream.onSubscribe(d)
            }

            override fun onError(e: Throwable) {
                downstream.onError(e)
            }

            override fun onSuccess(item: T) {
                runCatching { block(item) }
                    .onSuccess {
                        downstream.onSuccess(item)
                    }
                    .onFailure {
                        downstream.onError(it)
                    }
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}