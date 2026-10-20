package fr.courtillon.onimobile.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkStatusTest {
    @Test
    fun theLinkSaysWhetherAPressReachesTheDesktop() {
        val up = LinkStatus.line(LinkModel.Status.CONNECTED, "Studio", 11.6, waiting = false, problem = null)
        assertEquals(LinkLine(LinkHealth.CONNECTED, "Connected to Studio · 12 ms"), up)
        assertEquals(LinkHealth.WAITING, LinkStatus.line(LinkModel.Status.CONNECTED, "Studio", 12.0, true, null).health)
        val again = LinkStatus.line(LinkModel.Status.CONNECTING, "Studio", null, false, "The link to the desktop dropped.")
        assertEquals(LinkHealth.RECONNECTING, again.health)
        assertTrue(again.text.endsWith("The link to the desktop dropped."))
        val down = LinkStatus.line(LinkModel.Status.OFFLINE, "Studio", null, false, null)
        assertTrue(down.text.contains("nothing pressed here reaches it"))
        // Twin: a link redialling is never read as connected, whatever its last round trip.
        assertNotEquals(LinkHealth.CONNECTED, LinkStatus.line(LinkModel.Status.CONNECTING, "Studio", 3.0, false, null).health)
    }
}
