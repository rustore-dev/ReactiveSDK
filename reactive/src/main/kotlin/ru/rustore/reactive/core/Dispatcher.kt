package ru.rustore.reactive.core

import java.util.concurrent.TimeUnit

public interface Dispatcher {

    public fun execute(block: () -> Unit)

    public fun executeDelayed(delay: Long, timeUnit: TimeUnit, block: () -> Unit): Disposable
}