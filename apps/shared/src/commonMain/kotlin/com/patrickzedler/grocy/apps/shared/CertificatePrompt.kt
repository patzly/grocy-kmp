package com.patrickzedler.grocy.apps.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.settings.CertificateQuestion
import com.patrickzedler.grocy.core.data.settings.CertificateTrustGate
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.network.ServerCertificate
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.certificates_fingerprint
import com.patrickzedler.grocy.core.resources.certificates_trust_action
import com.patrickzedler.grocy.core.resources.certificates_trust_issuer
import com.patrickzedler.grocy.core.resources.certificates_trust_message
import com.patrickzedler.grocy.core.resources.certificates_trust_subject
import com.patrickzedler.grocy.core.resources.certificates_trust_title
import com.patrickzedler.grocy.core.resources.certificates_valid_until
import org.jetbrains.compose.resources.stringResource

/** Asks during a login whether to trust a server's certificate nothing vouches for. */
@Composable
internal fun CertificatePrompt(gate: CertificateTrustGate) {
    val question by gate.question.collectAsStateWithLifecycle()
    val current = question ?: return
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = { gate.onAnswered(false) }) {
        CertificatePromptContent(
            question = current,
            onTrustClick = {
                haptic.confirm()
                gate.onAnswered(true)
            },
            onCancelClick = {
                haptic.confirm()
                gate.onAnswered(false)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CertificatePromptContent(
    question: CertificateQuestion,
    onTrustClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val certificate = question.certificate
    ScrollableAlertDialogContent(
        title = { Text(stringResource(Res.string.certificates_trust_title)) },
        confirmButton = {
            TextButton(
                onClick = onTrustClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.certificates_trust_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.certificates_trust_message, question.host))
            Text(
                text = stringResource(Res.string.certificates_trust_subject, certificate.subject),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.certificates_trust_issuer, certificate.issuer),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    Res.string.certificates_valid_until,
                    formatDate(certificate.notAfterEpochDay),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    Res.string.certificates_fingerprint,
                    certificate.fingerprint,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun CertificatePromptPreview() {
    GrocyTheme {
        CertificatePromptContent(
            question = CertificateQuestion(
                host = "grocy.local",
                certificate = ServerCertificate("ab12cd34", "CN=grocy", "CN=grocy", 21_000),
            ),
            onTrustClick = {},
            onCancelClick = {},
        )
    }
}
