package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.core.errorStub
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Single<T>.subscribe(onError: (Throwable) -> Unit = errorStub, onSuccess: (T) -> Unit): Disposable {
    val observer = SingleSubscribeObserver(onError, onSuccess)
    subscribe(observer)
    return observer
}

private class SingleSubscribeObserver<T>(
    private val onErrorCallback: (Throwable) -> Unit,
    private val onSuccessCallback: (T) -> Unit,
) : SingleObserver<T>, Disposable {

    private val disposed = AtomicBoolean()
    private val upstreamDisposable = AtomicReference<Disposable>(null)

    override fun onSubscribe(d: Disposable) {
        upstreamDisposable.compareAndSet(null, d)
        if (isDisposed()) {
            upstreamDisposable.getAndSet(null)?.dispose()
        }
    }

    override fun onError(e: Throwable) {
        if (disposed.compareAndSet(false, true)) {
            onErrorCallback(e)
        }
    }

    override fun onSuccess(item: T) {
        if (disposed.compareAndSet(false, true)) {
            onSuccessCallback(item)
        }
    }

    override fun isDisposed(): Boolean =
        disposed.get()

    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            upstreamDisposable.getAndSet(null)?.dispose()
        }
    }
}