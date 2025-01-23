package ru.rustore.reactive.backpressure

public sealed interface BackpressureStrategy {

    public class BufferDropLast(public val bufferSize: Int) : BackpressureStrategy

    public class BufferDropOldest(public val bufferSize: Int) : BackpressureStrategy
}