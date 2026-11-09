package com.patrickzedler.grocy.apps.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import com.patrickzedler.grocy.core.data.settings.CertificateTrustGate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.certificates_fingerprint
import com.patrickzedler.grocy.core.resources.certificates_trust_message
import com.patrickzedler.grocy.core.resources.certificates_trust_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks about a server's certificate nothing vouches for, only for logins entered on the watch:
 * those the phone hands over bring what the user trusted there.
 */
@Composable
internal fun WearCertificatePrompt(gate: CertificateTrustGate) {
    val question by gate.question.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val current = question
    AlertDialog(
        visible = current != null,
        onDismissRequest = { gate.onAnswered(false) },
        title = { Text(stringResource(Res.string.certificates_trust_title)) },
        text = {
            if (current != null) {
                Text(
                    stringResource(Res.string.certificates_trust_message, current.host) + "\n\n" +
                        stringResource(
                            Res.string.certificates_fingerprint,
                            current.certificate.fingerprint,
                        ),
                )
            }
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    gate.onAnswered(true)
                },
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    gate.onAnswered(false)
                },
            )
        },
    )
}
