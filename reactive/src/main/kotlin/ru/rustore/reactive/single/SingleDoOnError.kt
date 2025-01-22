package ru.rustore.reactive.single

import ru.rustore.reactive.core.CompositeException
import ru.rustore.reactive.core.Disposable

public fun <T> Single<T>.doOnError(block: (Throwable) -> Unit): Single<T> =
    SingleDoOnError(this, block)

private class SingleDoOnError<T>(private val upstream: Single<T>, private val block: (Throwable) -> Unit) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T> {

            override fun onSubscribe(d: Disposable) {
                downstream.onSubscribe(d)
            }

            @Suppress("TooGenericExceptionCaught")
            override fun onError(e: Throwable) {
                var error = e
                try {
                    block(error)
                } catch (blockError: Throwable) {
                    error = CompositeException(blockError.stackTraceToString(), e)
                }

                downstream.onError(error)
            }

            override fun onSuccess(item: T) {
                downstream.onSuccess(item)
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}