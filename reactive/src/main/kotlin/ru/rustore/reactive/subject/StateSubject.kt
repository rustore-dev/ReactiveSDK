package ru.rustore.reactive.subject

public interface StateSubject<T> : Subject<T> {

    public val value: T
}