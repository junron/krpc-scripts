import krpc.client.Connection
import krpc.client.services.SpaceCenter
import util.Manoeuvre
import util.Mechanics

object Synchronous {
    @JvmStatic
    fun main(args: Array<String>) {
        val connection = Connection.newInstance(
            "Synchronous",
            "localhost",
            6666,
            6667
        )
        val sc = SpaceCenter.newInstance(connection)
        sc.save("krpc")
        val vessel = sc.activeVessel
        println("Vessel: ${vessel.name}")
        val targetAp = Mechanics.calculateSynchronous(vessel.orbit.body)
        println(targetAp)
        repeat(2) {
            val dv = Mechanics.visVivaDv(
                vessel.orbit,
                vessel.orbit.apoapsis,
                targetAp
            )
            val node = vessel.control.addNode(
                sc.ut + vessel.orbit.timeToApoapsis,
                dv.toFloat(),
                0F,
                0F
            )
            Manoeuvre.executeManoeuvre(sc, vessel, node)
        }

        connection.close()
    }
}
