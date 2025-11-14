package reactive.core

import reactive.DependencyTracker
import reactive.DependencyTracker.appendToCurrentTransaction
import reactive.DependencyTracker.runAndTrack
import reactive.core.base.Subscriber
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
) : reactive.core.base.Computation<T>(),
    Subscriber {
    @Volatile private var current: T = runAndTrack(compute)

    @Volatile override var lastRunEpoch: ULong = 0u

    @Volatile override var level: Int = 0
        private set

    override fun get(): T {
        DependencyTracker.track(this)
        return current
    }

    override fun <S> map(transform: (T) -> S): Observable<S> = Observable("[mapped]-$name") { transform(compute()) }

    override fun notifyUpdate() {
        appendToCurrentTransaction {
            val newValue = runAndTrack(compute)
            if (newValue != current) {
                current = newValue
                updateFreshSubscribers()
            }
        }
    }

    override fun notifyStale() {
        // no-op for PUSH-based computations
    }

    override fun updateLevel(newLevel: (Int) -> Int) {
        level = newLevel(level)
    }

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = get()

    override fun toString(): String = "Observable-$name(value=$current, epoch=$lastRunEpoch)"
}
