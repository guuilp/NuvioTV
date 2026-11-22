package com.nuvio.tv.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents the watch progress for a content item (movie or episode).
 */
@Immutable
data class WatchProgress(
    val contentId: String,           // IMDB ID of the movie/series
    val contentType: String,         // "movie" or "series"
    val name: String,                // Movie or series name
    val poster: String?,             // Poster URL
    val backdrop: String?,           // Backdrop URL
    val logo: String?,               // Logo URL
    val videoId: String,             // Specific video/episode ID being watched
    val season: Int?,                // Season number (null for movies)
    val episode: Int?,               // Episode number (null for movies)
    val episodeTitle: String?,       // Episode title (null for movies)
    val position: Long,              // Current playback position in ms
    val duration: Long,              // Total duration in ms
    val lastWatched: Long,           // Timestamp when last watched
    val addonBaseUrl: String? = null, // Addon that was used to play
    val progressPercent: Float? = null, // 0..100 from remote sources like Trakt playback
    val source: String = SOURCE_LOCAL,
    val traktPlaybackId: Long? = null,
    val traktMovieId: Int? = null,
    val traktShowId: Int? = null,
    val traktEpisodeId: Int? = null,
    val simklPlaybackId: Long? = null,
    override val trackingProviderId: String? = null,
    override val trackingProviderItemId: String? = null,
    override val trackingSourceUrl: String? = null,
    val completionThresholdOverride: Float? = null,
    val excludedNextUpSeasons: Set<Int> = emptySet()
) : TrackingAttributedItem {
    override val trackingContentId: String
        get() = contentId

    companion object {
        const val SOURCE_LOCAL = "local"
        const val SOURCE_TRAKT_PLAYBACK = "trakt_playback"
        const val SOURCE_TRAKT_HISTORY = "trakt_history"
        const val SOURCE_TRAKT_SHOW_PROGRESS = "trakt_show_progress"
        const val SOURCE_SIMKL_PLAYBACK = "simkl_playback"
        const val SOURCE_REMOTE_PLAYBACK = "remote_playback"
        const val SOURCE_REMOTE_HISTORY = "remote_history"
        const val COMPLETED_THRESHOLD = 0.90f
        const val SIMKL_COMPLETED_THRESHOLD = 0.80f

        // Resume polish: without a floor, a 4-second accidental tap pins every
        // later open of an item to 0:04 and creates a Continue Watching entry
        // forever. 30 s is long enough to ignore such peeks and short enough to
        // keep genuinely sampled 30-60 s positions visible.
        const val MIN_IN_PROGRESS_MS = 30_000L

        // Percent-only sources (SOURCE_SIMKL_PLAYBACK and other remote playback
        // projections) have no reliable millisecond position, so 2% keeps their
        // resume state from being lost to the millisecond floor.
        const val MIN_IN_PROGRESS_PERCENT = 0.02f
    }

    /**
     * Progress percentage (0.0 to 1.0)
     */
    val progressPercentage: Float
        get() {
            progressPercent?.let { explicitPercent ->
                return (explicitPercent / 100f).coerceIn(0f, 1f)
            }
            return if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
        }

    /**
     * Returns true if the content has been watched past the threshold (default 90%)
     */
    fun isCompleted(threshold: Float = completionThreshold()): Boolean = progressPercentage >= threshold

    /**
     * Returns true if meaningful progress was saved and the content is not completed.
     *
     * Resume polish: below [MIN_IN_PROGRESS_MS] (or [MIN_IN_PROGRESS_PERCENT] for
     * percent-only sources) the position is treated as an accidental peek and does
     * not create a Continue Watching entry. At or above the floor the previous
     * semantics are unchanged.
     */
    fun isInProgress(): Boolean =
        !isCompleted() &&
            (position >= MIN_IN_PROGRESS_MS || progressPercentage >= MIN_IN_PROGRESS_PERCENT)

    private fun completionThreshold(): Float =
        completionThresholdOverride?.takeIf { it.isFinite() && it > 0f && it <= 1f }
            ?: if (source == SOURCE_SIMKL_PLAYBACK) SIMKL_COMPLETED_THRESHOLD else COMPLETED_THRESHOLD

    /**
     * Returns the remaining time in milliseconds
     */
    val remainingTime: Long
        get() = (duration - position).coerceAtLeast(0)

    fun resolveResumePosition(actualDuration: Long): Long {
        val resolved = if (actualDuration <= 0) {
            position.coerceAtLeast(0L)
        } else if (position > 0) {
            // Position is the most precise resume indicator. Prefer it over
            // progressPercent even when saved duration is 0 (e.g. progress was
            // saved while paused and getEffectiveDuration returned 0).
            position.coerceIn(0L, actualDuration)
        } else {
            progressPercent?.let { explicitPercent ->
                val fraction = (explicitPercent / 100f).coerceIn(0f, 1f)
                (actualDuration * fraction).toLong()
            } ?: position.coerceAtLeast(0L)
        }
        // Resume polish: a target under the in-progress floor is an accidental
        // peek, so start fresh instead of pinning playback to a tiny position.
        // Legitimate larger positions are returned untouched.
        return if (resolved < MIN_IN_PROGRESS_MS) 0L else resolved
    }
}

/**
 * Represents the next item to watch for a series or a movie to resume.
 */
@Immutable
data class NextToWatch(
    val watchProgress: WatchProgress?,  // Null if nothing has been watched yet
    val isResume: Boolean,              // True if resuming current item, false if next episode
    val nextVideoId: String?,           // Video ID to play next
    val nextSeason: Int?,               // Next season number
    val nextEpisode: Int?,              // Next episode number
    val displayText: String             // Text to show on button (e.g., "Resume S1E2", "Play S1E3")
)
