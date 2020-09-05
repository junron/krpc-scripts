package util

import krpc.client.services.SpaceCenter
import kotlin.math.floor

class TimedPrinter(val sc: SpaceCenter) {
    val startTime = sc.ut

    private fun formatSeconds(seconds: Int): String {
        val minutes = seconds/60
        val seconds = seconds % 60
        return "${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}"
    }

    fun print(string: String){
        println("[T+${formatSeconds((sc.ut - startTime).toInt())}]: $string")
    }
}
