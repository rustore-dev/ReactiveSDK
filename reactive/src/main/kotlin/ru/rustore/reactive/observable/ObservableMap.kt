package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.core.ifNotDisposed
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T, R> Observable<T>.map(block: (T) -> R): Observable<R> =
    ObservableMap(this, block)

private class ObservableMap<T, R>(
    private val upstream: Observable<T>,
    private val block: (T) -> R,
) : Observable<R>() {

    override fun subscribe(downstream: ObservableObserver<R>) {
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
                runCatching { block(item) }
                    .onSuccess { value ->
                        ifNotDisposed {
                            downstream.onNext(value)
                        }
                    }
                    .onFailure { error ->
                        if (disposed.compareAndSet(false, true)) {
                            upstreamDisposable.getAndSet(null)?.dispose()
                            downstream.onError(error)
                        }
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