package ru.rustore.reactive.observable

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.backpressure.processor.BufferEmitProcessor
import ru.rustore.reactive.backpressure.processor.createBufferEmitProcessor
import ru.rustore.reactive.core.Disposable
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNCHECKED_CAST", "MagicNumber")
public fun <T1, T2, R> Observable<T1>.combineLatest(
    source2: Observable<T2>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(128),
    mapper: (T1, T2) -> R,
): Observable<R> =
    ObservableCombineLatest(
        arrayOf(
            this as Observable<Any?>,
            source2 as Observable<Any?>,
        ),
        backpressureStrategy,
    )
        .map {
            mapper(
                it[0] as T1,
                it[1] as T2,
            )
        }

@Suppress("UNCHECKED_CAST", "MagicNumber")
public fun <T1, T2, T3, R> Observable<T1>.combineLatest(
    source2: Observable<T2>,
    source3: Observable<T3>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(128),
    mapper: (T1, T2, T3) -> R,
): Observable<R> =
    ObservableCombineLatest(
        arrayOf(
            this as Observable<Any?>,
            source2 as Observable<Any?>,
            source3 as Observable<Any?>,
        ),
        backpressureStrategy,
    )
        .map {
            mapper(
                it[0] as T1,
                it[1] as T2,
                it[2] as T3,
            )
        }

@Suppress("UNCHECKED_CAST", "MagicNumber")
public fun <T1, T2, T3, T4, R> Observable<T1>.combineLatest(
    source2: Observable<T2>,
    source3: Observable<T3>,
    source4: Observable<T4>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(128),
    mapper: (T1, T2, T3, T4) -> R,
): Observable<R> =
    ObservableCombineLatest(
        arrayOf(
            this as Observable<Any?>,
            source2 as Observable<Any?>,
            source3 as Observable<Any?>,
            source4 as Observable<Any?>,
        ),
        backpressureStrategy,
    )
        .map {
            mapper(
                it[0] as T1,
                it[1] as T2,
                it[2] as T3,
                it[3] as T4,
            )
        }

@Suppress("UNCHECKED_CAST", "MagicNumber")
public fun <T1, T2, T3, T4, T5, R> Observable<T1>.combineLatest(
    source2: Observable<T2>,
    source3: Observable<T3>,
    source4: Observable<T4>,
    source5: Observable<T5>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(128),
    mapper: (T1, T2, T3, T4, T5) -> R,
): Observable<R> =
    ObservableCombineLatest(
        arrayOf(
            this as Observable<Any?>,
            source2 as Observable<Any?>,
            source3 as Observable<Any?>,
            source4 as Observable<Any?>,
            source5 as Observable<Any?>,
        ),
        backpressureStrategy,
    )
        .map {
            mapper(
                it[0] as T1,
                it[1] as T2,
                it[2] as T3,
                it[3] as T4,
                it[4] as T5,
            )
        }

private class ObservableCombineLatest(
    private val sources: Array<Observable<Any?>>,
    private val backpressureStrategy: BackpressureStrategy,
) : Observable<Array<Any?>>() {

    override fun subscribe(downstream: ObservableObserver<Array<Any?>>) {
        val combineCollector = CombineCollector(sources.size, downstream, backpressureStrategy)

        sources.forEachIndexed { index, source ->
            source.map { index to it }
                .subscribe(combineCollector)
        }
    }
}

private class CombineCollector(
    collectorSize: Int,
    private val downstream: ObservableObserver<Array<Any?>>,
    backpressureStrategy: BackpressureStrategy,
) : ObservableObserver<Pair<Int, Any?>>, Disposable {

    private val disposed = AtomicBoolean()
    private val upstreamDisposables = CopyOnWriteArraySet<AtomicReference<Disposable?>>()

    private val completeCountLeft = AtomicInteger(collectorSize)

    private val results = Array<Any?>(collectorSize) { NULL }

    private val emitProcessor: BufferEmitProcessor<Array<Any?>> = backpressureStrategy.createBufferEmitProcessor(downstream)

    override fun onSubscribe(d: Disposable) {
        val ref = AtomicReference(d)
        upstreamDisposables.add(ref)
        if (isDisposed()) {
            ref.getAndSet(null)?.dispose()
        }
        downstream.onSubscribe(this)
    }

    override fun onComplete() {
        val completeLeft = completeCountLeft.decrementAndGet()
        if (completeLeft == 0 && disposed.compareAndSet(false, true)) {
            synchronized(this) {
                emitProcessor.complete()
            }
            emitProcessor.drain()
        }
    }

    override fun onError(e: Throwable) {
        if (disposed.compareAndSet(false, true)) {
            emitProcessor.error(e)
            disposeUpstreams()
            emitProcessor.drain()
        }
    }

    override fun onNext(item: Pair<Int, Any?>) {
        synchronized(this) {
            val (index, value) = item

            results[index] = value

            val containEmptySlot = results.any { slotValue ->
                slotValue == NULL
            }
            if (!containEmptySlot) {
                emitProcessor.emit(results.copyOf())
            }
        }

        emitProcessor.drain()
    }

    override fun isDisposed(): Boolean =
        disposed.get()

    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            disposeUpstreams()
            emitProcessor.dispose()
        }
    }

    private fun disposeUpstreams() {
        upstreamDisposables.forEach { disposableRef ->
            disposableRef.getAndSet(null)?.dispose()
        }
    }

    private companion object {
        val NULL = Any()
    }
}