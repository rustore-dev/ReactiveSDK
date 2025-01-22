package ru.rustore.reactive.observable

import ru.rustore.reactive.core.CompositeException
import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.core.ifNotDisposed
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.doOnError(block: (Throwable) -> Unit): Observable<T> =
    ObservableDoOnError(this, block)

private class ObservableDoOnError<T>(
    private val upstream: Observable<T>,
    private val block: (Throwable) -> Unit,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val wrappedObserver = object : ObservableObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
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
                    downstream.onComplete()
                }
            }

            override fun onNext(item: T) {
                ifNotDisposed {
                    downstream.onNext(item)
                }
            }

            @Suppress("TooGenericExceptionCaught")
            override fun onError(e: Throwable) {
                if (disposed.compareAndSet(false, true)) {
                    var error = e
                    try {
                        block(error)
                    } catch (blockError: Throwable) {
                        error = CompositeException(blockError.stackTraceToString(), e)
                    }

                    downstream.onError(error)
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