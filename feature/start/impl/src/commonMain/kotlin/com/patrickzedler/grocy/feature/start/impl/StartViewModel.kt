package com.patrickzedler.grocy.feature.start.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import kotlinx.coroutines.flow.StateFlow

/** Pull to refresh on the start screen downloads the server's data again. */
internal class StartViewModel(repository: MasterDataRepository) : ViewModel() {

    private val refreshController = RefreshController(viewModelScope) { repository.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()
}
