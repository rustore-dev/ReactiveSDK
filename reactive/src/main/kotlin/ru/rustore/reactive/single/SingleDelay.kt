package ru.rustore.reactive.single

import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.core.Dispatchers
import ru.rustore.reactive.core.Disposable
import java.util.concurrent.TimeUnit

public fun <T> Single<T>.delay(delay: Long, dispatcher: Dispatcher = Dispatchers.io): Single<T> =
    flatMap { value ->
        var delayedTaskDisposable: Disposable? = null
        Single.create { emitter ->
            delayedTaskDisposable = dispatcher.executeDelayed(delay, TimeUnit.MILLISECONDS) {
                emitter.success(value)
            }
        }
            .doOnDispose { delayedTaskDisposable?.dispose() }
    }