package com.example.sanathanatattva.ui.brand

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Swallows every touch, so nothing beneath a playing brand moment can be tapped. */
internal fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
