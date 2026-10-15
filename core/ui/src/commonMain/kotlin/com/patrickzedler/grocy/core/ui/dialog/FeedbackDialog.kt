package com.patrickzedler.grocy.core.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.tryOpenUri
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_email
import com.patrickzedler.grocy.core.resources.action_email_description
import com.patrickzedler.grocy.core.resources.action_issue
import com.patrickzedler.grocy.core.resources.action_issue_description
import com.patrickzedler.grocy.core.resources.action_rate
import com.patrickzedler.grocy.core.resources.action_rate_description
import com.patrickzedler.grocy.core.resources.action_recommend
import com.patrickzedler.grocy.core.resources.action_recommend_description
import com.patrickzedler.grocy.core.resources.action_support
import com.patrickzedler.grocy.core.resources.action_support_description
import com.patrickzedler.grocy.core.resources.app_github
import com.patrickzedler.grocy.core.resources.app_mail
import com.patrickzedler.grocy.core.resources.app_vending_app
import com.patrickzedler.grocy.core.resources.ic_rounded_bug_report
import com.patrickzedler.grocy.core.resources.ic_rounded_group
import com.patrickzedler.grocy.core.resources.ic_rounded_mail
import com.patrickzedler.grocy.core.resources.ic_rounded_star
import com.patrickzedler.grocy.core.resources.ic_rounded_volunteer_activism
import com.patrickzedler.grocy.core.resources.msg_feedback
import com.patrickzedler.grocy.core.resources.title_feedback
import com.patrickzedler.grocy.core.ui.rememberRecommendAction
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val FEEDBACK_SUBJECT = "Grocy%20feedback"

@Composable
internal fun FeedbackDialog(
    unlockState: UnlockState?,
    onSupportClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val recommend = rememberRecommendAction(isPlayVariant = unlockState != null)

    val appVendingApp = stringResource(Res.string.app_vending_app)
    val appGithub = stringResource(Res.string.app_github)
    val appMail = stringResource(Res.string.app_mail)

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        FeedbackDialogContent(
            isRateVisible = unlockState?.isPlayStoreInstalled == true,
            isSupportVisible = unlockState != null &&
                unlockState.checkUnlockKey &&
                unlockState.isPlayStoreInstalled &&
                !unlockState.isKeyInstalled,
            onRateClick = {
                haptic.confirm()
                uriHandler.tryOpenUri(appVendingApp)
                onDismissRequest()
            },
            onSupportClick = {
                haptic.confirm()
                onSupportClick()
            },
            onRecommendClick = {
                haptic.confirm()
                recommend()
                onDismissRequest()
            },
            onIssueClick = {
                haptic.confirm()
                uriHandler.tryOpenUri("$appGithub/issues")
                onDismissRequest()
            },
            onEmailClick = {
                haptic.confirm()
                uriHandler.tryOpenUri("mailto:$appMail?subject=$FEEDBACK_SUBJECT")
                onDismissRequest()
            },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeedbackDialogContent(
    isRateVisible: Boolean,
    isSupportVisible: Boolean,
    modifier: Modifier = Modifier,
    onRateClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onRecommendClick: () -> Unit = {},
    onIssueClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.title_feedback)) },
        confirmButton = {
            TextButton(
                onClick = onCloseClick,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
    ) {
        val colors = SegmentedListDefaults.colors()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.msg_feedback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                val supportIndex = if (isRateVisible) 1 else 0
                val recommendIndex = supportIndex + if (isSupportVisible) 1 else 0
                val count = recommendIndex + 1

                if (isRateVisible) {
                    FeedbackItem(
                        title = stringResource(Res.string.action_rate),
                        description = stringResource(Res.string.action_rate_description),
                        icon = Res.drawable.ic_rounded_star,
                        index = 0,
                        count = count,
                        colors = colors,
                        onClick = onRateClick,
                    )
                }
                if (isSupportVisible) {
                    FeedbackItem(
                        title = stringResource(Res.string.action_support),
                        description = stringResource(Res.string.action_support_description),
                        icon = Res.drawable.ic_rounded_volunteer_activism,
                        index = supportIndex,
                        count = count,
                        colors = colors,
                        onClick = onSupportClick,
                    )
                }
                FeedbackItem(
                    title = stringResource(Res.string.action_recommend),
                    description = stringResource(Res.string.action_recommend_description),
                    icon = Res.drawable.ic_rounded_group,
                    index = recommendIndex,
                    count = count,
                    colors = colors,
                    onClick = onRecommendClick,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                FeedbackItem(
                    title = stringResource(Res.string.action_issue),
                    description = stringResource(Res.string.action_issue_description),
                    icon = Res.drawable.ic_rounded_bug_report,
                    index = 0,
                    count = 2,
                    colors = colors,
                    onClick = onIssueClick,
                )
                FeedbackItem(
                    title = stringResource(Res.string.action_email),
                    description = stringResource(Res.string.action_email_description),
                    icon = Res.drawable.ic_rounded_mail,
                    index = 1,
                    count = 2,
                    colors = colors,
                    onClick = onEmailClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeedbackItem(
    title: String,
    description: String,
    icon: DrawableResource,
    index: Int,
    count: Int,
    colors: ListItemColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = colors,
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            LeadingContentWrapper {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                )
            }
        },
        supportingContent = { Text(description) },
        content = { Text(title) },
    )
}

@Preview
@Composable
private fun FeedbackDialogPreview() {
    GrocyTheme {
        FeedbackDialogContent(
            isRateVisible = true,
            isSupportVisible = true,
        )
    }
}
