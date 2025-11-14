@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import reactive.DependencyTracker
import reactive.core.Signal
import reactive.dsl.eagerObserving
import reactive.dsl.lazyObserving
import reactive.dsl.source
import kotlin.test.Test

@Suppress("UNUSED")
class TopologyTests {
    @BeforeEach
    fun resetState() {
        DependencyTracker.reset()
    }

    @Test
    fun `testing topology 1 (eager)`() {
        // ┌───────►B───────┐
        // │                │
        // │                ▼
        // A ─────────────► C

        var cComputeCount = -1

        var a by source("Hello")
        val b by eagerObserving { a.uppercase() }
        val c by eagerObserving {
            cComputeCount++
            "$a, $b"
        }

        a = "Hello World!"
        assertEquals(1, cComputeCount) {
            "C should have been called once!"
        }
    }

    @Test
    fun `testing topology 1 (lazy)`() {
        // ┌───────►B───────┐
        // │                │
        // │                ▼
        // A ─────────────► C

        var cComputeCount = 0

        var a by source("Hello")
        val b by lazyObserving { a.uppercase() }
        val c by lazyObserving {
            cComputeCount++
            "$a, $b"
        }

        a = "Hello World!"
        c
        b
        assertEquals(1, cComputeCount) {
            "C should have been called once!"
        }
        cComputeCount = 0
        a = "Hello"
        b
        c
        assertEquals(1, cComputeCount) {
            "C should have been called once!"
        }
    }

    @Test
    fun `test Signal diamond dependency is not called twice`() {
        // ┌──────►B────┐
        // │            │
        // │            │
        // │            ▼
        // A             D
        // │            ▲
        // │            │
        // └─────►C─────┘

        var a by source(10)
        var bComputeCount = 0
        var cComputeCount = 0
        var dComputeCount = 0

        val b by lazyObserving {
            bComputeCount++
            a * 2
        }
        val c by lazyObserving {
            cComputeCount++
            a + 5
        }
        val d by lazyObserving {
            dComputeCount++
            b + c
        }

        assertEquals(35, d)
        assertEquals(1, bComputeCount)
        assertEquals(1, cComputeCount)
        assertEquals(1, dComputeCount)

        a = 20
        assertEquals(1, bComputeCount, "B should not recompute yet")
        assertEquals(1, cComputeCount, "C should not recompute yet")
        assertEquals(1, dComputeCount, "D should not recompute yet")

        listOf("should recompute", "should use cache").forEach { msg ->
            assertEquals(65, d)
            assertEquals(2, bComputeCount, "B $msg")
            assertEquals(2, cComputeCount, "C $msg")
            assertEquals(2, dComputeCount, "D $msg")
        }
    }

    @Test
    fun `test Observable diamond dependency is not called twice`() {
        // ┌──────►B────┐
        // │            │
        // │            │
        // │            ▼
        // A             D
        // │            ▲
        // │            │
        // └─────►C─────┘

        var a by source(10)
        // starting from -1 because eager stuff triggers recomputation right away
        var dComputeCount = -1

        val b by eagerObserving { a * 2 }
        val c by eagerObserving { a + 5 }

        val d by eagerObserving {
            dComputeCount++
            b + c
        }

        a = 20

        assertEquals(
            1,
            dComputeCount,
            "D was expected to be updated once, but it has been updated $dComputeCount",
        )
    }

    @Test
    fun `circular dependencies should be detected and throw IllegalStateException`() {
        lateinit var a: Signal<Int>
        lateinit var b: Signal<Int>

        a =
            Signal("A") {
                b.get() + 1
            }

        b =
            Signal("B") {
                a.get() + 2
            }

        assertThrows<IllegalStateException> { a.get() }
    }
}
