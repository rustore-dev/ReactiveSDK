package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable

public fun <T> Single<T>.doOnSubscribe(block: (Disposable) -> Unit): Single<T> =
    SingleDoOnSubscribe(this, block)

private class SingleDoOnSubscribe<T>(private val upstream: Single<T>, private val onSubscribe: (Disposable) -> Unit) : Single<T>() {

    override fun subscribe(downstream: SingleObserver<T>) {
        val wrappedObserver = object : SingleObserver<T> {
            override fun onSubscribe(d: Disposable) {
                runCatching { this@SingleDoOnSubscribe.onSubscribe(d) }
                    .onFailure { error ->
                        d.dispose()
                        downstream.onSubscribe(d)
                        downstream.onError(error)
                    }
                    .onSuccess { downstream.onSubscribe(d) }
            }

            override fun onError(e: Throwable) {
                downstream.onError(e)
            }

            override fun onSuccess(item: T) {
                downstream.onSuccess(item)
            }
        }
        return upstream.subscribe(wrappedObserver)
    }
}