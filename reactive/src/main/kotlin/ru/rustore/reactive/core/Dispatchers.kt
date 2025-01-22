package ru.rustore.reactive.core

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

public object Dispatchers {

    private const val CORE_POOL_SIZE = 3
    private const val KEEP_ALIVE_TIME_SEC = 10L

    private val scheduler by lazy {
        ScheduledThreadPoolExecutor(1)
    }

    private val threadPool by lazy {
        ThreadPoolExecutor(
            CORE_POOL_SIZE,
            Integer.MAX_VALUE,
            KEEP_ALIVE_TIME_SEC,
            TimeUnit.SECONDS,
            SynchronousQueue(),
        )
    }

    private val mainDispatcher: Dispatcher by lazy {
        val handler = Handler(Looper.getMainLooper())
        object : Dispatcher {
            override fun executeDelayed(delay: Long, timeUnit: TimeUnit, block: () -> Unit): Disposable {
                val delayMillis = timeUnit.toMillis(delay)
                handler.postDelayed(block, delayMillis)
                return SimpleDisposable()
            }

            override fun execute(block: () -> Unit) {
                handler.post(block)
            }
        }
    }

    private val ioDispatcher: Dispatcher by lazy {
        object : Dispatcher {
            override fun executeDelayed(delay: Long, timeUnit: TimeUnit, block: () -> Unit): Disposable {
                val delayedTask = Runnable {
                    threadPool.execute(block)
                }
                val future = scheduler.schedule(delayedTask, delay, timeUnit)
                val disposable = object : Disposable {

                    private val disposed = AtomicBoolean()

                    override fun isDisposed(): Boolean =
                        disposed.get()

                    override fun dispose() {
                        if (disposed.compareAndSet(false, true)) {
                            future.cancel(false)
                        }
                    }
                }

                return disposable
            }

            override fun execute(block: () -> Unit) {
                threadPool.execute(block)
            }
        }
    }

    public val main: Dispatcher
        get() = DispatchersPlugin.main ?: mainDispatcher

    public val io: Dispatcher
        get() = DispatchersPlugin.io ?: ioDispatcher

    public val trampoline: Dispatcher = TrampolineDispatcher
}
