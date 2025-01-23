package ru.rustore.reactive.observable

import ru.rustore.reactive.backpressure.processor.BufferEmitProcessor

public class ObservableEmitter<T> internal constructor(
    private val emitProcessor: BufferEmitProcessor<T>,
) {
    public fun isDisposed(): Boolean = emitProcessor.isDisposed()

    public fun onNext(item: T) {
        emitProcessor.emit(item)
        emitProcessor.drain()
    }

    public fun onComplete() {
        emitProcessor.complete()
        emitProcessor.drain()
    }

    public fun onError(e: Throwable) {
        emitProcessor.error(e)
        emitProcessor.drain()
    }
}