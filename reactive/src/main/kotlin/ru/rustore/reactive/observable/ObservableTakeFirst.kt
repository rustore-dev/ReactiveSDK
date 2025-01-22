package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.core.TakeCountException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.takeFirst(limit: Int = 1): Observable<T> =
    ObservableTakeFirst(this, limit)

private class ObservableTakeFirst<T>(
    private val upstream: Observable<T>,
    private val limit: Int,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val wrappedObserver = object : ObservableObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val emitCounterLeft = AtomicInteger(limit)
            private val upstreamDisposable = AtomicReference<Disposable>(null)

            override fun onSubscribe(d: Disposable) {
                upstreamDisposable.compareAndSet(null, d)
                if (isDisposed()) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                }
                downstream.onSubscribe(this)
            }

            override fun onComplete() {
                if (disposed.compareAndSet(false, true)) {
                    if (emitCounterLeft.get() > 0) {
                        downstream.onError(TakeCountException("onComplete() called before all emits reached"))
                    } else {
                        onCompleteInternal()
                    }
                }
            }

            override fun onNext(item: T) {
                if (emitCounterLeft.decrementAndGet() == 0) {
                    downstream.onNext(item)
                    onCompleteInternal()
                } else {
                    downstream.onNext(item)
                }
            }

            private fun onCompleteInternal() {
                if (disposed.compareAndSet(false, true)) {
                    downstream.onComplete()
                }
            }

            override fun onError(e: Throwable) {
                if (disposed.compareAndSet(false, true)) {
                    downstream.onError(e)
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