package core

import DependencyTracker.runAndTrack
import graph.ObservableProvider
import java.util.UUID
import kotlin.reflect.KProperty

/**
 * A PULL-based (lazy) computed value.
 * It only recomputes when its value is requested *and* it has
 * been marked as stale.
 */
class Signal<T>(
    private val name: String = UUID.randomUUID().toString(),
    private val compute: () -> T,
) : ObservableProvider<T>(), Subscriber {

    @Volatile private var _value: T? = null
    @Volatile private var isStale: Boolean = true

    @Volatile override var lastRunEpoch: Long = 0L

    override fun get(): T {
        DependencyTracker.track(this)
        if (isStale) {
            _value = runAndTrack(compute)
            isStale = false
        }
        return _value!!
    }

    override fun notifyUpdate() {
        // no-op for PULL-based computations
    }

    override fun notifyStale() {
        if (!isStale) {
            isStale = true
            updateFreshSubscribers()
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun toString(): String = "Signal-$name(value=$_value, epoch=$lastRunEpoch)"
}