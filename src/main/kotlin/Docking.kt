import krpc.client.Connection
import krpc.client.services.SpaceCenter
import util.Manoeuvre
import util.Mechanics
import kotlin.math.min

object Docking {
    fun main(args: Array<String>) {
        val connection = Connection.newInstance(
            "Docking 1",
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
        val target = sc.targetVessel
        val vOrbit = vessel.orbit
        val tOrbit = target.orbit
            val dnTime = vOrbit.uTAtTrueAnomaly(vOrbit.trueAnomalyAtDN(tOrbit))
            val anTime = vOrbit.uTAtTrueAnomaly(vOrbit.trueAnomalyAtAN(tOrbit))
            val minTime = min(dnTime, anTime)
            val an = minTime == anTime
            val ta = if (an) vOrbit.trueAnomalyAtAN(tOrbit) else vOrbit.trueAnomalyAtDN(
                tOrbit
            )
            val predictedDv = util.Mechanics.inclinationChange(vOrbit, tOrbit, ta)
            val node = control.addNode(
                minTime,
                0F,
                (if (an) -predictedDv else predictedDv).toFloat(),
                0F
            )
            Manoeuvre.executeManoeuvre(sc, vessel, node)
//        val orbitalRef = tOrbit.body.nonRotatingReferenceFrame
//        val timeToTargetAp = tOrbit.timeToApoapsis
//        val altAtTargetAp = vOrbit.radiusAt(timeToTargetAp + sc.ut)
//        println(timeToTargetAp)
//        println(altAtTargetAp)
//        val dv = Mechanics.visVivaDv(
//            vOrbit,
//            altAtTargetAp,
//            tOrbit.periapsis
//        )
//        println(dv)
//
//        val periapsisLowering = control.addNode(
//            sc.ut + timeToTargetAp,
//            dv.toFloat(),
//            0F,
//            0F
//        )
//        Manoeuvre.executeManoeuvre(sc, vessel, periapsisLowering)
        connection.close()
    }
}

fun main() {
    Docking.main(emptyArray())
}
