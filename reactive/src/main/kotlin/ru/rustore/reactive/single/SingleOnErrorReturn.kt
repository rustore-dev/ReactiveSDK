package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Single<T>.onErrorReturn(mapper: (Throwable) -> T): Single<T> =
    SingleOnErrorReturn(this, mapper)

private class SingleOnErrorReturn<T>(private val upstream: Single<T>, private val mapper: (Throwable) -> T) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable?>(null)

            override fun onSubscribe(d: Disposable) {
                downstream.onSubscribe(d)
            }

            @Suppress("TooGenericExceptionCaught")
            override fun onError(e: Throwable) {
                if (disposed.compareAndSet(false, true)) {
                    val mapperResult = try {
                        Result.success(mapper(e))
                    } catch (blockError: Throwable) {
                        Result.failure(blockError)
                    }

                    mapperResult
                        .onSuccess {
                            downstream.onSuccess(it)
                        }
                        .onFailure {
                            downstream.onError(it)
                        }
                }
            }

            override fun onSuccess(item: T) {
                if (disposed.compareAndSet(false, true)) {
                    downstream.onSuccess(item)
                }
            }

            override fun isDisposed(): Boolean =
                disposed.get()

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                }
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}