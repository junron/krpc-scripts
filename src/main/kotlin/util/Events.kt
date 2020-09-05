package util

import krpc.client.Connection
import krpc.client.services.KRPC
import krpc.client.services.SpaceCenter

object Events {
    fun waitAltitude(
        altitude: Double,
        vessel: SpaceCenter.Vessel,
        krpc: KRPC,
        connection: Connection,
        climbing: Boolean = false
    ) {
        val ref = vessel.orbit.body.referenceFrame
        val meanAltitude =
            connection.getCall(vessel.flight(ref), "getSurfaceAltitude")
        val expression = if (climbing) {
            KRPC.Expression.greaterThanOrEqual(
                connection,
                KRPC.Expression.call(connection, meanAltitude),
                KRPC.Expression.constantDouble(connection, altitude)
            )
        } else {
            KRPC.Expression.lessThanOrEqual(
                connection,
                KRPC.Expression.call(connection, meanAltitude),
                KRPC.Expression.constantDouble(connection, altitude)
            )
        }
        val event = krpc.addEvent(expression)
        synchronized(event.condition) {
            event.waitFor()
        }
    }

    fun waitApoapsis(
        apoapsis: Double,
        vessel: SpaceCenter.Vessel,
        krpc: KRPC,
        connection: Connection
    ) {
        val meanAltitude =
            connection.getCall(vessel.orbit, "getApoapsisAltitude")
        val expression = KRPC.Expression.greaterThanOrEqual(
            connection,
            KRPC.Expression.call(connection, meanAltitude),
            KRPC.Expression.constantDouble(connection, apoapsis)
        )
        val event = krpc.addEvent(expression)
        synchronized(event.condition) {
            event.waitFor()
        }
    }

    fun waitVelocity(
        velocity: Double,
        vessel: SpaceCenter.Vessel,
        krpc: KRPC,
        connection: Connection
    ) {
        val ref = vessel.orbit.body.referenceFrame
        val verticalSpeed =
            connection.getCall(vessel.flight(ref), "getVerticalSpeed")
        val expression = KRPC.Expression.greaterThanOrEqual(
            connection,
            KRPC.Expression.call(connection, verticalSpeed),
            KRPC.Expression.constantDouble(connection, -velocity)
        )
        val event = krpc.addEvent(expression)
        synchronized(event.condition) {
            event.waitFor()
        }
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
