package com.patrickzedler.grocy.feature.settings.impl.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.data.util.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.core.data.util.withMinimumDuration
import kotlinx.coroutines.launch

internal class LogViewModel(
    private val logReader: LogReader,
) : ViewModel() {

    val source: LogSource = logReader.source

    /** The errors as text, null while they load. */
    var log by mutableStateOf<String?>(null)
        private set

    init {
        reload()
    }

    fun reload() {
        log = null
        viewModelScope.launch {
            log = withMinimumDuration(MINIMUM_LOADING_DURATION) { logReader.readErrors() }
        }
    }
}
