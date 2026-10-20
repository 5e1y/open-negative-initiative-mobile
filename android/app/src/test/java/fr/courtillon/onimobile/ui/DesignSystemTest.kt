package fr.courtillon.onimobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The screens are built from the design system's components, as the desktop's design_system check
 * holds its screens: no styled primitive, no Material widget and no keycap outside `ds/`.
 */
class DesignSystemTest {
    private val sources = File("src/main/java/fr/courtillon/onimobile")
    private val screens = File(sources, "ui").listFiles { f -> f.extension == "kt" }.orEmpty().toList()

    /** What only `ds/` may write: text and fields drawn raw, fills, edges, faces, sizes of type, Material. */
    private val styled = Regex(
        """\bText\(|\bBasicText\(|\bBasicTextField\(|\.background\(|\.border\(|RoundedCornerShape|CircleShape|""" +
            """fontSize|fontWeight|FontFamily|TextStyle\(|import androidx\.compose\.material|\bKeycap|ds\.keycap|""" +
            """\bTokens\.""",
    )

    private fun found(lines: List<Pair<String, String>>) =
        lines.filter { (_, line) -> !line.trimStart().startsWith("//") && !line.trimStart().startsWith("*") && styled.containsMatchIn(line) }
            .map { (at, line) -> "$at: ${line.trim()}" }

    @Test
    fun everyScreenComposesTheDesignSystemOnly() {
        assertTrue(screens.size >= 6)
        val lines = screens.flatMap { file -> file.readLines().mapIndexed { i, line -> "${file.name}:${i + 1}" to line } }
        assertEquals(emptyList<String>(), found(lines))
        // Each screen draws through the components: it imports from ds/.
        for (file in screens.filter { it.name != "QrScanner.kt" }) {
            assertTrue("${file.name} uses no component", file.readText().contains("import fr.courtillon.onimobile.ds."))
        }
        // Twin: the keycap screens' own lines are what the pattern refuses.
        val old = listOf(
            "Text(name, color = DS.Palette.textPrimary, fontSize = DS.Type.label)",
            "Keycap(KeyLabel.Text(title), action, greyStyle, modifier)",
            "Box(Modifier.background(DS.Palette.lcdGlass, RoundedCornerShape(DS.Radius.badge)))",
            "import androidx.compose.material3.AlertDialog",
        )
        assertEquals(old.size, found(old.map { "planted" to it }).size)
    }

    @Test
    fun theKeycapComponentsAreGone() {
        assertFalse(File(sources, "ds/keycap").exists())
        val components = File(sources, "ds").listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
        for (name in listOf("Button", "Slider", "Tabs", "Section", "Label", "KeyPlate", "Keypad", "Surface", "WarningBand", "StatusLine")) {
            assertTrue("ds/$name.kt", name in components)
        }
        // Twin: a component the desktop has and this build does not is not found.
        assertFalse("Toggle" in components)
    }
}
