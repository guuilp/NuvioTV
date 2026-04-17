package com.nuvio.tv.ui.screens.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Builds a [MediaItem] enriched with [MediaMetadata] for the current playback context.
 *
 * The metadata is consumed by ExoPlayer's [androidx.media3.session.MediaSession] and propagated
 * to the Android media notification system — this is what makes the phone notification show the
 * title, series name and artwork instead of an empty/anonymous entry.
 *
 * Field mapping:
 *  - title     → episode title (series) or movie title
 *  - artist    → series / show name (contentName), empty for movies
 *  - albumTitle → series / show name (same as artist for series)
 *  - artworkUri → backdrop URL  (falls back to poster)
 *  - subtitle  → "Season X · Episode Y" for series, year for movies
 */
internal fun PlayerRuntimeController.buildMediaItemWithMetadata(
    url: String,
    subtitleConfigurations: List<MediaItem.SubtitleConfiguration> = emptyList(),
    mimeTypeOverride: String? = null
): MediaItem {
    val state = _uiState.value

    // Resolve the best available artwork URL
    val artworkUri: Uri? = listOfNotNull(backdrop, poster, logo)
        .firstOrNull { it.isNotBlank() }
        ?.let { Uri.parse(it) }

    // For series: title is the episode title, artist is "Show Name · S1 E3"
    // For movies: title is the movie title, artist is null (year shown via subtitle)
    //
    // Google Home uses the `artist` field as its second display line; the separate `subtitle`
    // field is only shown when `artist` is null (which is why movies correctly showed the year).
    // We therefore embed the S/E tag directly into `artist` so it always appears.
    val isSeriesContent = contentType?.lowercase() in listOf("series", "tv")
    val displayTitle: String = when {
        isSeriesContent && !state.currentEpisodeTitle.isNullOrBlank() -> state.currentEpisodeTitle
        else -> title
    }
    val seasonEpisodeTag: String? = if (
        isSeriesContent && state.currentSeason != null && state.currentEpisode != null
    ) {
        "S${state.currentSeason.toString().padStart(2, '0')} E${state.currentEpisode.toString().padStart(2, '0')}"
    } else null

    val artistName: String? = when {
        isSeriesContent -> {
            val showName = contentName?.takeIf { it.isNotBlank() } ?: title
            if (seasonEpisodeTag != null) "$showName · $seasonEpisodeTag" else showName
        }
        else -> null
    }
    // For series the S/E is now in `artist`; for movies keep year as subtitle fallback
    val subtitleText: String? = when {
        !isSeriesContent && !year.isNullOrBlank() -> year
        else -> null
    }

    val mediaMetadata = MediaMetadata.Builder()
        .setTitle(displayTitle.takeIf { it.isNotBlank() })
        .setArtist(artistName)
        .setAlbumTitle(artistName)  // shown as secondary line in some notification styles
        .setDisplayTitle(displayTitle.takeIf { it.isNotBlank() })
        .setSubtitle(subtitleText)
        .setArtworkUri(artworkUri)
        // Mark this as a video so the notification uses the right template
        .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
        .build()

    val mediaItemBuilder = MediaItem.Builder()
        .setUri(url)
        .setMediaMetadata(mediaMetadata)

    mimeTypeOverride?.let { mediaItemBuilder.setMimeType(it) }

    if (subtitleConfigurations.isNotEmpty()) {
        mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
    }

    return mediaItemBuilder.build()
}
