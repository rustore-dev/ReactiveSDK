package ru.rustore.reactive.test

import ru.rustore.reactive.core.Dispatcher
import ru.rustore.reactive.core.Disposable
import java.util.PriorityQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TestDispatcher : Dispatcher {

    private val taskQueue = PriorityQueue<DelayedTask>()

    @Volatile
    private var dispatcherClock: Long = getCurrentNanoTime()

    @Volatile
    private var advanceTimeNano: Long = 0

    private var drainActive = false

    override fun execute(block: () -> Unit) {
        synchronized(this) {
            val delayedTask = DelayedTask(dispatcherClock, block)
            taskQueue.add(delayedTask)
            updateLocalClock()
        }

        drain()
    }

    override fun executeDelayed(delay: Long, timeUnit: TimeUnit, block: () -> Unit): Disposable {
        val delayedTask: DelayedTask
        synchronized(this) {
            delayedTask = DelayedTask(dispatcherClock + timeUnit.toNanos(delay), block)
            taskQueue.add(delayedTask)
            updateLocalClock()
        }

        drain()

        val disposable = object : Disposable {

            private val disposed = AtomicBoolean()

            override fun isDisposed(): Boolean =
                disposed.get()

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    synchronized(this@TestDispatcher) {
                        taskQueue.remove(delayedTask)
                    }
                }
            }
        }

        return disposable
    }

    fun advanceTime(time: Long, timeUnit: TimeUnit) {
        synchronized(this) {
            advanceTimeNano += timeUnit.toNanos(time)
        }
        drain()
    }

    private fun updateLocalClock() {
        dispatcherClock = getCurrentNanoTime() + advanceTimeNano
    }

    private fun getCurrentNanoTime(): Long =
        System.nanoTime()

    private fun drain() {
        synchronized(this) {
            if (drainActive) return

            drainActive = true
        }

        while (true) {
            val nextTask = synchronized(this) {
                val delayedTask = taskQueue.peek()
                updateLocalClock()
                if (delayedTask == null || delayedTask.plannedTimeNano > dispatcherClock) {
                    drainActive = false
                    return
                }
                taskQueue.remove(delayedTask)
                delayedTask
            }
            nextTask.block()
        }
    }

    private class DelayedTask(
        val plannedTimeNano: Long,
        val block: () -> Unit,
    ) : Comparable<DelayedTask> {

        override fun compareTo(other: DelayedTask): Int =
            (plannedTimeNano - other.plannedTimeNano).coerceIn(-1L..1L).toInt()
    }
}