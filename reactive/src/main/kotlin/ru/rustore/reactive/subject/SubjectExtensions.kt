package ru.rustore.reactive.subject

public fun <T> MutableSubject<T>.asSubject(): Subject<T> =
    ReadOnlySubject(this)

public fun <T> MutableStateSubject<T>.asSubject(): Subject<T> =
    ReadOnlySubject(this)

public fun <T> MutableStateSubject<T>.asStateSubject(): StateSubject<T> =
    ReadOnlyStateSubject(this)