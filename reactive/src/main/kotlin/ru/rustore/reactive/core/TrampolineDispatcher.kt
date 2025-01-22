package ru.rustore.reactive.core

import java.util.concurrent.TimeUnit

public object TrampolineDispatcher : Dispatcher {

    private val taskQueue = ArrayDeque<() -> Unit>()

    private var drainActive = false

    override fun execute(block: () -> Unit) {
        synchronized(this) {
            taskQueue.addLast(block)
        }

        drain()
    }

    override fun executeDelayed(delay: Long, timeUnit: TimeUnit, block: () -> Unit): Disposable {
        timeUnit.sleep(delay)

        synchronized(this) {
            taskQueue.addLast(block)
        }

        drain()

        return EmptyDisposable
    }

    private fun drain() {
        synchronized(this) {
            if (drainActive) return

            drainActive = true
        }

        while (true) {
            val nextTask = synchronized(this) {
                val task = taskQueue.removeFirstOrNull()
                if (task == null) {
                    drainActive = false
                    return
                }

                task
            }
            nextTask()
        }
    }
}