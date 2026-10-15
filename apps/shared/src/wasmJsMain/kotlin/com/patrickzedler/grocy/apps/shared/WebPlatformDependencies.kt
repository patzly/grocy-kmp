package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.credentials.WebCredentialStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.settings.WebSettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory

class WebPlatformDependencies(
    override val versionName: String,
) : PlatformDependencies {

    override val credentialStore: CredentialStore by lazy { WebCredentialStore() }

    override val settingsStore: SettingsStore by lazy { WebSettingsStore() }

    override val unlockChecker: UnlockChecker? = null

    override val barcodeDecoders: Map<ScannerEngine, BarcodeDecoderFactory> =
        mapOf(ScannerEngine.ZXING to BarcodeDecoderFactory.Zxing)
}
