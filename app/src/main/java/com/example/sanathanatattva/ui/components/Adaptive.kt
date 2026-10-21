package com.example.sanathanatattva.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widest a screen's content grows (the Material "expanded" window breakpoint).
 * Tablets, unfolded foldables and landscape phones centre a column this wide
 * instead of stretching rows edge to edge.
 */
val ContentMaxWidth = 840.dp

/** Forms read best narrow: sign-in and sign-up fields stop at this width. */
val FormMaxWidth = 600.dp

/** Grid cells never get narrower than the smallest phone renders them. */
val ProductCellMinWidth = 300.dp

/**
 * Centres [content] in a column no wider than [maxWidth]; on a phone in portrait
 * this is a no-op. In landscape the column also stays clear of a side camera
 * cutout, which edge-to-edge windows otherwise draw underneath.
 */
@Composable
fun ReadableWidth(
    maxWidth: Dp = ContentMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxSize()) { content() }
    }
}
