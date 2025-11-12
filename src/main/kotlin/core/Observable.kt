package core

import DependencyTracker.runAndTrack
import graph.ObservableProvider
import java.util.UUID
import kotlin.reflect.KProperty

/**
 * A PUSH-based (eager) computed value.
 * It recomputes **immediately** when a dependency changes.
 * Its [get] is always a cheap read of its cached value.
 */
class Observable<T>(
    private val name: String = UUID.randomUUID().toString(),
    private val compute: () -> T,
) : ObservableProvider<T>(), Subscriber {

    @Volatile private var _value: T = runAndTrack(compute)

    @Volatile override var lastRunEpoch: Long = 0L

    override fun get(): T {
        DependencyTracker.track(this)
        return _value
    }

    override fun notifyUpdate() {
        val newValue = runAndTrack(compute)
        if (newValue != _value) {
            _value = newValue
            updateFreshSubscribers()
        }
    }

    override fun notifyStale() {
        // no-op for PUSH-based computations
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun toString(): String = "Observable-$name(value=$_value, epoch=$lastRunEpoch)"

}