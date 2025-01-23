package ru.rustore.reactive.single

import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Single<T>.observeOn(dispatcher: Dispatcher): Single<T> =
    SingleObserveOn(this, dispatcher)

private class SingleObserveOn<T>(private val upstream: Single<T>, private val dispatcher: Dispatcher) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable?>(null)

            override fun onSubscribe(d: Disposable) {
                upstreamDisposable.compareAndSet(null, d)
                if (isDisposed()) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                }
                dispatcher.execute {
                    if (!isDisposed()) {
                        downstream.onSubscribe(this)
                    }
                }
            }

            override fun onError(e: Throwable) {
                dispatcher.execute {
                    if (disposed.compareAndSet(false, true)) {
                        downstream.onError(e)
                    }
                }
            }

            override fun onSuccess(item: T) {
                dispatcher.execute {
                    if (disposed.compareAndSet(false, true)) {
                        downstream.onSuccess(item)
                    }
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