package ru.rustore.reactive.backpressure.processor

import ru.rustore.reactive.backpressure.buffer.Buffer
import ru.rustore.reactive.backpressure.buffer.BufferItemType
import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.observable.ObservableObserver

internal abstract class BufferEmitProcessor<T>(
    private val downStream: ObservableObserver<T>,
    private val bufferSize: Int,
    private val dispatcher: Dispatcher?,
) : Disposable {

    private val monitor = Any()

    private val buffer = Buffer<T>(monitor)

    private var streamDone = false

    private var isDrainActive = false

    init {
        assert(bufferSize > 0) {
            "bufferSize must be > 0"
        }
    }

    // Вызывается под монитором monitor
    abstract fun onOverflow(buffer: Buffer<T>, item: BufferItemType.Item<T>)

    fun complete() {
        synchronized(monitor) {
            if (streamDone) return

            streamDone = true
            buffer.offer(BufferItemType.Complete)
        }
    }

    fun error(e: Throwable) {
        synchronized(monitor) {
            if (streamDone) return

            streamDone = true
            buffer.clear()
            buffer.offer(BufferItemType.Error(e))
        }
    }

    fun emit(item: T) {
        synchronized(monitor) {
            if (streamDone) return

            if (buffer.size() >= bufferSize) {
                onOverflow(buffer, BufferItemType.Item(item))
            } else {
                buffer.offer(BufferItemType.Item(item))
            }
        }
    }

    fun emitAll(items: List<T>) {
        synchronized(monitor) {
            items.forEach { item ->
                emit(item)
            }
        }
    }

    fun drain() {
        synchronized(monitor) {
            if (isDrainActive) return

            isDrainActive = true
        }

        if (dispatcher != null) {
            dispatcher.execute { loop() }
        } else {
            loop()
        }
    }

    override fun isDisposed(): Boolean =
        synchronized(monitor) {
            streamDone
        }

    override fun dispose() {
        synchronized(monitor) {
            streamDone = true
            buffer.clear()
        }
    }

    private fun loop() {
        while (true) {
            val value = synchronized(monitor) {
                val nextValue = buffer.popFirstOrNull()
                if (nextValue == null) {
                    isDrainActive = false
                    return
                }
                nextValue
            }

            @Suppress("UNCHECKED_CAST")
            when (value) {
                is BufferItemType.Item<*> -> downStream.onNext(value.item as T)
                is BufferItemType.Error -> downStream.onError(value.e)
                BufferItemType.Complete -> downStream.onComplete()
            }
        }
    }
}
