package ru.rustore.reactive.backpressure.buffer

internal class Buffer<T>(private val monitor: Any = Any()) {

    private val elements = ArrayDeque<BufferItemType>()

    fun isEmpty(): Boolean =
        synchronized(monitor) {
            elements.isEmpty()
        }

    fun size(): Int =
        synchronized(monitor) {
            elements.size
        }

    fun clear() {
        synchronized(monitor) {
            elements.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun popLastOrNull(): BufferItemType? =
        synchronized(monitor) {
            when (val item = elements.removeLastOrNull()) {
                is BufferItemType.Error -> item
                is BufferItemType.Item<*> -> item as BufferItemType.Item<T>
                BufferItemType.Complete -> item
                null -> null
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun popFirstOrNull(): BufferItemType? =
        synchronized(monitor) {
            when (val item = elements.removeFirstOrNull()) {
                is BufferItemType.Error -> item
                is BufferItemType.Item<*> -> item as BufferItemType.Item<T>
                BufferItemType.Complete -> item
                null -> null
            }
        }

    fun offer(value: BufferItemType) {
        synchronized(monitor) {
            elements.addLast(value)
        }
    }

    fun toList() =
        synchronized(monitor) {
            elements.toList()
        }
}