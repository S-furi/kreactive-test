package compare

import reactive.dsl.lazyObserving
import reactive.dsl.source
import reactive.push.MutableObservable.Companion.observe
import kotlin.math.abs

class SignalAgent {
    var position by source(0.0)
    var obstacleLocation by source(10.0)

    val distance by lazyObserving {
        abs(obstacleLocation - position)
    }

    val dangerLevel by lazyObserving {
        if (distance < 2.0) "HIGH" else "LOW"
    }
}

class PushAgent {
    val position = observe(0.0)
    val obstacleLocation = observe(10.0)

    val distance = position.mergeWith(obstacleLocation) { pos, obs ->
        abs(pos - obs)
    }

    val dangerLevel = distance.map { dist -> if (dist < 2.0) "HIGH" else "LOW" }
}