package ru.rustore.reactive.subject

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.backpressure.processor.BufferDropOldestEmitProcessor
import ru.rustore.reactive.backpressure.processor.BufferEmitProcessor
import ru.rustore.reactive.backpressure.processor.createBufferEmitProcessor
import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.observable.Observable
import ru.rustore.reactive.observable.ObservableObserver
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public class MutableSubject<T>(
    private val replayCount: Int = 0,
    bufferSize: Int = 128,
) : Subject<T> {

    private val replayBufferMonitor = Any()
    private val replayBuffer = ArrayDeque<T>()

    private val observers = CopyOnWriteArraySet<BufferEmitProcessor<T>>()

    private val downstream = object : ObservableObserver<T> {
        override fun onSubscribe(d: Disposable) = Unit

        override fun onComplete() = Unit

        override fun onError(e: Throwable) = Unit

        override fun onNext(item: T) {
            observers.forEach { emitter ->
                emitter.emit(item)
                emitter.drain()
            }
        }
    }

    private val processor: BufferDropOldestEmitProcessor<T>

    init {
        assert(replayCount >= 0) {
            "replayCount must be >= 0"
        }
        assert(bufferSize > 0) {
            "bufferSize must be > 0"
        }
        assert(bufferSize >= replayCount) {
            "bufferSize must be >= replayCount"
        }
        processor = BufferDropOldestEmitProcessor(downstream, bufferSize)
    }

    public fun emit(item: T) {
        fillBuffer(item)

        processor.emit(item)
        processor.drain()
    }

    public override fun observe(
        backpressureStrategy: BackpressureStrategy,
    ): Observable<T> =
        SubjectObservable(backpressureStrategy)

    private fun fillBuffer(item: T) {
        if (replayCount == 0) return

        synchronized(replayBufferMonitor) {
            if (replayBuffer.size >= replayCount) {
                replayBuffer.removeFirstOrNull()
            }
            replayBuffer.addLast(item)
        }
    }

    private inner class SubjectObservable(
        private val backpressureStrategy: BackpressureStrategy,
    ) : Observable<T>() {

        override fun subscribe(downstream: ObservableObserver<T>) {
            val subjectObservableDisposable = object : Disposable {

                private val disposed = AtomicBoolean()
                private val emitProcessorRef = AtomicReference<BufferEmitProcessor<T>?>(null)

                fun attach(emitProcessor: BufferEmitProcessor<T>) {
                    emitProcessorRef.compareAndSet(null, emitProcessor)

                    if (disposed.get()) {
                        disposeInternal()
                    }
                }

                override fun isDisposed(): Boolean =
                    disposed.get()

                override fun dispose() {
                    if (disposed.compareAndSet(false, true)) {
                        disposeInternal()
                    }
                }

                private fun disposeInternal() {
                    val emitterToRemove = emitProcessorRef.getAndSet(null)

                    if (emitterToRemove != null) {
                        emitterToRemove.dispose()
                        observers.remove(emitterToRemove)
                    }
                }
            }

            downstream.onSubscribe(subjectObservableDisposable)

            val downstreamEmitProcessor: BufferEmitProcessor<T>
            if (replayCount == 0) {
                downstreamEmitProcessor = backpressureStrategy.createBufferEmitProcessor(downstream)
                observers.add(downstreamEmitProcessor)
            } else {
                // Создание буффера для обсервера и добавление эмиттера в список емитеров сабжекта
                // выполняется под монитором изменение буффера, что бы исключить ситуацию гонки,
                // когда в промежуток между созданием буффера и добавление в список, состояние основного буффера может измениться
                // и может быть пропущен элемент эмита
                synchronized(replayBufferMonitor) {
                    downstreamEmitProcessor = backpressureStrategy.createBufferEmitProcessor(downstream)
                    downstreamEmitProcessor.emitAll(replayBuffer.toList())

                    observers.add(downstreamEmitProcessor)
                }
            }

            subjectObservableDisposable.attach(downstreamEmitProcessor)

            downstreamEmitProcessor.drain()
        }
    }
}
