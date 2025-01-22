package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.doOnDispose(block: () -> Unit): Observable<T> =
    ObservableDoOnDispose(this, block)

private class ObservableDoOnDispose<T>(
    private val upstream: Observable<T>,
    private val onDispose: () -> Unit,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val wrappedObserver = object : ObservableObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable?>(null)

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

            override fun onError(e: Throwable) {
                if (disposed.compareAndSet(false, true)) {
                    downstream.onError(e)
                }
            }

            override fun onNext(item: T) {
                if (!isDisposed()) {
                    downstream.onNext(item)
                }
            }

            override fun isDisposed(): Boolean =
                disposed.get()

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    runCatching { onDispose() }
                        .onFailure { error ->
                            upstreamDisposable.getAndSet(null)?.dispose()
                            onError(error)
                        }
                        .onSuccess { upstreamDisposable.getAndSet(null)?.dispose() }
                }
            }
        }

        upstream.subscribe(wrappedObserver)
    }
}