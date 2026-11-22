package com.nuvio.tv.ui.navigation

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.ui.theme.NuvioMotion

/**
 * The [SharedTransitionScope] that wraps the root NavHost. Deep composables can read this
 * and conditionally apply [Modifier.sharedElement]/[Modifier.sharedBounds].
 */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * The [AnimatedVisibilityScope] of the current root NavHost destination. Provided per
 * destination so shared-element modifiers know which nav transition they belong to.
 */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Returns true when the system animator duration scale is set to 0 (reduced motion).
 * Shared-element morphs should be skipped in that case and fall back to the route fade.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Marks a composable as the shared hero for a given content item.
 *
 * The card poster and the detail hero are different images (poster vs. backdrop) and often
 * different aspect ratios, so [sharedBounds] is used: the bounds morph while the content
 * crossfades inside. The same modifier is also applied to the hero carousel backdrop and the
 * detail hero so that all Home → Detail entry points share a single, collision-free key.
 */
fun Modifier.sharedDetailHero(itemId: String): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val navScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || navScope == null || rememberReducedMotion()) {
        return@composed this
    }
    with(sharedScope) {
        this@composed.sharedBounds(
            rememberSharedContentState(key = "detail-hero-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                androidx.compose.animation.core.tween(
                    durationMillis = NuvioMotion.tokens.durations.medium,
                    easing = NuvioMotion.tokens.easings.emphasized
                )
            }
        )
    }
}
