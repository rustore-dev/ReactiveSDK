package ru.rustore.reactive.single

import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.core.Dispatchers
import ru.rustore.reactive.core.Disposable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Single<T>.timeout(delay: Long, timeUnit: TimeUnit, dispatcher: Dispatcher = Dispatchers.io): Single<T> =
    SingleTimeout(this, delay, timeUnit, dispatcher)

private class SingleTimeout<T>(
    private val upstream: Single<T>,
    private val delay: Long,
    private val timeUnit: TimeUnit,
    private val dispatcher: Dispatcher,
) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable?>(null)
            private val delayedTaskDisposable = AtomicReference<Disposable?>(null)

            override fun onSubscribe(d: Disposable) {
                upstreamDisposable.compareAndSet(null, d)
                if (disposed.get()) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                    delayedTaskDisposable.getAndSet(null)?.dispose()
                } else {
                    val delayedTask = dispatcher.executeDelayed(delay, timeUnit) {
                        upstreamDisposable.getAndSet(null)?.dispose()
                        onError(TimeoutException("No value after timeout $delay $timeUnit"))
                    }
                    delayedTaskDisposable.getAndSet(delayedTask)?.dispose()
                }
                downstream.onSubscribe(this)
            }

            override fun onError(e: Throwable) {
                if (disposed.compareAndSet(false, true)) {
                    delayedTaskDisposable.getAndSet(null)?.dispose()
                    downstream.onError(e)
                }
            }

            override fun onSuccess(item: T) {
                if (disposed.compareAndSet(false, true)) {
                    delayedTaskDisposable.getAndSet(null)?.dispose()
                    downstream.onSuccess(item)
                }
            }

            override fun isDisposed(): Boolean =
                disposed.get()

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                    delayedTaskDisposable.getAndSet(null)?.dispose()
                }
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}