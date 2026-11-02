package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The forms whose user used the scanner last, read once at app start in [scope], so a form opens
 * with or without its scanner in its first frame.
 */
class ScannerFormsRepository(
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored forms are read. */
    val openForms: StateFlow<Set<ScannerForm>?> =
        settingsStore.scannerForms.stateIn(scope, SharingStarted.Eagerly, null)

    fun isOpen(form: ScannerForm): Boolean = openForms.value?.contains(form) == true

    /** Remembers it in the app's scope, a form that closes right away does not stop it. */
    fun setOpen(form: ScannerForm, open: Boolean) {
        scope.launch {
            val current = openForms.value ?: return@launch
            settingsStore.setScannerForms(if (open) current + form else current - form)
        }
    }
}
