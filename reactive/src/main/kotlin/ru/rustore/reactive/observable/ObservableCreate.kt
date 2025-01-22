package ru.rustore.reactive.observable

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.backpressure.processor.createBufferEmitProcessor

internal class ObservableCreate<T>(
    private val backpressureStrategy: BackpressureStrategy,
    private val source: (ObservableEmitter<T>) -> Unit,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val emitProcessor = backpressureStrategy.createBufferEmitProcessor(downstream)
        downstream.onSubscribe(emitProcessor)

        source(ObservableEmitter(emitProcessor))
    }
}
