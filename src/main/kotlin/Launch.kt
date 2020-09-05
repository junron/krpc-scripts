import kotlinx.coroutines.delay
import krpc.client.Connection
import krpc.client.services.KRPC
import krpc.client.services.SpaceCenter
import org.javatuples.Triplet
import util.*

suspend fun boosterManoeuvre(
    booster: SpaceCenter.Vessel,
    printer: TimedPrinter
) {
    with(booster) {
        autoPilot.referenceFrame = orbitalReferenceFrame
        autoPilot.engage()
        autoPilot.targetDirection = Triplet(0.0, 0.0, -1.0)
        this.control.throttle = 0.5F
        printer.print("Booster burn")
        delay(15000)
        this.control.throttle = 0F
        printer.print("Booster burn complete")
        // Point retrograde
        autoPilot.targetDirection = Triplet(0.0, -1.0, 0.0)

    }
}

suspend fun main() {
    val connection = Connection.newInstance(
        "R1-Simple Launch",
        "localhost",
        6666,
        6667
    )
    val krpc = KRPC.newInstance(connection)
    val sc = SpaceCenter.newInstance(connection)
    val printer = TimedPrinter(sc)
    val vessel = sc.activeVessel
    printer.print("Vessel: ${vessel.name}")
    val ap = vessel.autoPilot
    val control = vessel.control
    ap.apply {
        engage()
        targetPitchAndHeading(90F, 90F)
    }
    control.throttle = 1F
    delay(1000)
    control.activateNextStage()
    printer.print("Launch")
    val targetAp = 150_000
    val ref = vessel.orbit.body.referenceFrame
    while (true) {
        val flight = vessel.flight(ref)
        val speed = flight.speed
        if (speed > 650) {
            break
        }
        if (flight.speed > 100) {
            ap.targetPitchAndHeading((90F - speed / 10F).toFloat(), 90F)
        }
        delay(1000)
    }
    Events.waitApoapsis(45_000.0, vessel, krpc, connection)
    ap.referenceFrame = vessel.orbitalReferenceFrame
    ap.targetDirection = Triplet(0.0, 1.0, 0.0)
    Events.waitApoapsis(targetAp * 0.8, vessel, krpc, connection)
    control.throttle = 0.1F
    Events.waitApoapsis(targetAp * 0.9, vessel, krpc, connection)
    control.throttle = 0F
    printer.print("MECO")
    delay(3000)
    control.activateNextStage()
    val booster = sc.getVesselByName("R1-Simple Probe")
        ?: return printer.print("No booster found")

    control.throttle = 0.4F
    printer.print("Second stage start")
    delay(2000)
    control.throttle = 0F
//    Allow for sufficient stage separation
    delay(5000)
    printer.print("Stage separation")
    boosterManoeuvre(booster, printer)
    Events.waitAltitude(70_000.0, vessel, krpc, connection, true)
    control.setActionGroup(1, true)
    printer.print("Panels extended")
    control.throttle = 0.4F
    Events.waitApoapsis(targetAp.toDouble(), vessel, krpc, connection)
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
    Events.waitAltitude(40_000.0, booster, krpc, connection)
    booster.control.brakes = true
    booster.control.gear = true
    booster.parts.parachutes.forEach { it.deploy() }
    printer.print("Booster gear down")
    Events.waitAltitude(20.0, booster, krpc, connection)
    printer.print("Booster landed")
    connection.close()
}

