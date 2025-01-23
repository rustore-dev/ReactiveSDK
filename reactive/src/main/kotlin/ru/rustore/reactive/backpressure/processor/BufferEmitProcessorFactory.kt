package ru.rustore.reactive.backpressure.processor

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.observable.ObservableObserver

internal fun <T> BackpressureStrategy.createBufferEmitProcessor(
    downstream: ObservableObserver<T>,
    dispatcher: Dispatcher? = null,
): BufferEmitProcessor<T> =
    when (this) {
        is BackpressureStrategy.BufferDropLast -> BufferDropLastEmitProcessor(
            downstream,
            bufferSize,
            dispatcher = dispatcher,
        )

        is BackpressureStrategy.BufferDropOldest -> BufferDropOldestEmitProcessor(
            downstream,
            bufferSize,
            dispatcher = dispatcher,
        )
    }