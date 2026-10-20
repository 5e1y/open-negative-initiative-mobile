package fr.courtillon.onimobile.ds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** The Kotlin copy of the desktop's tokens, and the screens that must read them rather than numbers. */
class TokensTest {
    private val sources = File("src/main/java/fr/courtillon/onimobile")
    private val copy = File(sources, "ds/Tokens.kt")

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** The desktop's tokens.slint: `ONI_RS` names the Rust repository, else it sits beside this one. */
    private fun desktopTokens(): File? {
        val rs = System.getenv("ONI_RS")?.let(::File) ?: File("../../../open-negative-initiative-rs")
        return File(rs, "crates/oni-ui/ui/ds/tokens.slint").takeIf { it.isFile }
    }

    @Test
    fun theCopyWasGeneratedFromTheDesktopsTokensAsTheyStand() {
        val source = desktopTokens()
        assumeTrue("the Rust repository is not here: set ONI_RS to check the copy", source != null)
        val bytes = source!!.readBytes()
        val recorded = Regex("Source sha256: ([0-9a-f]{64})").find(copy.readText())?.groupValues?.get(1)
        assertEquals("Tokens.kt drifted from the desktop's tokens: run scripts/ds-tokens.py", sha256(bytes), recorded)
        // Twin: one byte more in the desktop's tokens no longer matches the copy.
        assertNotEquals(recorded, sha256(bytes + ' '.code.toByte()))
    }

    @Test
    fun theDesignSystemReadsTheGeneratedTokens() {
        assertEquals(Tokens.Palette.canvas, DS.Palette.canvas)
        assertEquals(Tokens.Scanner.keyHeight, DS.Scanner.keyHeight)
        assertEquals(Tokens.Scanner.bandCool.rgba, DS.Scanner.bandCool)
        // Twin: two bands of the keypad are two tokens, so the check tells them apart.
        assertNotEquals(DS.Scanner.bandCool, DS.Scanner.bandWarm)
    }

    /** A size, a colour or a duration written as a number in a screen, outside the design system. */
    private val literal = Regex("""\b\d+(\.\d+)?\.(dp|sp)\b|Color\(0x|Color\(\d""")

    @Test
    fun noScreenWritesALiteralSizeOrColour() {
        val screens = File(sources, "ui").listFiles { f -> f.extension == "kt" }.orEmpty()
        assertTrue(screens.isNotEmpty())
        val found = screens.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line -> if (literal.containsMatchIn(line)) "${file.name}:${i + 1}: $line" else null }
        }
        assertEquals(emptyList<String>(), found)
        // Twin: the screens' old literals are what the pattern finds.
        assertTrue(literal.containsMatchIn("Modifier.height(120.dp).clip(RoundedCornerShape(12.dp))"))
        assertTrue(literal.containsMatchIn("Text(\"Forget\", color = Color(0xFFFF6B5E))"))
    }
}
