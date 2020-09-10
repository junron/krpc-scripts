import krpc.client.Connection
import krpc.client.services.SpaceCenter
import org.javatuples.Triplet
import util.Events
import util.Manoeuvre
import util.Mechanics
import util.TimedPrinter

object Deorbit {
    @JvmStatic
    suspend fun main(args: Array<String>) {
        val connection = Connection.newInstance(
            "Deorbit",
            "localhost",
            6666,
            6667
        )
        val sc = SpaceCenter.newInstance(connection)
        sc.save("krpc")
        val printer = TimedPrinter(sc)
        val vessel = sc.activeVessel
        printer.print("Vessel: ${vessel.name}")
        val ap = vessel.autoPilot
        val control = vessel.control
        ap.apply {
            engage()
            referenceFrame = vessel.orbit.body.referenceFrame
        }
        val dv = Mechanics.visVivaDv(
            vessel.orbit,
            vessel.orbit.apoapsis,
            vessel.orbit.body.equatorialRadius.toDouble() - 100_000
        )
        val node = control.addNode(
            sc.ut + vessel.orbit.timeToApoapsis,
            dv.toFloat(),
            0F,
            0F
        )
        Manoeuvre.executeManoeuvre(sc, vessel, node)

        control.throttle = 0F
        ap.referenceFrame = vessel.orbitalReferenceFrame
        ap.targetDirection = Triplet(0.0, 1.0, 0.0)
        ap.wait_()
        vessel.parts.decouplers.forEach {
            if (it.staged) {
                it.decouple()
            }
        }
        printer.print("Second stage decoupled")

        control.antennas = false
        control.solarPanels = false
        printer.print("Panels retracted")
        Thread.sleep(10000)
        ap.targetDirection = Triplet(0.0, -1.0, 0.0)
        Events.waitAltitude(20_000.0, vessel, connection)
        vessel.parts.parachutes.forEach {
            it.deploy()
        }
        ap.disengage()
        control.sas = true
        control.sasMode = SpaceCenter.SASMode.RETROGRADE
        Events.waitAltitude(10.0, vessel, connection)
        printer.print("Landed")
        connection.close()
    }

}
