package fr.courtillon.onimobile.link

import kotlin.math.roundToInt

/** How the link reads at the top of the remote: well, waiting on an answer, redialling, or down. */
enum class LinkHealth { CONNECTED, WAITING, RECONNECTING, OFFLINE }

data class LinkLine(val health: LinkHealth, val text: String)

object LinkStatus {
    /** A press unanswered this long is said out loud: the desktop may be gone. */
    const val PATIENCE_MS = 1_000L

    fun line(status: LinkModel.Status, name: String, roundTripMs: Double?, waiting: Boolean, problem: String?): LinkLine {
        val why = problem?.let { " $it" }.orEmpty()
        return when (status) {
            LinkModel.Status.CONNECTED -> if (waiting) {
                LinkLine(LinkHealth.WAITING, "Waiting for $name to answer…")
            } else {
                val trip = roundTripMs?.let { " · ${maxOf(1, it.roundToInt())} ms" }.orEmpty()
                LinkLine(LinkHealth.CONNECTED, "Connected to $name$trip")
            }
            LinkModel.Status.CONNECTING -> LinkLine(LinkHealth.RECONNECTING, "Reconnecting to $name… Presses wait until it answers.$why")
            LinkModel.Status.OFFLINE, LinkModel.Status.UNPAIRED ->
                LinkLine(LinkHealth.OFFLINE, "Not connected to $name: nothing pressed here reaches it.$why")
        }
    }
}
