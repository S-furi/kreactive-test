package utils

import java.util.Stack

/**
 * A simple stack for storing unique elements granting
 * constant time addition, removal and lookup.
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
