package graph

import core.Provider
import core.Subscriber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

abstract class ObservableProvider<T> : Provider<T> {
    /**
     * Keep a reference to all [Subscribers][Subscriber] along with
     * the [las run epoch][Subscriber.lastRunEpoch] counter it had when it last *read* this node.
     */
    protected val subscribers: MutableMap<Subscriber, Long> = ConcurrentHashMap()

    fun addSubscriber(sub: Subscriber) {
        subscribers[sub] = sub.lastRunEpoch
    }

    fun removeSubscriber(sub: Subscriber) {
        subscribers.remove(sub)
    }

    /**
     * Update dependencies that are not stale.
     *
     * Staleness check is performed checking:
     * - `sub.lastRunEpoch`: The epoch of the *last time the subscriber ran*.
     * - `lastAccessEpoch`: The epoch it had when it *last read this Source*.
     */
    protected fun updateFreshSubscribers() {
        subscribers.keys.toList().forEach { sub ->
            subscribers[sub]?.let { lastAccessEpoch ->
                if (lastAccessEpoch < sub.lastRunEpoch) {
                    /**
                     * The subscriber has run *since* it last read this Source,
                     * and it did NOT read this Source in that new run.
                     * This is a stale dependency.
                     */
                    removeSubscriber(sub)
                } else {
                    sub.notifyStale()
                    sub.notifyUpdate()
                }
            }
        }
    }

    abstract override fun get(): T
}

/**
 * A writeable [Provider].
 *
 * This is the start of a reactive chain. When its value changes (by
 * means of [set]) it notifies both push and pull subscribers.
 */
class Source<T>(initialValue: T, private val name: String = UUID.randomUUID().toString()) : ObservableProvider<T>() {

    @Volatile private var _value: T = initialValue

    override fun get(): T {
        DependencyTracker.track(this)
        return _value
    }

    fun set(newValue: T) {
        if (_value == newValue) return
        _value = newValue
        updateFreshSubscribers()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

    override fun toString(): String = "SOURCE-$name(value=$_value)"
}