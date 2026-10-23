package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_github
import com.patrickzedler.grocy.core.resources.app_vending_app
import com.patrickzedler.grocy.core.resources.msg_recommend
import org.jetbrains.compose.resources.stringResource

/**
 * Shares a recommendation. Links to Google Play in the Play app, which is the only variant with
 * an [com.patrickzedler.grocy.core.model.UnlockState], and to GitHub everywhere else. Without a
 * share sheet [onShareUnavailable] is called instead, to show [AppDialog.RECOMMEND].
 */
@Composable
fun rememberRecommendAction(
    isPlayVariant: Boolean,
    onShareUnavailable: () -> Unit,
): () -> Unit {
    val shareText = rememberTextSharer()
    val text = rememberRecommendText(isPlayVariant)
    val currentOnShareUnavailable by rememberUpdatedState(onShareUnavailable)
    return remember(shareText, text) {
        {
            if (isTextSharingAvailable()) shareText(text) else currentOnShareUnavailable()
        }
    }
}

@Composable
internal fun rememberRecommendText(isPlayVariant: Boolean): String {
    val link = stringResource(
        if (isPlayVariant) Res.string.app_vending_app else Res.string.app_github,
    )
    return stringResource(Res.string.msg_recommend, link)
}
