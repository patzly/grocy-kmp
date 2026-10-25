package com.patrickzedler.grocy.core.design.component

import kotlin.math.roundToInt

/**
 * The step a discrete slider stands on. A drag reports the same step on every move, and only
 * the move onto another step may vibrate or change the value.
 */
internal class SliderStepTracker(value: Float, private val stepSize: Float) {

    private var step = stepOf(value)

    /** True if [value] lies on another step than before, which it then stands on. */
    fun moveTo(value: Float): Boolean {
        val newStep = stepOf(value)
        if (newStep == step) return false
        step = newStep
        return true
    }

    /** The value was set from elsewhere, without a move of the user. */
    fun reset(value: Float) {
        step = stepOf(value)
    }

    private fun stepOf(value: Float): Int = (value / stepSize).roundToInt()
}
