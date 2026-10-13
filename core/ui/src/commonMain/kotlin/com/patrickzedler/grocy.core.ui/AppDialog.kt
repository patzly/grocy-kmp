package com.patrickzedler.grocy.core.ui

import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.about_changelog
import com.patrickzedler.grocy.core.resources.license_google_sans_flex
import com.patrickzedler.grocy.core.resources.license_google_sans_flex_link
import com.patrickzedler.grocy.core.resources.license_material_icons
import com.patrickzedler.grocy.core.resources.license_material_icons_link
import org.jetbrains.compose.resources.StringResource

/** Dialogs every screen can open, shown by [AppDialogHost]. */
enum class AppDialog(
    internal val title: StringResource? = null,
    internal val textFile: String? = null,
    internal val link: StringResource? = null,
) {
    FEEDBACK,
    HELP,
    UNLOCK,
    CHANGELOG(
        title = Res.string.about_changelog,
        textFile = "files/changelog.txt",
    ),
    LICENSE_FONT(
        title = Res.string.license_google_sans_flex,
        textFile = "files/license_ofl.txt",
        link = Res.string.license_google_sans_flex_link,
    ),
    LICENSE_ICONS(
        title = Res.string.license_material_icons,
        textFile = "files/license_apache.txt",
        link = Res.string.license_material_icons_link,
    ),
}
