package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_github
import com.patrickzedler.grocy.core.resources.app_vending_app
import com.patrickzedler.grocy.core.resources.msg_recommend
import org.jetbrains.compose.resources.stringResource

/**
 * Shares a recommendation. Links to Google Play in the Play app, which is the only variant with
 * an [com.patrickzedler.grocy.core.model.UnlockState], and to GitHub everywhere else.
 */
@Composable
fun rememberRecommendAction(isPlayVariant: Boolean): () -> Unit {
    val shareText = rememberTextSharer()
    val link = stringResource(
        if (isPlayVariant) Res.string.app_vending_app else Res.string.app_github,
    )
    val text = stringResource(Res.string.msg_recommend, link)
    return remember(shareText, text) { { shareText(text) } }
}
