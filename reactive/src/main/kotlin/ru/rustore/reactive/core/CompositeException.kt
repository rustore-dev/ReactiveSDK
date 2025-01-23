package ru.rustore.reactive.core

public class CompositeException(message: String, vararg cause: Throwable) : Exception(message, cause.firstOrNull())