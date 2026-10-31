package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType

/** Input of one userfield; [value] is what Grocy stores for it. */
@Stable
class UserfieldInput(val userfield: Userfield, private val initial: String?) {
    val text = TextFieldState(
        if (userfield.type == UserfieldType.LINK_WITH_TITLE) "" else initial.orEmpty(),
    )
    val title = TextFieldState(decodeLinkWithTitle(initial).first)
    val link = TextFieldState(decodeLinkWithTitle(initial).second)
    var checked by mutableStateOf(initial == "1")
    var selectedOption by mutableStateOf(initial?.takeIf { it.isNotBlank() })
    var checklist by mutableStateOf(decodeChecklist(initial))

    /** True while the text of a preset list names none of its options. */
    var isUnmatched by mutableStateOf(false)

    val value: String?
        get() = when (userfield.type) {
            UserfieldType.CHECKBOX -> if (checked) "1" else "0"
            UserfieldType.PRESET_LIST -> selectedOption
            UserfieldType.PRESET_CHECKLIST -> encodeChecklist(checklist, userfield.options)
            UserfieldType.LINK_WITH_TITLE ->
                encodeLinkWithTitle(title.text.toString(), link.text.toString())
            UserfieldType.NUMBER_INTEGRAL, UserfieldType.NUMBER_DECIMAL,
            UserfieldType.NUMBER_CURRENCY ->
                text.text.toString().takeIf { it.isNotBlank() }
                    ?.let { normalizeUserfieldNumber(userfield.type, it) }
            UserfieldType.FILE, UserfieldType.IMAGE -> initial
            else -> text.text.toString()
        }

    /** Only touched values are sent, like Grocy sends only changed userfields. */
    val isChanged: Boolean
        get() = userfield.type.isEditable && !sameUserfieldValue(value, initial) &&
            !(userfield.type == UserfieldType.CHECKBOX && initial == null && !checked)
}
