@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

import dsl.eagerObserving
import dsl.lazyObserving
import dsl.source
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReactiveFrameworkTest {
    @BeforeEach
    fun resetState() {
        DependencyTracker.reset()
    }

    @Test
    fun `test Source value setting idempotency`() {
        var a by source(10)
        var computeCount = 0
        val b by eagerObserving {
            computeCount++
            a
        }

        assertEquals(1, computeCount, "Observable should run once on init")
        a = 10
        assertEquals(
            1,
            computeCount,
            "Observable should not re-run when source is set to the same value",
        )
    }

    @Test
    fun `an observer can depend on another observer`() {
        var a by source("Hello")
        val b by eagerObserving {
            println(a)
            a.uppercase()
        }
        val c by eagerObserving {
            println("B computed from B: $b")
            "World!"
        }
    }

    @Test
    fun `test Signal is never computed if never read`() {
        var a by source(20)
        var pullComputeCount = 0

        val signal by lazyObserving {
            pullComputeCount++
            a * 10
        }

        a = 20
        a = 30

        assertEquals(
            0,
            pullComputeCount,
            "Signal should not run on dependency change if not accessed",
        )
    }

    @Test
    fun `an observable that depends on a signal should make it act as a push dependency`() {
        var a by source(10)
        var signalComputeCount = 0

        val pullSignal by lazyObserving {
            signalComputeCount++
            a * 2
        }

        val pushObservable by eagerObserving {
            pullSignal // This Observable depends on the Signal
        }

        a = 30

        assertEquals(
            2,
            signalComputeCount,
            "Signal recomputation has been triggered by it's Observable dependency",
        )
        assertEquals(60, pushObservable, "Observable has value updated")
    }

    @Test
    fun `exception in Observable is not swallowed`() {
        var a by source(10)
        val b by eagerObserving {
            if (a > 15) throw IllegalStateException("Test exception")
            a
        }
        assertThrows<IllegalStateException> {
            a = 20
        }
    }

    @Test
    fun `exception in Signals is not swallowed`() {
        var a by source(10)
        val b by lazyObserving {
            if (a > 15) throw IllegalStateException("Test exception")
            a
        }

        a = 20
        assertThrows<IllegalStateException> {
            val value = b
        }
    }

    @Test
    fun `test Push (Observable) vs Pull (Signal) behavior()`() {
        var simulationTime by source(0.0)
        var eventQueueCount by source(0)
        var pushComputeCount = 0
        var pullComputeCount = 0

        val canRunNextEvent by eagerObserving {
            pushComputeCount++
            val time = simulationTime
            val count = eventQueueCount
            time < 100.0 && count > 0
        }

        val expensiveStatistic by lazyObserving {
            pullComputeCount++
            val time = simulationTime
            (1..100).fold(time) { acc, i -> acc + (i % 10) * 0.01 }
        }

        assertEquals(1, pushComputeCount, "Eager observable should run on initialization")
        assertEquals(0, pullComputeCount, "Lazy signal should NOT run on initialization")
        assertFalse(canRunNextEvent)

        simulationTime = 1.0

        assertEquals(2, pushComputeCount, "Eager observable should re-run on dependency change")
        assertEquals(0, pullComputeCount, "Lazy signal should still not have run")

        eventQueueCount = 5

        assertEquals(3, pushComputeCount, "Eager observable should re-run again")
        assertTrue(canRunNextEvent)
        assertEquals(0, pullComputeCount)

        val stat1 = expensiveStatistic

        assertEquals(1, pullComputeCount, "Lazy signal should run on first 'get()'")
        assertTrue(stat1 > 1.0)

        val stat2 = expensiveStatistic

        assertEquals(1, pullComputeCount, "Lazy signal should use cached value")
        assertEquals(stat1, stat2)

        simulationTime = 2.0

        assertEquals(4, pushComputeCount, "Eager observable should re-run on time change")
        assertEquals(1, pullComputeCount, "Lazy signal should be marked stale, but not re-run yet")

        val stat3 = expensiveStatistic

        assertEquals(2, pullComputeCount, "Lazy signal must re-compute because it was stale")
        assertNotEquals(stat1, stat3)
    }

    @Test
    fun `test dynamic dependencies and lazy unlinking`() {
        var cond by source(true)
        var a by source("A")
        var b by source("B")
        var computeCount = 0

        val computed by eagerObserving {
            computeCount++
            if (cond) {
                "Cond is true, value is: $a"
            } else {
                "Cond is false, value is: $b"
            }
        }

        assertEquals(1, computeCount)
        assertEquals("Cond is true, value is: A", computed)

        a = "A2"

        assertEquals(2, computeCount, "Should recompute when active dependency 'a' changes")
        assertEquals("Cond is true, value is: A2", computed)

        b = "B2"

        assertEquals(2, computeCount, "Should NOT recompute when inactive dependency 'b' changes")
        assertEquals("Cond is true, value is: A2", computed)

        cond = false

        assertEquals(3, computeCount, "Should recompute when 'cond' changes")
        assertEquals("Cond is false, value is: B2", computed)

        a = "A3"

        assertEquals(3, computeCount, "Should NOT recompute when stale dependency 'a' changes")
        assertEquals("Cond is false, value is: B2", computed)

        b = "B3"

        assertEquals(4, computeCount, "Should recompute when new active dependency 'b' changes")
        assertEquals("Cond is false, value is: B3", computed)
    }

    @Test
    fun `guardrail should prevent mutation inside a computed block (observable or signal)`() {
        var a by source(10.0)
        var b by source(false)

        val comp by eagerObserving {
            if (b) {
                a += 10
            }
        }

        val lazyComp by lazyObserving {
            if (b) {
                a += 10
            }
        }

        val exception =
            assertThrows<IllegalStateException> {
                b = true
            }

        assertThrows<IllegalStateException> {
            println(lazyComp)
        }

        assertTrue(
            exception.message!!.contains("Cannot set Source"),
            "Exception message should indicate a forbidden mutation",
        )
    }
}
