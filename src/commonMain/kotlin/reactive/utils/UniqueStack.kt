package reactive.utils

/**
 * A simple stack for storing unique elements granting
 * constant time addition (on average, it depends on time complexity
 * of addition on the backing [LinkedHashSet]), removal and lookup.
 *
 * @param T the type of the items of this stack.
 */
class UniqueStack<T> {
    private val stack = ArrayDeque<T>()
    private val set = LinkedHashSet<T>()

    val size: Int
        get() = stack.size

    fun push(item: T): Boolean = set.add(item).apply { if (this) stack.addLast(item) }

    fun pop(): T? = stack.removeLastOrNull()?.apply { set.remove(this) }

    fun peek(): T? = stack.lastOrNull()

    fun contains(item: T): Boolean = set.contains(item)

    fun isEmpty(): Boolean = stack.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    fun clear() {
        stack.clear()
        set.clear()
    }
}
