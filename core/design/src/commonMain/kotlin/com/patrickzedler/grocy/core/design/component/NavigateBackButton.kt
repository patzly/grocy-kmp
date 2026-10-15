package com.patrickzedler.grocy.core.design.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_back
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NavigateBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBarIconButton(
        painter = painterResource(Res.drawable.ic_rounded_arrow_back),
        contentDescription = stringResource(Res.string.action_back),
        onClick = onClick,
        modifier = modifier,
    )
}
