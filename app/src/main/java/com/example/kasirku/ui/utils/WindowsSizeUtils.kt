package com.example.kasirku.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun isLandscapeMode(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

@Composable
fun getScreenWidthDp(): Int {
    return LocalConfiguration.current.screenWidthDp
}