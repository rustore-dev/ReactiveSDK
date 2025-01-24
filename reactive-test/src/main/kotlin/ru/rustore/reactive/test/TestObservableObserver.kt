package ru.rustore.reactive.test

import org.junit.jupiter.api.Assertions
import ru.rustore.reactive.core.Disposable
import ru.rustore.reactive.observable.ObservableObserver

class TestObservableObserver<T> : ObservableObserver<T> {

    private val monitor = Any()

    private val items = mutableListOf<T>()
    private val errors = mutableListOf<Throwable>()
    private val completions = mutableListOf<Unit>()
    private val subscriptions = mutableListOf<Unit>()

    override fun onSubscribe(d: Disposable) {
        synchronized(monitor) {
            subscriptions.add(Unit)
        }
    }

    override fun onComplete() {
        synchronized(monitor) {
            completions.add(Unit)
        }
    }

    override fun onNext(item: T) {
        synchronized(monitor) {
            items.add(item)
        }
    }

    override fun onError(e: Throwable) {
        synchronized(monitor) {
            errors.add(e)
        }
    }

    fun getEmittedItems(): List<T> =
        synchronized(monitor) {
            items
        }
            .toList()

    fun assertError(other: Throwable) {
        synchronized(monitor) {
            Assertions.assertEquals(1, errors.size, "Error ${errors.size} times")
            Assertions.assertEquals(other, errors[0])
        }
    }

    fun assertComplete() {
        synchronized(monitor) {
            Assertions.assertEquals(1, completions.size, "Completed ${completions.size} times")
        }
    }

    fun assertSubscribed() {
        synchronized(monitor) {
            Assertions.assertEquals(1, subscriptions.size, "Subscribed ${subscriptions.size} times")
        }
    }
}