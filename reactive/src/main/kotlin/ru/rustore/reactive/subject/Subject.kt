package ru.rustore.reactive.subject

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.observable.Observable

public interface Subject<T> {

    public fun observe(
        backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(bufferSize = 128),
    ): Observable<T>
}