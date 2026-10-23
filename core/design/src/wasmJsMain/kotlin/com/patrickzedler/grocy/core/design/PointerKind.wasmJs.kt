package com.patrickzedler.grocy.core.design

import androidx.compose.runtime.Composable

@Composable
internal actual fun prefersMouse(): Boolean = hasFinePrimaryPointer()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.matchMedia('(pointer: fine)').matches")
private external fun hasFinePrimaryPointer(): Boolean
