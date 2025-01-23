package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable

public fun <T> Observable<T>.doOnSubscribe(block: (Disposable) -> Unit): Observable<T> =
    ObservableDoOnSubscribe(this, block)

private class ObservableDoOnSubscribe<T>(
    private val upstream: Observable<T>,
    private val onSubscribe: (Disposable) -> Unit,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val wrappedObserver = object : ObservableObserver<T> {
            override fun onSubscribe(d: Disposable) {
                runCatching { this@ObservableDoOnSubscribe.onSubscribe(d) }
                    .onFailure { error ->
                        d.dispose()
                        downstream.onSubscribe(d)
                        downstream.onError(error)
                    }
                    .onSuccess { downstream.onSubscribe(d) }
            }

            override fun onComplete() {
                downstream.onComplete()
            }

            override fun onNext(item: T) {
                downstream.onNext(item)
            }

            override fun onError(e: Throwable) {
                downstream.onError(e)
            }
        }

        return upstream.subscribe(wrappedObserver)
    }
}