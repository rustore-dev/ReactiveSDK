package ru.rustore.reactive.subject

internal class ReadOnlyStateSubject<T>(
    private val stateSubject: StateSubject<T>,
) : StateSubject<T> by stateSubject