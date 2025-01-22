package ru.rustore.reactive.single

import java.util.concurrent.CountDownLatch

public fun <T> Single<T>.blockingGet(): Result<T> {
    var result: Result<T>? = null
    val countDownLatch = CountDownLatch(1)

    val disposable = subscribe(
        onSuccess = { value ->
            result = Result.success(value)
            countDownLatch.countDown()
        },
        onError = { error ->
            result = Result.failure(error)
            countDownLatch.countDown()
        },
    )

    try {
        countDownLatch.await()
    } finally {
        disposable.dispose()
    }

    return requireNotNull(result)
}