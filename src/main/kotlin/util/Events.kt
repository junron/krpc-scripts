package util

import krpc.client.Connection
import krpc.client.services.SpaceCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Events {
    suspend fun waitAltitude(
        altitude: Double,
        vessel: SpaceCenter.Vessel,
        connection: Connection,
        climbing: Boolean = false
    ) = suspendCoroutine<Unit> { cont ->
        val ref = vessel.orbit.body.referenceFrame
        val stream = connection.addStream<Double>(
            vessel.flight(ref),
            "getSurfaceAltitude"
        )
        var tag = 0
        tag = stream.addCallback {
            if (climbing && it > altitude) {
                cont.resume(Unit)
                stream.removeCallback(tag)
            } else if (!climbing && it < altitude) {
                cont.resume(Unit)
                stream.removeCallback(tag)
            }
        }
        stream.rate = 2F
        stream.start()
    }

    suspend fun waitApoapsis(
        apoapsis: Double,
        vessel: SpaceCenter.Vessel,
        connection: Connection
    ) = suspendCoroutine<Unit> { cont ->
        var tag = 0
        val stream =
            connection.addStream<Double>(vessel.orbit, "getApoapsisAltitude")
        tag = stream.addCallback {
            if (it > apoapsis) {
                cont.resume(Unit)
                stream.removeCallback(tag)
            }
        }
        stream.rate = 2F
        stream.start()
    }

    suspend fun waitVelocity(
        velocity: Double,
        vessel: SpaceCenter.Vessel,
        connection: Connection
    ) = suspendCoroutine<Unit> { cont ->
        val ref = vessel.orbit.body.referenceFrame
        val stream =
            connection.addStream<Double>(vessel.flight(ref), "getVerticalSpeed")
        var tag = 0
        tag = stream.addCallback {
            if (-it > velocity) {
                cont.resume(Unit)
                stream.removeCallback(tag)
            }
        }
        stream.rate = 2F
        stream.start()
    }
//        fun waitDrag(
//        drag: Double,
//        vessel: SpaceCenter.Vessel,
//        krpc: KRPC,
//        connection: Connection
//    ) {
//        val ref = vessel.orbit.body.referenceFrame
//        val verticalSpeed =
//            connection.getCall(vessel.flight(ref), "drag")
//        val expression = KRPC.Expression.greaterThanOrEqual(
//            connection,
//            KRPC.Expression.call(connection, verticalSpeed),
//            KRPC.Expression.constantDouble(connection, -velocity)
//        )
//        val event = krpc.addEvent(expression)
//        synchronized(event.condition) {
//            event.waitFor()
//        }
//    }
}
