package ru.rustore.reactive.single

internal class SingleCreate<T>(private val source: (SingleEmitter<T>) -> Unit) : Single<T>() {

    @Suppress("TooGenericExceptionCaught")
    override fun subscribe(downstream: SingleObserver<T>) {
        val emitter = SingleEmitterImpl(downstream)
        downstream.onSubscribe(emitter)

        source(emitter)
    }
}
