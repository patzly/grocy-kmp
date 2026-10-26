package com.patrickzedler.grocy.feature.start.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Pull to refresh on the start screen downloads the server's data again. [serverConfig] (read
 * at app start, null until then) hides the sections of features the server turned off.
 */
internal class StartViewModel(
    repository: MasterDataRepository,
    val serverConfig: StateFlow<ServerConfig?>,
) : ViewModel() {

    private val refreshController = RefreshController(viewModelScope) { repository.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()
}
