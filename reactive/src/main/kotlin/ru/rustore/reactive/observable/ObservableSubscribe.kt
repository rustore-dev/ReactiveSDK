package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.core.errorStub
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.subscribe(
    onError: (Throwable) -> Unit = errorStub,
    onComplete: () -> Unit = {},
    onNext: (T) -> Unit,
): Disposable {
    val observer = ObservableSubscribeObserver(onError, onComplete, onNext)
    subscribe(observer)
    return observer
}

private class ObservableSubscribeObserver<T>(
    private val onErrorCallback: (Throwable) -> Unit,
    private val onCompleteCallback: () -> Unit,
    private val onNextCallback: (T) -> Unit,
) : ObservableObserver<T>, Disposable {

    private val disposed = AtomicBoolean()
    private val upstreamDisposable = AtomicReference<Disposable>(null)

    override fun onSubscribe(d: Disposable) {
        upstreamDisposable.compareAndSet(null, d)
        if (isDisposed()) {
            upstreamDisposable.getAndSet(null)?.dispose()
        }
    }

    override fun onComplete() {
        if (disposed.compareAndSet(false, true)) {
            onCompleteCallback()
        }
    }

    override fun onError(e: Throwable) {
        if (disposed.compareAndSet(false, true)) {
            onErrorCallback(e)
        }
    }

    override fun onNext(item: T) {
        if (!isDisposed()) {
            onNextCallback(item)
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