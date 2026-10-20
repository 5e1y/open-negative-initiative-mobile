package fr.courtillon.onimobile.ds

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/** `DSTabBar` as the desktop's `Tabs`: a segmented control, one raised pill under the selection. */
@Composable
fun Tabs(names: List<String>, selected: Int, enabled: Boolean = true, modifier: Modifier = Modifier, picked: (Int) -> Unit) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(DS.Tabs.height)
            .clip(RoundedCornerShape(DS.Tabs.radius))
            .background(DS.Palette.canvas)
            .padding(DS.Tabs.inset),
    ) {
        val pitch = maxWidth / maxOf(1, names.size)
        val at by animateDpAsState(pitch * selected, tween(DS.Motion.quickMs.toInt()), label = "pill")
        Box(
            Modifier.offset(x = at).width(pitch).fillMaxHeight()
                .background(DS.Palette.raised, RoundedCornerShape(DS.Tabs.pillRadius)),
        )
        Row(Modifier.fillMaxSize()) {
            names.forEachIndexed { i, name ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { role = Role.Tab; this.selected = i == selected }
                        .clickable(enabled = enabled) { picked(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    val shown = if (!enabled) DS.State.disabled else if (i == selected) 1f else DS.State.idle
                    Text(name, color = DS.Palette.textPrimary, fontSize = DS.Type.label, modifier = Modifier.alpha(shown))
                }
            }
        }
    }
}
