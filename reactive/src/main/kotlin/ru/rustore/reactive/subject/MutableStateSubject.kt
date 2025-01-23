package ru.rustore.reactive.subject

import ru.rustore.reactive.backpressure.BackpressureStrategy
import ru.rustore.reactive.observable.Observable

public class MutableStateSubject<T>(initialValue: T) : StateSubject<T> {

    private val monitor = Any()
    private val mutableSubject = MutableSubject<T>(replayCount = 1)

    @Volatile
    private var _value: T = initialValue

    override var value: T
        get() = _value
        set(value) {
            updateState(value)
        }

    init {
        mutableSubject.emit(initialValue)
    }

    override fun observe(backpressureStrategy: BackpressureStrategy): Observable<T> =
        mutableSubject.observe(backpressureStrategy)

    public fun emit(item: T) {
        updateState(item)
    }

    private fun updateState(newValue: T) {
        synchronized(monitor) {
            val oldValue = _value
            if (newValue != oldValue) {
                _value = newValue
                mutableSubject.emit(newValue)
            }
        }
    }
}