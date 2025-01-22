package ru.rustore.reactive.single

import ru.rustore.reactive.core.Dispatcher

public fun <T> Single<T>.subscribeOn(dispatcher: Dispatcher): Single<T> =
    SingleSubscribeOn(this, dispatcher)

private class SingleSubscribeOn<T>(private val upstream: Single<T>, private val dispatcher: Dispatcher) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        dispatcher.execute {
            upstream.subscribe(downstream)
        }
    }
}
