package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.network.isWebAddress
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.recipe_import
import com.patrickzedler.grocy.core.resources.recipe_import_action
import com.patrickzedler.grocy.core.resources.recipe_import_url
import com.patrickzedler.grocy.core.resources.recipe_import_url_hint
import com.patrickzedler.grocy.core.resources.recipe_import_url_invalid
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the address of a recipe page; the keyboard opens at once, typing (or pasting) is all
 * the dialog is for, and its action imports. An address that is no web page says so.
 */
@Composable
internal fun RecipeImportDialog(
    onImport: (url: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val url = rememberTextFieldState()
    var showsError by rememberSaveable { mutableStateOf(false) }
    FormDialog(
        title = stringResource(Res.string.recipe_import),
        isSaving = false,
        error = null,
        saveText = stringResource(Res.string.recipe_import_action),
        onSave = {
            val text = url.text.toString().trim()
            if (isWebAddress(text)) onImport(text) else showsError = true
        },
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            TextItem(
                state = url,
                label = stringResource(Res.string.recipe_import_url),
                index = 0,
                count = 1,
                hint = stringResource(Res.string.recipe_import_url_hint),
                error = stringResource(Res.string.recipe_import_url_invalid).takeIf { showsError },
                keyboardType = KeyboardType.Uri,
                autoFocus = true,
                isLastField = true,
                autoCorrect = false,
            )
        }
    }
}
