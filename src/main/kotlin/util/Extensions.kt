package util

import krpc.client.services.SpaceCenter
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

val Float.radians: Float
    get() = (this * PI / 180).toFloat()
val Double.radians: Double
    get() = this * PI / 180

fun SpaceCenter.getVesselByName(name: String) =
    this.vessels.firstOrNull { it.name == name }

val SpaceCenter.Orbit.meanMotion: Double
    get() = (2 * PI) / period

// https://www.faa.gov/about/office_org/headquarters_offices/avs/offices/aam/cami/library/online_libraries/aerospace_medicine/tutorial/media/III.4.1.5_Maneuvering_in_Space.pdf
val SpaceCenter.Orbit.angularVelocity: Double
    get() = sqrt(body.gravitationalParameter / semiMajorAxis.pow(3))

fun SpaceCenter.Orbit.transferSemiMajorAxis(targetOrbit: SpaceCenter.Orbit) =
    (semiMajorAxis + targetOrbit.semiMajorAxis) / 2

fun SpaceCenter.Orbit.phasedAngle(targetOrbit: SpaceCenter.Orbit) =
    PI - (angularVelocity * PI * sqrt(transferSemiMajorAxis(targetOrbit).pow(3) / body.gravitationalParameter))

