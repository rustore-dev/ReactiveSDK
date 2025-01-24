package ru.rustore.reactive.test

import ru.rustore.reactive.core.DispatchersPlugin

fun runReactiveTest(testBlock: (TestDispatcher) -> Unit) {
    val testDispatcher = TestDispatcher()

    DispatchersPlugin.main = testDispatcher
    DispatchersPlugin.io = testDispatcher
    try {
        testBlock(testDispatcher)
    } finally {
        DispatchersPlugin.main = null
        DispatchersPlugin.io = null
    }
}