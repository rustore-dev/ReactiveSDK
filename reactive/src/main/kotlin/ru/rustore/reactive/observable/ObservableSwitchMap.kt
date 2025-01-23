package ru.rustore.reactive.observable

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.backpressure.processor.BufferEmitProcessor
import ru.rustore.reactive.backpressure.processor.createBufferEmitProcessor
import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Suppress("MagicNumber")
public fun <T, R> Observable<T>.switchMap(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(128),
    mapper: (T) -> Observable<R>,
): Observable<R> =
    ObservableSwitchMap(this, mapper, backpressureStrategy)

private class ObservableSwitchMap<T, R>(
    private val upstream: Observable<T>,
    private val mapper: (T) -> Observable<R>,
    private val backpressureStrategy: BackpressureStrategy,
) : Observable<R>() {

    override fun subscribe(downstream: ObservableObserver<R>) {
        val switchMapObserver = SwitchMapObserver(downstream, mapper, backpressureStrategy)

        upstream.subscribe(switchMapObserver)
    }
}

private class SwitchMapObserver<T, R>(
    private val downstream: ObservableObserver<R>,
    private val mapper: (T) -> Observable<R>,
    backpressureStrategy: BackpressureStrategy,
) :
    ObservableObserver<T>, Disposable {

    private val switchMapDisposed = AtomicBoolean()
    private val actualSubstream = AtomicReference<Disposable?>(null)
    private val upstreamDisposable = AtomicReference<Disposable?>(null)

    private val emitProcessor: BufferEmitProcessor<R> = backpressureStrategy.createBufferEmitProcessor(downstream)

    // OnComplete downstream будет вызван в случае уменьшения счетчика до 0
    private val completionsLeftCount = AtomicInteger(1)

    override fun onSubscribe(d: Disposable) {
        upstreamDisposable.compareAndSet(null, d)
        if (switchMapDisposed.get()) {
            upstreamDisposable.getAndSet(null)?.dispose()
        }
        downstream.onSubscribe(this)
    }

    override fun onComplete() {
        completionsLeftCount.decrementAndGet()
        innerOnComplete()
    }

    override fun onError(e: Throwable) {
        if (switchMapDisposed.compareAndSet(false, true)) {
            actualSubstream.getAndSet(null)?.dispose()
            emitProcessor.error(e)
            emitProcessor.drain()
        }
    }

    override fun onNext(item: T) {
        if (isDisposed()) return

        completionsLeftCount.incrementAndGet()

        val substream = SubstreamSubscriber()

        val substreamToDispose = actualSubstream.getAndSet(substream)
        substreamToDispose?.dispose()

        if (!isDisposed()) {
            substream.subscribe(item)
        }
    }

    override fun dispose() {
        if (switchMapDisposed.compareAndSet(false, true)) {
            upstreamDisposable.getAndSet(null)?.dispose()
            actualSubstream.getAndSet(null)?.dispose()
        }
    }

    override fun isDisposed(): Boolean =
        switchMapDisposed.get()

    private fun innerOnComplete() {
        if (completionsLeftCount.get() == 0 && switchMapDisposed.compareAndSet(false, true)) {
            emitProcessor.complete()
            emitProcessor.drain()
        }
    }

    private inner class SubstreamSubscriber : Disposable {

        private val substreamDisposable = AtomicReference<Disposable?>(null)
        private val substreamDisposed = AtomicBoolean()

        fun subscribe(item: T) {
            if (isDisposed()) return

            runCatching { mapper(item) }
                .onSuccess { substream ->
                    val subDisposable = substream
                        .subscribe(
                            onError = { error ->
                                sendError(error)
                            },
                            onNext = { value ->
                                if (!isDisposed()) {
                                    emitProcessor.emit(value)
                                    emitProcessor.drain()
                                }
                            },
                            onComplete = {
                                if (substreamDisposed.compareAndSet(false, true)) {
                                    completionsLeftCount.decrementAndGet()
                                    innerOnComplete()
                                }
                            },
                        )
                    attachSubstream(subDisposable)
                }
                .onFailure { createSubstreamError ->
                    sendError(createSubstreamError)
                }
        }

        override fun isDisposed(): Boolean =
            substreamDisposed.get()

        override fun dispose() {
            if (substreamDisposed.compareAndSet(false, true)) {
                completionsLeftCount.decrementAndGet()
                substreamDisposable.getAndSet(null)?.dispose()
            }
        }

        private fun sendError(error: Throwable) {
            if (substreamDisposed.compareAndSet(false, true) &&
                switchMapDisposed.compareAndSet(false, true)
            ) {
                substreamDisposable.getAndSet(null)?.dispose()
                emitProcessor.error(error)
                emitProcessor.drain()
            }
        }

        private fun attachSubstream(d: Disposable) {
            substreamDisposable.compareAndSet(null, d)
            if (isDisposed()) {
                substreamDisposable.getAndSet(null)?.dispose()
            }
        }
    }
}