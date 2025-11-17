package reactive.core.base

import org.slf4j.LoggerFactory
import reactive.core.Observer
import reactive.core.Signal
import java.util.WeakHashMap

abstract class Computation<T> : Provider<T> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    open val level: Int = 0

    /**
     * Keep a reference to all [Subscribers][Subscriber] along with
     * the [last run epoch][Subscriber.lastRunEpoch] counter it had when it last *read* this node.
     */
    protected val subscribers: MutableMap<Subscriber, ULong> = WeakHashMap()

    fun addSubscriber(sub: Subscriber) {
        if (this is Signal<*> && sub is Observer<*>) {
            logger.warn(
                "Calling from an eager computation a lazy computation will \n" +
                    "pull the value on every update, making it effectively an eager (push) computation.\n" +
                    "This can be tolerated, however consider that you are no more exploiting lazy computation.",
            )
        }
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
