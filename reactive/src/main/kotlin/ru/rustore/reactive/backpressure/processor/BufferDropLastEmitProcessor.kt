package ru.rustore.reactive.backpressure.processor

import ru.rustore.reactive.backpressure.buffer.Buffer
import ru.rustore.reactive.backpressure.buffer.BufferItemType
import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.observable.ObservableObserver

internal class BufferDropLastEmitProcessor<T>(
    downStream: ObservableObserver<T>,
    bufferSize: Int,
    dispatcher: Dispatcher? = null,
) : BufferEmitProcessor<T>(downStream, bufferSize, dispatcher) {

    override fun onOverflow(buffer: Buffer<T>, item: BufferItemType.Item<T>) = Unit
}