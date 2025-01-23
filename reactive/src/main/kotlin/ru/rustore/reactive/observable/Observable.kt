package ru.rustore.reactive.observable

import ru.rustore.reactive.backpressure.BackpressureStrategy

public abstract class Observable<T> {

    public abstract fun subscribe(downstream: ObservableObserver<T>)

    public companion object {

        public fun <T> create(
            backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BufferDropLast(bufferSize = 128),
            source: (ObservableEmitter<T>) -> Unit,
        ): Observable<T> =
            ObservableCreate(backpressureStrategy, source)
    }
}
