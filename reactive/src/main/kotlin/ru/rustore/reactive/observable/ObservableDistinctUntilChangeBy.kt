package ru.rustore.reactive.observable

import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public fun <T> Observable<T>.distinctUntilChange(): Observable<T> =
    ObservableDistinctUntilChangeBy(this) { oldValue, newValue ->
        oldValue == newValue
    }

public fun <T> Observable<T>.distinctUntilChangeBy(
    comparator: (oldValue: T, newValue: T) -> Boolean,
): Observable<T> =
    ObservableDistinctUntilChangeBy(this, comparator)

private class ObservableDistinctUntilChangeBy<T>(
    private val upstream: Observable<T>,
    private val comparator: (oldValue: T, newValue: T) -> Boolean,
) : Observable<T>() {

    override fun subscribe(downstream: ObservableObserver<T>) {
        val wrappedObserver = DistinctUntilChangeByObserver(downstream, comparator)

        upstream.subscribe(wrappedObserver)
    }
}

private class DistinctUntilChangeByObserver<T>(
    private val downstream: ObservableObserver<T>,
    private val comparator: (oldValue: T, newValue: T) -> Boolean,
) : ObservableObserver<T>, Disposable {

    private val disposed = AtomicBoolean()
    private val upstreamDisposable = AtomicReference<Disposable>(null)

    @Suppress("CanBeNonNullable")
    @Volatile
    private var oldProceedValue: Any? = NULL

    override fun onSubscribe(d: Disposable) {
        upstreamDisposable.compareAndSet(null, d)
        if (isDisposed()) {
            upstreamDisposable.getAndSet(null)?.dispose()
        }
        downstream.onSubscribe(this)
    }

    override fun onComplete() {
        if (disposed.compareAndSet(false, true)) {
            downstream.onComplete()
        }
    }

    override fun onNext(item: T) {
        runCatching {
            val localOldProceedValue = oldProceedValue

            if (localOldProceedValue == NULL) {
                oldProceedValue = item
                true
            } else {
                @Suppress("UNCHECKED_CAST")
                val proceed = !comparator(localOldProceedValue as T, item)
                if (proceed) {
                    oldProceedValue = item
                }

                proceed
            }
        }
            .onSuccess { proceed ->
                if (proceed && !isDisposed()) {
                    downstream.onNext(item)
                }
            }
            .onFailure { error ->
                if (disposed.compareAndSet(false, true)) {
                    upstreamDisposable.getAndSet(null)?.dispose()
                    downstream.onError(error)
                }
            }
    }

    override fun onError(e: Throwable) {
        if (disposed.compareAndSet(false, true)) {
            downstream.onError(e)
        }
    }

    override fun isDisposed(): Boolean =
        disposed.get()

    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            upstreamDisposable.getAndSet(null)?.dispose()
        }
    }

    private companion object {
        val NULL = Any()
    }
}