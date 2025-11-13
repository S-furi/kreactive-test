import core.Subscriber
import graph.ObservableProvider
import java.util.Stack
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages the dependency-tracking stack.
 *
 * When a computed value (like Signal or Observable) runs its
 * `compute` function, it first pushes itself into this stack.
 *
 * When any other [core.Provider]'s `get()` is called, it checks
 * this stack. If a subscriber is on the stack, it adds that subscriber as a dependant.
 */
object DependencyTracker {

    /**
     * A global version number for all reactive computations.
     * Incremented every time any subscriber re-runs.
     */
    private val globalEpoch = AtomicLong(0)

    private val subscribersStack = ThreadLocal.withInitial { Stack<Subscriber>() }

    /**
     * Tracks a read operation.
     *
     * If a subscriber is currently computing, registers that subscriber
     * as a dependant of the node being popped from the stack.
     */
    fun track(node: ObservableProvider<*>) {
        subscribersStack.get().peekOrNull()?.let {
            node.addSubscriber(it)
        }
    }

    /**
     * Runs the given [action][compute] while tracking the calling
     * [Subscriber].
     */
    fun <T> Subscriber.runAndTrack(compute: () -> T): T {
        lastRunEpoch = globalEpoch.incrementAndGet()
        val stack = subscribersStack.get().also { it.push(this) }
        try {
            return compute()
        } finally {
            stack.pop()
        }
    }

    fun isCurrentlyTracking(): Boolean = this.subscribersStack.get().isNotEmpty()

    private fun <T> Stack<T>.peekOrNull(): T? = runCatching { peek() }.getOrNull()
}