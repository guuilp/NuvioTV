package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.components.SkeletonBar
import com.nuvio.tv.ui.components.rememberShimmerBrush
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Launch chain: placeholder for the Modern home shape (hero block + poster rows).
 *
 * It reuses the shared shimmer primitives so Home does not introduce a second
 * skeleton system, and it is only shown while there is no content at all.
 */
@Composable
internal fun HomeSkeleton(
    modifier: Modifier = Modifier,
    posterCardStyle: PosterCardStyle = PosterCardDefaults.Style,
    heroHeight: Dp = 200.dp,
    rowCount: Int = 3,
    posterCount: Int = 6
) {
    val shimmerBrush = rememberShimmerBrush()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = NuvioTheme.spacing.xl,
            end = NuvioTheme.spacing.xl,
            top = NuvioTheme.spacing.lg,
            bottom = NuvioTheme.spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xl)
    ) {
        item(key = "home_skeleton_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(NuvioTheme.radii.xl))
                    .background(shimmerBrush)
            )
        }

        items(rowCount, key = { index -> "home_skeleton_row_$index" }) { index ->
            Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)) {
                SkeletonBar(
                    width = 160.dp,
                    height = 20.dp,
                    brush = shimmerBrush,
                    cornerRadius = 10.dp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)) {
                    repeat(posterCount) {
                        Box(
                            modifier = Modifier
                                .width(posterCardStyle.width)
                                .height(posterCardStyle.height)
                                .clip(RoundedCornerShape(posterCardStyle.cornerRadius))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}
