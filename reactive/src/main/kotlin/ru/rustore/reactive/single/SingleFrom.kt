package ru.rustore.reactive.single

import ru.rustore.reactive.core.SimpleDisposable
import ru.rustore.reactive.core.ifNotDisposed

internal class SingleFrom<T>(private val source: () -> T) : Single<T>() {

    @Suppress("TooGenericExceptionCaught")
    override fun subscribe(downstream: SingleObserver<T>) {
        val disposable = SimpleDisposable()
        downstream.onSubscribe(disposable)

        disposable.ifNotDisposed {
            runCatching { source() }
                .onSuccess {
                    disposable.ifNotDisposed { downstream.onSuccess(it) }
                }
                .onFailure {
                    disposable.ifNotDisposed { downstream.onError(it) }
                }
        }
    }
}
