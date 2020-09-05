package util

import krpc.client.services.SpaceCenter
import krpc.client.services.SpaceCenter.Vessel
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

object Mechanics {
    fun circularize(vessel: Vessel): Double {
        val mu = vessel.orbit.body.gravitationalParameter
        val r = vessel.orbit.apoapsis
        val a1 = vessel.orbit.semiMajorAxis
        val a2 = r
        val v1 = sqrt(mu * ((2.0 / r) - (1.0 / a1)))
        val v2 = sqrt(mu * ((2.0 / r) - (1.0 / a2)))
        return v2 - v1
    }

    fun burnTime(vessel: Vessel, deltaV: Float): Double {
        val F = vessel.availableThrust
        val isp = vessel.specificImpulse * 9.81
        val m0 = vessel.mass
        val m1 = m0 / exp(deltaV / isp)
        val flowRate = F / isp
        return (m0 - m1) / flowRate
    }

    fun suicideBurn(
        vessel: Vessel,
        flight: SpaceCenter.Flight
    ): Pair<Double, Double> {
        val mass = vessel.mass
        val velocity = -flight.verticalSpeed
        val alt = flight.surfaceAltitude
        println("m: $mass, v: $velocity, alt: $alt, i:${vessel.specificImpulse}, f: ${vessel.maxThrust}, f2: ${vessel.availableThrust}")
        // T=\frac{\left(m_{i}-\frac{m_{i}}{e^{\left(\frac{V_{i}+\sqrt{2gD_{i}}}{9.8I}\right)}}\right)}{\left(\frac{F_{T}}{9.8\cdot I}\right)}
        val t =
            (mass - mass / exp((velocity + sqrt(2 * 9.81 * alt)) / (9.81 * vessel.specificImpulse))) / (vessel.maxThrust / (9.8 * vessel.specificImpulse))
        val burnAlt = ((velocity + sqrt(2 * 9.81 * alt)) / 2) * t
        return Pair(t, burnAlt)
    }

    fun inclinationChange(
        orbit: SpaceCenter.Orbit,
        targetOrbit: SpaceCenter.Orbit,
        ta: Double
    ): Double {
        // Radians
        val inclination = orbit.relativeInclination(targetOrbit)
        return (2 * sin(inclination / 2) *
            sqrt(1 - orbit.eccentricity * orbit.eccentricity) *
            cos(orbit.argumentOfPeriapsis + ta) * orbit.meanMotion * orbit.semiMajorAxis) /
            (1 + orbit.eccentricity * cos(orbit.trueAnomaly))
    }

    fun visViva(
        orbit: SpaceCenter.Orbit,
        originalHeight: Double,
        targetHeight: Double
    ): Double {
        val atx = (originalHeight + targetHeight)/2
        val mu = orbit.body.gravitationalParameter
        val vtx = sqrt(mu * (2.0 / originalHeight - 1.0 / atx))
        val v1 = sqrt(mu/originalHeight)
        return vtx - v1
    }
}
