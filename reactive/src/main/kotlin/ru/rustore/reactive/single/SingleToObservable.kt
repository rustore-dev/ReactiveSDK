package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.observable.Observable
import ru.rustore.reactive.observable.ObservableObserver
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Single<T>.toObservable(): Observable<T> =
    SingleToObservable(this)

private class SingleToObservable<T>(private val upstream: Single<T>) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val wrappedDownstream = object : SingleObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable>(null)

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
                    downstream.onNext(item)
                    downstream.onComplete()
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

        upstream.subscribe(wrappedDownstream)
    }
}