package ru.rustore.reactive.observable

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.backpressure.processor.createBufferEmitProcessor
import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.observeOn(
    dispatcher: Dispatcher,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(bufferSize = 128),
): Observable<T> =
    ObservableObserveOn(this, dispatcher, backpressureStrategy)

private class ObservableObserveOn<T>(
    private val upstream: Observable<T>,
    private val dispatcher: Dispatcher,
    private val backpressureStrategy: BackpressureStrategy,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val emitProcessor = backpressureStrategy.createBufferEmitProcessor(downstream, dispatcher = dispatcher)

        val wrappedDownstream = object : ObservableObserver<T>, Disposable {

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
                disposed.set(true)
                emitProcessor.complete()
                emitProcessor.drain()
            }

            override fun onError(e: Throwable) {
                disposed.set(true)
                emitProcessor.error(e)
                emitProcessor.drain()
            }

            override fun onNext(item: T) {
                emitProcessor.emit(item)
                emitProcessor.drain()
            }

            override fun isDisposed(): Boolean =
                disposed.get()

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    emitProcessor.dispose()
                    upstreamDisposable.getAndSet(null)?.dispose()
                }
            }
        }

        upstream.subscribe(wrappedDownstream)
    }
}