package com.patrickzedler.grocy.core.forms

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.settings.ScannerFormsRepository
import com.patrickzedler.grocy.core.model.scanner.ScannerForm

/** The field a form with a scanner wants the focus in. */
enum class FormFocusTarget {
    /** The field that can be typed or scanned: the keyboard is the way to enter it. */
    CODE_FIELD,

    /** The field to fill next, once the code field has its value. */
    NEXT_FIELD,
}

/**
 * The scanner of a form's first field and where the focus goes around it: a form starts with what
 * the user entered with last, the scanner or the keyboard. A scan closes the scanner without
 * changing that, so it opens again for the next object; typing in the field, or the button, does.
 *
 * @param rememberedOpen the user used the scanner in this form last.
 * @param onRememberedChange stores what the user used last.
 * @param hasValue the form opens with its value (a preset product, an edited object), which
 *   needs neither the scanner nor the keyboard in the field.
 */
@Stable
class ScannerFieldState(
    rememberedOpen: Boolean = false,
    private val onRememberedChange: (Boolean) -> Unit = {},
    hasValue: Boolean = false,
) {
    private var rememberedOpen = rememberedOpen
    private var isFieldFocused = false

    var isOpen by mutableStateOf(rememberedOpen && !hasValue)
        private set

    /** Where the focus should go, until [onFocusHandled]. */
    var focusTarget by mutableStateOf(
        FormFocusTarget.CODE_FIELD.takeIf { !rememberedOpen && !hasValue },
    )
        private set

    fun onFocusHandled() {
        focusTarget = null
    }

    fun requestFocus(target: FormFocusTarget) {
        focusTarget = target
    }

    /** The field's button: closing it means typing instead, so the keyboard comes right away. */
    fun toggle() {
        isOpen = !isOpen
        remember(isOpen)
        focusTarget = FormFocusTarget.CODE_FIELD.takeIf { !isOpen }
    }

    /** A code was scanned and used; a page that keeps scanning leaves the scanner open. */
    fun onScanned(staysOpen: Boolean = false) {
        remember(true)
        isOpen = staysOpen
    }

    /** A scan that needs the user's answer first closes the scanner, without a next field. */
    fun closeWithoutFocus() {
        isOpen = false
    }

    /** Tapping into the field while scanning means typing instead. */
    fun onFieldFocusChange(isFocused: Boolean) {
        isFieldFocused = isFocused
        if (isFocused && isOpen) {
            isOpen = false
            remember(false)
        }
    }

    /**
     * The field got its value from the keyboard: the next field takes over. Chosen with the
     * arrow, without the field having the focus, the keyboard stays closed.
     */
    fun onValueChosen() {
        if (isFieldFocused) focusTarget = FormFocusTarget.NEXT_FIELD
    }

    /** The form is empty again for the next object: back to what the user entered with last. */
    fun onFormReset() {
        isOpen = rememberedOpen
        focusTarget = FormFocusTarget.CODE_FIELD.takeIf { !rememberedOpen }
    }

    private fun remember(open: Boolean) {
        if (rememberedOpen == open) return
        rememberedOpen = open
        onRememberedChange(open)
    }
}

/** The state of the scanner of [form], which opens as the user left it there last. */
fun ScannerFormsRepository.fieldState(form: ScannerForm, hasValue: Boolean) = ScannerFieldState(
    rememberedOpen = isOpen(form),
    onRememberedChange = { setOpen(form, it) },
    hasValue = hasValue,
)
