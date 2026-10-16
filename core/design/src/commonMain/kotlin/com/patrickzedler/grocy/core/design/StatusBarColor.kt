package com.patrickzedler.grocy.core.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/**
 * Colors the status bar like the top app bar below it where the app cannot draw behind the
 * status bar, i.e. on the web; Android draws edge-to-edge, so the app bar reaches up there anyway.
 * [color] is read again whenever the state it reads changes, e.g. while the bar collapses.
 */
@Composable
internal expect fun StatusBarColorEffect(color: () -> Color)

/** One screen's wish for the status bar color. */
internal class StatusBarColorRequest {
    var color: Color = Color.Unspecified
}

/**
 * Status bar color requests of the screens on display: the newest one wins, the screen
 * background ([default]) applies without any. [apply] gets every change of the result.
 */
internal class StatusBarColorRequests(private val apply: (Color) -> Unit) {

    private val requests = mutableListOf<StatusBarColorRequest>()
    private var applied = Color.Unspecified

    var default: Color = Color.Unspecified
        set(value) {
            field = value
            update()
        }

    fun add(request: StatusBarColorRequest) {
        requests += request
        update()
    }

    fun remove(request: StatusBarColorRequest) {
        requests -= request
        update()
    }

    fun setColor(request: StatusBarColorRequest, color: Color) {
        request.color = color
        update()
    }

    private fun update() {
        val color = requests.lastOrNull { it.color.isSpecified }?.color ?: default
        if (color.isSpecified && color != applied) {
            applied = color
            apply(color)
        }
    }
}
