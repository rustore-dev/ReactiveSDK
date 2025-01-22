package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Single<T>.doOnDispose(block: () -> Unit): Single<T> =
    SingleDoOnDispose(this, block)

private class SingleDoOnDispose<T>(private val upstream: Single<T>, private val onDispose: () -> Unit) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable?>(null)

            override fun onSubscribe(d: Disposable) {
                upstreamDisposable.compareAndSet(null, d)
                if (isDisposed()) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                }
                downstream.onSubscribe(this)
            }

            override fun onError(e: Throwable) {
                if (disposed.compareAndSet(false, true)) {
                    downstream.onError(e)
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
                    runCatching { onDispose() }
                        .onFailure { error ->
                            upstreamDisposable.getAndSet(null)?.dispose()
                            downstream.onError(error)
                        }
                        .onSuccess { upstreamDisposable.getAndSet(null)?.dispose() }
                }
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}