package ru.rustore.reactive.single

import ru.rustore.reactive.core.CompositeException
import ru.rustore.reactive.core.Disposable

public fun <T> Single<T>.mapError(mapper: (Throwable) -> Throwable): Single<T> =
    SingleMapError(this, mapper)

private class SingleMapError<T>(private val upstream: Single<T>, private val mapper: (Throwable) -> Throwable) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T> {

            override fun onSubscribe(d: Disposable) {
                downstream.onSubscribe(d)
            }

            @Suppress("TooGenericExceptionCaught")
            override fun onError(e: Throwable) {
                val error = try {
                    mapper(e)
                } catch (blockError: Throwable) {
                    CompositeException(blockError.stackTraceToString(), e)
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