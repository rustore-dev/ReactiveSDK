package ru.rustore.reactive.test

import org.junit.jupiter.api.Assertions
import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.single.SingleObserver

class TestSingleObserver<T> : SingleObserver<T> {

    private val monitor = Any()

    private val items = mutableListOf<T>()
    private val errors = mutableListOf<Throwable>()
    private val subscriptions = mutableListOf<Unit>()

    override fun onSubscribe(d: Disposable) {
        synchronized(monitor) {
            subscriptions.add(Unit)
        }
    }

    override fun onError(e: Throwable) {
        synchronized(monitor) {
            errors.add(e)
        }
    }

    override fun onSuccess(item: T) {
        synchronized(monitor) {
            items.add(item)
        }
    }

    fun assertEquals(other: T) {
        synchronized(monitor) {
            Assertions.assertEquals(1, items.size, "Success emitted ${items.size} times")
            Assertions.assertEquals(other, items[0])
        }
    }

    fun assertError(other: Throwable) {
        synchronized(monitor) {
            Assertions.assertEquals(1, errors.size, "Error ${errors.size} times")
            Assertions.assertEquals(other, errors[0])
        }
    }

    fun assertSubscribed() {
        synchronized(monitor) {
            Assertions.assertEquals(1, subscriptions.size, "Subscribed ${subscriptions.size} times")
        }
    }
}