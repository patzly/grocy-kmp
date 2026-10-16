package com.patrickzedler.grocy.core.data.update

import com.patrickzedler.grocy.core.model.UpdateCheck
import com.patrickzedler.grocy.core.network.GitHubRelease
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.network.GitHubReleasesException

/** Marks nightly builds in their version name, followed by "+<short commit>" from CI. */
private const val NIGHTLY_MARKER = "-nightly"

/**
 * Updates from GitHub releases, for the FOSS app and every build outside Google Play. Regular
 * builds compare their version with the latest release, nightly builds their commit with the
 * nightly release. Installing opens the release page, where the APK can be downloaded.
 */
class GitHubAppUpdater(
    private val releases: GitHubReleases,
    private val versionName: String,
    private val openUrl: (String) -> Unit,
) : AppUpdater {

    private var foundRelease: GitHubRelease? = null

    override suspend fun check(): UpdateCheck {
        val release = try {
            if (isNightly) releases.nightlyRelease() else releases.latestRelease()
        } catch (_: GitHubReleasesException) {
            return UpdateCheck.Failed
        }
        val update = release?.let { if (isNightly) nightlyUpdate(it) else releaseUpdate(it) }
        foundRelease = release.takeIf { update != null }
        return update ?: UpdateCheck.UpToDate
    }

    override suspend fun install() {
        foundRelease?.let { openUrl(it.pageUrl) }
    }

    private val isNightly: Boolean
        get() = NIGHTLY_MARKER in versionName

    private fun releaseUpdate(release: GitHubRelease): UpdateCheck.Available? {
        val releaseVersion = release.tagName.removePrefix("v")
        return if (isNewerVersion(releaseVersion, than = versionName)) {
            UpdateCheck.Available(releaseVersion)
        } else {
            null
        }
    }

    private fun nightlyUpdate(release: GitHubRelease): UpdateCheck.Available? {
        // Local builds carry no commit, the published nightly is newer than them
        val commit = versionName.substringAfter("+", missingDelimiterValue = "")
        val isSameBuild = commit.isNotEmpty() && release.commit.startsWith(commit)
        return if (isSameBuild) null else UpdateCheck.Available(versionName = null)
    }
}

/**
 * True if version [candidate] is newer than [than], comparing the numbers of "major.minor.patch"
 * one by one. Suffixes such as "-nightly+abc1234" are ignored, missing numbers count as 0.
 */
internal fun isNewerVersion(candidate: String, than: String): Boolean {
    val new = candidate.versionNumbers() ?: return false
    val current = than.versionNumbers() ?: return true
    for (i in 0 until maxOf(new.size, current.size)) {
        val a = new.getOrElse(i) { 0 }
        val b = current.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

private fun String.versionNumbers(): List<Int>? =
    substringBefore('-')
        .substringBefore('+')
        .split('.')
        .map { it.toIntOrNull() ?: return null }
