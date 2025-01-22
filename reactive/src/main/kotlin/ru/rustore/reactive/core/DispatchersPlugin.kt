package ru.rustore.reactive.core

public object DispatchersPlugin {

    public var main: Dispatcher? = null
        get() = synchronized(this) { field }
        set(value) = synchronized(this) { field = value }

    public var io: Dispatcher? = null
        get() = synchronized(this) { field }
        set(value) = synchronized(this) { field = value }
}