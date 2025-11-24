package reactive

import reactive.DependencyTracker.runAndTrack
import reactive.DependencyTracker.track
import reactive.core.base.Computation
import reactive.core.base.Subscriber
import java.util.PriorityQueue
import kotlin.math.max

/**
 * Manages the dependency-tracking stack.
 *
 * When a computed value (like Signal or Observable) runs its
 * `compute` function, it first pushes itself into this stack.
 *
 * When any other [reactive.core.base.Provider]'s `get()` is called, it checks
 * this stack. If a subscriber is on the stack, it adds that subscriber as a dependant.
 */
object DependencyTracker {
    /**
     * A global version number for all reactive computations.
     * Incremented every time any subscriber re-runs.
     */
    private var globalEpoch: ULong = 0u

    private val subscribersStack = ArrayList<Subscriber>()
    private val subscribersOnStack = HashSet<Subscriber>()

    private var isInsideTransaction = false

    private val transactionQueue = PriorityQueue<Subscriber> { a, b -> a.level.compareTo(b.level) }

    private val transactionComputations = HashMap<Subscriber, () -> Unit>()

    /**
     * Tracks a read operation of the given [node].
     * The [Subscriber] at the top of the stack has called the
     * [get] method of the [node], triggering its computation, meaning
     * that the subscriber at the top of the stack is depending on
     * the input [node]. Being a dependency, the subscriber execution
     * depth ([Subscriber.level]) must not be less than its current
     * level or one level deeper than the input [node].
     */
    fun track(node: Computation<*>) {
        if (subscribersStack.isNotEmpty()) {
            val sub = subscribersStack.last()
            node.addSubscriber(sub)
            sub.updateLevel { max(it, node.level + 1) }
        }
    }

    /**
     * Runs the given [action][compute] while tracking the calling
     * [Subscriber].
     */
    fun <T> Subscriber.runAndTrack(compute: () -> T): T {
        check(subscribersOnStack.add(this)) {
            "Circular dependency detected! $this is already being tracked."
        }
        subscribersStack.add(this)

        lastRunEpoch = ++globalEpoch
        updateLevel { 0 }

        try {
            return compute()
        } finally {
            subscribersStack.removeLast()
            subscribersOnStack.remove(this)
        }
    }

    fun isCurrentlyTracking(): Boolean = subscribersStack.isNotEmpty()

    fun transaction(body: () -> Unit) {
        if (isInsideTransaction) return body()
        isInsideTransaction = true
        try {
            body()
        } finally {
            try {
                runTransactions()
            } finally {
                isInsideTransaction = false
                transactionQueue.clear()
                transactionComputations.clear()
            }
        }
    }

    fun Subscriber.appendToCurrentTransaction(computation: () -> Unit) {
        if (isInsideTransaction) {
            if (transactionComputations.put(this, computation) == null) {
                transactionQueue.offer(this)
            }
        } else {
            computation()
        }
    }

    /**
     * Runs all the transactions topologically sorted by their execution level.
     *
     * Nodes need to know their "depth" in the execution graph. Generally, when
     * a computation run, it must reset its level and re-calculate it based on the
     * dependencies it tracks (hence the reset in [runAndTrack] and the increment in [track]).
     *
     * Suppose the following example for better clarifying:
     * ```
     * A = Source
     * B = Observable { A }
     * C = Observable { A, B }
     *
     * ┌───────►B───────┐
     * │                │
     * │                ▼
     * A ─────────────► C
     * ```
     * When updating A, without sorting topologically and just inserting the keys in a sorted set (or map), we could
     * end up with the set `[C, B]` (depends on insertion order of [source's][reactive.core.Source] subscribers). When
     * the scheduler starts draining the set, it executes C that uses updated value of A, but with stale value of B.
     * Then B is computed, pushing again in the stack C's computation which ultimately run with correct parameters.
     *
     * This is why the topological sort (through [level][Subscriber.level]) is essential for correctness.
     * It guarantees that no node ever runs until all of its dependencies have already been updated in the same
     * transaction.
     */
    private fun runTransactions() {
        while(!transactionQueue.isEmpty()) {
            val sub = transactionQueue.poll()
            transactionComputations.remove(sub)?.invoke()
        }
    }

    internal fun reset() {
        globalEpoch = 0u
        subscribersStack.clear()
        subscribersOnStack.clear()
        transactionQueue.clear()
        transactionComputations.clear()
        isInsideTransaction = false
    }
}
