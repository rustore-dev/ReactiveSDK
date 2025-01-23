package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Dispatcher

public fun <T> Observable<T>.subscribeOn(
    dispatcher: Dispatcher,
): Observable<T> =
    ObservableSubscribeOn(this, dispatcher)

private class ObservableSubscribeOn<T>(
    private val upstream: Observable<T>,
    private val dispatcher: Dispatcher,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        dispatcher.execute {
            upstream.subscribe(downstream)
        }
    }
}