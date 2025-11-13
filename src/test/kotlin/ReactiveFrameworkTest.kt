import org.junit.jupiter.api.Test

import dsl.eagerObserving
import dsl.lazyObserving
import dsl.source
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

class ReactiveFrameworkTest {

    @Test
    @DisplayName("Test Push (Observable) vs. Pull (Signal) behavior")
    fun testPushVsPullBehavior() {
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
    @DisplayName("Test Dynamic Dependencies and Lazy Unlinking")
    fun testDynamicDependenciesAndLazyUnlinking() {
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
    @DisplayName("Test guardrail prevents mutation inside a computed block")
    fun testComputedBlocksCannotMutateSources() {
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

        val exception = assertThrows<IllegalStateException> {
            b = true
        }

        assertThrows<IllegalStateException> {
            println(lazyComp)
        }

        assertTrue(
            exception.message!!.contains("Cannot set Source"),
            "Exception message should indicate a forbidden mutation"
        )
    }
}