package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.core.ifNotDisposed
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.doOnComplete(block: () -> Unit): Observable<T> =
    ObservableDoOnComplete(this, block)

private class ObservableDoOnComplete<T>(
    private val upstream: Observable<T>,
    private val block: () -> Unit,
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
                    runCatching { block() }
                        .onSuccess {
                            downstream.onComplete()
                        }
                        .onFailure { error ->
                            if (disposed.compareAndSet(false, true)) {
                                upstreamDisposable.getAndSet(null)?.dispose()
                                downstream.onError(error)
                            }
                        }
                }
            }

            override fun onNext(item: T) {
                ifNotDisposed {
                    downstream.onNext(item)
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