package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T, R> Single<T>.flatMap(mapper: (T) -> Single<R>): Single<R> =
    SingleFlatMap(this, mapper)

private class SingleFlatMap<T, R>(private val upstream: Single<T>, private val mapper: (T) -> Single<R>) : Single<R>() {

    override fun subscribe(downstream: SingleObserver<R>) {
        val wrappedObserver = object : SingleObserver<T>, Disposable {

            private val disposed = AtomicBoolean()
            private val upstreamDisposable = AtomicReference<Disposable?>(null)
            private val substreamDisposable = AtomicReference<Disposable?>(null)

            override fun onSubscribe(d: Disposable) {
                upstreamDisposable.compareAndSet(null, d)
                if (disposed.get()) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                    substreamDisposable.getAndSet(null)?.dispose()
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
                    val flatMapSource = SingleFlatMapSubscriber()

                    substreamDisposable.set(flatMapSource)

                    flatMapSource.subscribe(downstream, item)
                }
            }

            override fun isDisposed(): Boolean =
                disposed.get()

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                    substreamDisposable.getAndSet(null)?.dispose()
                }
            }
        }

        return upstream.subscribe(wrappedObserver)
    }

    private inner class SingleFlatMapSubscriber : Disposable {

        private val disposed = AtomicBoolean()
        private val upstreamDisposable = AtomicReference<Disposable>(null)

        fun subscribe(downstream: SingleObserver<R>, item: T) {
            val singleFlatMapObserver = object : SingleObserver<R> {

                override fun onSubscribe(d: Disposable) {
                    upstreamDisposable.compareAndSet(null, d)
                    if (isDisposed()) {
                        upstreamDisposable.getAndSet(null)?.dispose()
                    }
                }

                override fun onError(e: Throwable) {
                    if (disposed.compareAndSet(false, true)) {
                        downstream.onError(e)
                    }
                }

                override fun onSuccess(item: R) {
                    if (disposed.compareAndSet(false, true)) {
                        downstream.onSuccess(item)
                    }
                }
            }

            if (!isDisposed()) {
                runCatching { mapper(item) }
                    .onSuccess { newSingle ->
                        if (!isDisposed()) {
                            newSingle.subscribe(singleFlatMapObserver)
                        }
                    }
                    .onFailure { error ->
                        if (!isDisposed()) {
                            singleFlatMapObserver.onError(error)
                        }
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
}