package com.nuvio.tv.ui.navigation

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
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

@Composable
private fun rememberSharedElementEnabled(): Boolean {
    val sharedScope = LocalSharedTransitionScope.current
    val navScope = LocalNavAnimatedVisibilityScope.current
    return sharedScope != null && navScope != null && !rememberReducedMotion()
}

/**
 * Source-side artwork shared element.
 *
 * Registers unconditionally (when the scope is available and reduced motion is off). The key is
 * per-item, so only the clicked card's artwork can pair with the destination. This avoids the
 * race-prone URL comparison against a click-time volatile and lets size variants morph as long as
 * the destination renders the handoff image under the transition.
 */
fun Modifier.sharedDetailHeroBackdropSource(itemId: String): Modifier = composed {
    if (!rememberSharedElementEnabled()) return@composed this
    val sharedScope = LocalSharedTransitionScope.current!!
    val navScope = LocalNavAnimatedVisibilityScope.current!!
    with(sharedScope) {
        this@composed.sharedElement(
            rememberSharedContentState(key = "detail-hero-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                tween(
                    durationMillis = NuvioMotion.tokens.durations.medium,
                    easing = NuvioMotion.tokens.easings.emphasized
                )
            }
        )
    }
}

/**
 * Destination-side artwork shared element.
 *
 * Registers only when a handoff backdrop URL was supplied by the source screen. The detail's seed
 * underlay renders that exact image, so source and destination are visually identical even when
 * the loaded meta's own backdrop URL uses a different TMDB size variant.
 */
fun Modifier.sharedDetailHeroBackdropDestination(
    itemId: String,
    heroBackdropUrl: String?
): Modifier = composed {
    if (!rememberSharedElementEnabled()) return@composed this
    if (heroBackdropUrl.isNullOrBlank()) return@composed this
    val sharedScope = LocalSharedTransitionScope.current!!
    val navScope = LocalNavAnimatedVisibilityScope.current!!
    with(sharedScope) {
        this@composed.sharedElement(
            rememberSharedContentState(key = "detail-hero-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                tween(
                    durationMillis = NuvioMotion.tokens.durations.medium,
                    easing = NuvioMotion.tokens.easings.emphasized
                )
            }
        )
    }
}

/**
 * Logo shared element.
 *
 * Registers when this composable actually renders a logo image. If either side falls back to styled
 * text, no match occurs and the logo rides the route fade instead.
 */
fun Modifier.sharedDetailHeroLogo(
    itemId: String,
    logoUrl: String?
): Modifier = composed {
    if (!rememberSharedElementEnabled()) return@composed this
    if (logoUrl.isNullOrBlank()) return@composed this
    val sharedScope = LocalSharedTransitionScope.current!!
    val navScope = LocalNavAnimatedVisibilityScope.current!!
    with(sharedScope) {
        this@composed.sharedElement(
            rememberSharedContentState(key = "detail-logo-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                tween(
                    durationMillis = NuvioMotion.tokens.durations.medium,
                    easing = NuvioMotion.tokens.easings.emphasized
                )
            }
        )
    }
}
