package ru.rustore.reactive.subject

internal class ReadOnlySubject<T>(
    private val subject: Subject<T>,
) : Subject<T> by subject