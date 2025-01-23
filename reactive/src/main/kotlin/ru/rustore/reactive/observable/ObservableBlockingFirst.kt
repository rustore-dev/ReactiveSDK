package ru.rustore.reactive.observable

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

public fun <T> Observable<T>.blockingFirst(): Result<T> {
    val valueReceived = AtomicBoolean(false)
    var result: Result<T>? = null
    val countdownLatch = CountDownLatch(1)

    val disposable = subscribe(
        onError = { error ->
            if (valueReceived.compareAndSet(false, true)) {
                result = Result.failure(error)
                countdownLatch.countDown()
            }
        },
        onComplete = {
            if (valueReceived.compareAndSet(false, true)) {
                result = Result.failure(IllegalStateException("onComplete() called before value was received in blockingFirst()"))
                countdownLatch.countDown()
            }
        },
        onNext = { value ->
            if (valueReceived.compareAndSet(false, true)) {
                result = Result.success(value)
                countdownLatch.countDown()
            }
        },
    )

    try {
        countdownLatch.await()
    } finally {
        disposable.dispose()
    }

    return requireNotNull(result)
}