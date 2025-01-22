package ru.rustore.reactive.single

import ru.rustore.reactive.core.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class SingleEmitterImpl<T>(
    private val downstream: SingleObserver<T>,
) : SingleEmitter<T>, Disposable {

    private val disposed = AtomicBoolean()
    private val onFinishReference = AtomicReference<(() -> Unit)?>(null)

    override fun success(value: T) {
        if (disposed.compareAndSet(false, true)) {
            onFinishReference.get()?.invoke()
            downstream.onSuccess(value)
        }
    }

    override fun error(error: Throwable) {
        if (disposed.compareAndSet(false, true)) {
            onFinishReference.get()?.invoke()
            downstream.onError(error)
        }
    }

    override fun onFinish(block: () -> Unit) {
        if (!isDisposed()) {
            onFinishReference.set(block)
        } else {
            block()
        }
    }

    override fun isDisposed(): Boolean = disposed.get()

    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            onFinishReference.get()?.invoke()
        }
    }
}