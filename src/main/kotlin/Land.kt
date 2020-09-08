import krpc.client.Connection
import krpc.client.services.SpaceCenter
import org.javatuples.Triplet
import util.Events
import util.Mechanics

object Land {
    @JvmStatic
    suspend fun main(args: Array<String>) {
        val connection = Connection.newInstance(
            "Booster landing",
            "localhost",
            6666,
            6667
        )
        val sc = SpaceCenter.newInstance(connection)
        sc.save("krpc")
        val vessel = sc.activeVessel
        println("Vessel: ${vessel.name}")
        val ap = vessel.autoPilot
        val control = vessel.control
        ap.apply {
            engage()
            referenceFrame = vessel.surfaceVelocityReferenceFrame
            targetDirection = Triplet(0.0, -1.0, 0.0)
        }

        val ref = vessel.orbit.body.referenceFrame
        control.brakes = true
        control.gear = true
        while (true) {
            val flight = vessel.flight(ref)
            if (flight.drag.value1 < 0) {
                break
            }
            Thread.sleep(1000)
        }
        control.throttle = 0.2F
        Thread.sleep(3000)
        control.throttle = 0.5F
        while (true) {
            val flight = vessel.flight(ref)
            val horiz = flight.horizontalSpeed
            println("HORIZ $horiz")
            if (horiz < 3) {
                control.throttle = 0.0F
                if (horiz < 2 || flight.meanAltitude < 6000) {
                    break
                }
            } else if (horiz < 10) {
                control.throttle = 0.1F
            } else if (horiz < 20) {
                control.throttle = 0.2F
            }
            Thread.sleep(1000)
        }
        println("SAS ON")
        ap.disengage()
        control.sas = true
        control.sasMode = SpaceCenter.SASMode.STABILITY_ASSIST
        val suicideBurn = Mechanics.suicideBurn(vessel, vessel.flight(ref))
        println("Alt: ${suicideBurn.second}, Time: ${suicideBurn.first}")
        Events.waitAltitude(suicideBurn.second + 1000, vessel, connection)
        val suicideBurn2 = Mechanics.suicideBurn(vessel, vessel.flight(ref))
        println("Alt: ${suicideBurn2.second}, Time: ${suicideBurn2.first}")
        Events.waitAltitude(suicideBurn2.second, vessel, connection)
        control.throttle = 1F
        Events.waitVelocity(50.0, vessel, connection)
        control.throttle = 0F
        Events.waitAltitude(600.0, vessel, connection)
        var target = 50.0
        while (true) {
            val flight = vessel.flight(ref)
            val alt = flight.surfaceAltitude
            if (alt < 10) {
                control.throttle = 0F
                break
            } else if (alt < 70) {
                target = 10.0
            } else if (alt < 300) {
                control.rcs = true
            target = 30.0
        }
        val speed = -flight.verticalSpeed
        val proposedThrottle = control.throttle + when {
            speed < 0 -> -control.throttle
            else ->
                when {
                    speed > target -> {
                        0.05F
                    }
                    speed < target -> {
                        -0.1F
                    }
                    else -> {
                        0F
                    }
                }
        }
            control.throttle = proposedThrottle
            Thread.sleep(200)
        }
        connection.close()
    }

}
