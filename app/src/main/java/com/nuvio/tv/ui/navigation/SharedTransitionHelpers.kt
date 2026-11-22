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
import com.nuvio.tv.ui.screens.home.HeroBackdropState
import com.nuvio.tv.ui.screens.home.normalizedImageUrl
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
 * Only marks the artwork when the image this composable is currently showing matches the URL
 * that will be handed to the detail screen as [heroBackdropUrl]. This avoids a crossfade/fight
 * when the card shows a portrait poster but the detail hero is a wide backdrop.
 */
fun Modifier.sharedDetailHeroBackdropSource(
    itemId: String,
    sourceImageUrl: String?
): Modifier = composed {
    if (!rememberSharedElementEnabled()) return@composed this
    val handoffBackdropUrl = HeroBackdropState.currentHeroBackdropUrl
    if (sourceImageUrl.isNullOrBlank() ||
        handoffBackdropUrl.isNullOrBlank() ||
        sourceImageUrl.normalizedImageUrl() != handoffBackdropUrl.normalizedImageUrl()
    ) {
        return@composed this
    }
    val sharedScope = LocalSharedTransitionScope.current!!
    val navScope = LocalNavAnimatedVisibilityScope.current!!
    with(sharedScope) {
        this@composed.sharedElement(
            rememberSharedContentState(key = "detail-hero-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                tween(
                    durationMillis = NuvioMotion.tokens.durations.fast,
                    easing = NuvioMotion.tokens.easings.standard
                )
            }
        )
    }
}

/**
 * Destination-side artwork shared element.
 *
 * The detail applies this only when the incoming [heroBackdropUrl] (the image the home side was
 * showing) is the same URL the detail will display as its hero backdrop.
 */
fun Modifier.sharedDetailHeroBackdropDestination(
    itemId: String,
    heroBackdropUrl: String?,
    detailBackdropUrl: String?
): Modifier = composed {
    if (!rememberSharedElementEnabled()) return@composed this
    if (heroBackdropUrl.isNullOrBlank() ||
        detailBackdropUrl.isNullOrBlank() ||
        heroBackdropUrl.normalizedImageUrl() != detailBackdropUrl.normalizedImageUrl()
    ) {
        return@composed this
    }
    val sharedScope = LocalSharedTransitionScope.current!!
    val navScope = LocalNavAnimatedVisibilityScope.current!!
    with(sharedScope) {
        this@composed.sharedElement(
            rememberSharedContentState(key = "detail-hero-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                tween(
                    durationMillis = NuvioMotion.tokens.durations.fast,
                    easing = NuvioMotion.tokens.easings.standard
                )
            }
        )
    }
}

/**
 * Logo shared element.
 *
 * Applied on both sides only when that side renders a logo image ([logoUrl] non-blank). If either
 * side falls back to styled text, no match occurs and the logo rides the route fade instead.
 */
fun Modifier.sharedDetailHeroLogo(
    itemId: String,
    logoUrl: String?
): Modifier = composed {
    if (!rememberSharedElementEnabled()) return@composed this
    val handoffLogoUrl = HeroBackdropState.currentHeroLogoUrl
    if (logoUrl.isNullOrBlank() ||
        handoffLogoUrl.isNullOrBlank() ||
        logoUrl.normalizedImageUrl() != handoffLogoUrl
    ) {
        return@composed this
    }
    val sharedScope = LocalSharedTransitionScope.current!!
    val navScope = LocalNavAnimatedVisibilityScope.current!!
    with(sharedScope) {
        this@composed.sharedElement(
            rememberSharedContentState(key = "detail-logo-$itemId"),
            animatedVisibilityScope = navScope,
            boundsTransform = { _, _ ->
                tween(
                    durationMillis = NuvioMotion.tokens.durations.fast,
                    easing = NuvioMotion.tokens.easings.standard
                )
            }
        )
    }
}
