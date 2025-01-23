package ru.rustore.reactive.single

public abstract class Single<T> {

    public abstract fun subscribe(downstream: SingleObserver<T>)

    public companion object {

        public fun <T> from(source: () -> T): Single<T> =
            SingleFrom(source)

        public fun <T> create(source: (SingleEmitter<T>) -> Unit): Single<T> =
            SingleCreate(source)
    }
}
