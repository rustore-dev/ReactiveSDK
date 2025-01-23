package ru.rustore.reactive.backpressure.buffer

internal sealed class BufferItemType {

    object Complete : BufferItemType()

    class Error(val e: Throwable) : BufferItemType()

    class Item<T>(val item: T) : BufferItemType()
}