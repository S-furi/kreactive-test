package utils

import java.util.Stack

/**
 * A simple stack for storing unique elements granting
 * constant time addition (on average, it depends on time complexity
 * of addition on the backing [LinkedHashSet]), removal and lookup.
 *
 * @param T the type of the items of this stack.
 */
class UniqueStack<T> : LinkedHashSet<T>() {
    private val stack = Stack<T>()

    override var size: Int = 0
        private set

    fun push(item: T): Boolean {
        if (!contains(item)) {
            size++
            stack.push(item)
            return add(item)
        }
        return false
    }

    fun pop(): T? =
        runCatching {
            stack.pop()
        }.getOrNull()?.also {
            size--
            remove(it)
        }

    fun peek(): T? =
        runCatching {
            stack.peek()
        }.getOrNull()
}
