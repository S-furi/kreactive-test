import dsl.eagerObserving
import dsl.lazyObserving
import dsl.source

fun base() {
    println("--- DES Simulator Reactive Demo ---")

    // 1. PUSH MODEL for time and events
    // These are "Sources" that we will set manually.
    var simulationTime by source(0.0)
    var eventQueueCount by source(0)

    println("Initial Time: $simulationTime")
    println("Initial Events: $eventQueueCount")

    // This is an Observable (push). It will re-calculate *immediately*
    // when either `simulationTime` or `eventQueueCount` changes.
    val canRunNextEvent by eagerObserving {
        val time = simulationTime
        val count = eventQueueCount
        println("  (Push) Re-computing `canRunNextEvent`...")
        time < 100.0 && count > 0
    }
    println("Can Run Next Event? $canRunNextEvent")


    // 2. PULL MODEL for expensive calculations
    // This is a Signal (pull). It will *not* run yet.
    val expensiveStatistic by lazyObserving {
        val time = simulationTime
        println("  (Pull) LAZILY Re-computing `expensiveStatistic`...")
        // Simulate a big, heavy calculation
        (1..1_000_000).fold(time) { acc, i -> acc + (i % 100) * 0.00001 }
    }


    // 3. RUNNING THE SIMULATION
    println("\n--- Setting time to 1.0 ---")
    simulationTime = 1.0
    // OBSERVE: `canRunNextEvent` re-computed, but `expensiveStatistic` did NOT.
    // It was only marked "dirty".

    println("\n--- Setting event count to 5 ---")
    eventQueueCount = 5
    // OBSERVE: `canRunNextEvent` re-computed again.
    println("Can Run Next Event? $canRunNextEvent")


    println("\n--- Now, someone *asks* for the expensive stat ---")
    // NOW the computation for `expensiveStatistic` finally runs.
    println("Expensive Stat Value: $expensiveStatistic")

    println("\n--- Asking for the stat again (should be cached) ---")
    // This time, `get()` is cheap. The "dirty" flag is false.
    println("Expensive Stat Value: $expensiveStatistic")


    println("\n--- Setting time to 2.0 (invalidating the stat) ---")
    simulationTime = 2.0
    // OBSERVE: `canRunNextEvent` (Push) re-computes.
    // `expensiveStatistic` (Pull) is just marked dirty.

    println("\n--- Asking for the stat again (must recompute) ---")
    // The "dirty" flag is true, so it re-runs the computation.
    println("Expensive Stat Value: $expensiveStatistic")
}

fun cond() {
    println("--- Dynamic Dependency Demo (Lazy Unlinking) ---")

    var cond by source(true)
    var a by source("A")
    var b by source("B")

    val computed by eagerObserving {
        println("  (Push) Re-computing `computed`...")
        if (cond) {
            "Cond is true, value is: $a"
        } else {
            "Cond is false, value is: $b"
        }
    }

    println("Initial value: $computed")

    println("\n--- 1. Setting `a` to 'A2' (should recompute) ---")
    a = "A2"
    println("Value: $computed")

    println("\n--- 2. Setting `b` to 'B2' (should NOT recompute) ---")
    println("  (Setting `b`, no recompute expected)")
    b = "B2"
    println("Value: $computed")

    println("\n--- 3. Setting `cond` to false (should recompute) ---")
    cond = false
    println("Value: $computed")

    println("\n--- 4. Setting `a` to 'A3' (STALE DEPENDENCY TEST) ---")
    println("  (Setting `a`, no recompute expected. Lazy unlinking will happen inside `a.set`)")
    a = "A3"
    println("Value: $computed")

    println("\n--- 5. Setting `b` to 'B3' (should recompute) ---")
    b = "B3"
    println("Value: $computed")

    println("\n--- 6. Setting `a` to 'A4' (STALE DEPENDENCY TEST 2) ---")
    println("  (Setting `a`, no recompute expected. Should be fully unlinked now.)")
    a = "A4"
    println("Value: $computed")
}

fun main() {
    base()
//    cond()
}