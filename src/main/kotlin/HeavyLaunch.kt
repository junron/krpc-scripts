import Launch.boosterManoeuvre
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import krpc.client.Connection
import krpc.client.services.SpaceCenter
import org.javatuples.Triplet
import util.*

object HeavyLaunch {
    suspend fun main(args: Array<String>) {
        val connection = Connection.newInstance(
            "Heavy Launch",
            "localhost",
            6666,
            6667
        )
        val sc = SpaceCenter.newInstance(connection)
        sc.save("krpc")
        val printer = TimedPrinter(sc)
        val vessel = sc.activeVessel
        val craft = vessel.name
        printer.print("Vessel: ${vessel.name}")
        val ap = vessel.autoPilot
        val control = vessel.control

        control.throttle = 1F
        delay(1000)
        control.activateNextStage()
        printer.print("Launch")
        control.sas = true
        delay(3000)
        ap.apply {
            engage()
            targetPitchAndHeading(90F, 90F)
        }
        val targetAp = 100_000
        val ref = vessel.orbit.body.referenceFrame
        var speedStreamTag = 0
        var fuelStreamTag = 0
        val speedStream =
            connection.addStream<Double>(vessel.flight(ref), "getSpeed")
        speedStreamTag = speedStream.addCallback { speed ->
            if (speed in 100.0..650.0) {
                ap.targetPitchAndHeading((90F - speed / 10F).toFloat(), 90F)
            }
            if (speed > 700)
                speedStream.removeCallback(speedStreamTag)
        }
        speedStream.rate = 2F
        val tank = vessel.parts.withTag("booster_tank").first()
        val fuelStream = connection.addStream<Float>(
            vessel.resourcesInDecoupleStage(tank.decoupleStage, false),
            "amount",
            "LiquidFuel"
        )
        fuelStreamTag = fuelStream.addCallback {
            runBlocking {
                if (it < 10) {
                    control.throttle = 0F
                    delay(1000)
                    control.activateNextStage()
                    control.throttle = 1F
                    fuelStream.removeCallback(fuelStreamTag)
                    printer.print("Side booster separation")
                }
            }
        }
        fuelStream.rate = 2F
        fuelStream.start()
        speedStream.start()
        Events.waitApoapsis(50_000.0, vessel, connection)
        ap.referenceFrame = vessel.orbitalReferenceFrame
        ap.targetDirection = Triplet(0.0, 1.0, 0.0)
        Events.waitApoapsis(targetAp * 0.8, vessel, connection)
        control.throttle = 0.8F
        Events.waitApoapsis(targetAp * 0.9, vessel, connection)
        control.throttle = 0F
        printer.print("MECO")
        delay(3000)
        control.activateNextStage()
        val booster = sc.getVesselByName("$craft Probe")
            ?: return printer.print("No booster found")

        control.throttle = 0.4F
        printer.print("Second stage start")
        delay(2000)
        control.throttle = 0F
        //    Allow for sufficient stage separation
        delay(5000)
        printer.print("Stage separation")
        boosterManoeuvre(booster, printer, sc)
        Events.waitAltitude(70_000.0, vessel, connection, true)
        control.setActionGroup(1, true)
        printer.print("Panels extended")
        control.throttle = 0.4F
        Events.waitApoapsis(targetAp.toDouble(), vessel, connection)
        control.throttle = 0F
        val node = control.addNode(
            sc.ut + vessel.orbit.timeToApoapsis,
            Mechanics.circularize(vessel).toFloat(),
            0F, 0F
        )
        Manoeuvre.executeManoeuvre(sc, vessel, node)
        printer.print("Circularized")
        delay(10000)
        printer.print("Switching view to booster")
        sc.activeVessel = booster
        Events.waitAltitude(40_000.0, booster, connection)
        booster.control.brakes = true
        booster.control.gear = true
        booster.parts.parachutes.forEach { it.deploy() }
        printer.print("Booster gear down")
        Events.waitAltitude(20.0, booster, connection)
        printer.print("Booster landed")
        connection.close()
    }

}

suspend fun main() {
    HeavyLaunch.main(emptyArray())
}
