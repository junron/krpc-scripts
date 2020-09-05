package util

import krpc.client.services.SpaceCenter
import org.javatuples.Triplet

object Manoeuvre {
    fun executeManoeuvre(
        sc: SpaceCenter,
        vessel: SpaceCenter.Vessel,
        node: SpaceCenter.Node
    ) {
        val burnTime = Mechanics.burnTime(vessel, node.deltaV.toFloat())
        val ap = vessel.autoPilot
        ap.engage()
        ap.referenceFrame = node.referenceFrame
        ap.targetDirection = Triplet(0.0, 1.0, 0.0)
        ap.wait_()
        sc.warpTo(node.ut - burnTime / 2, 100000F, 100000F)
        vessel.control.throttle = 1F
        Thread.sleep((burnTime * 1000).toLong())
        vessel.control.throttle = 0F
        node.remove()
    }
}
