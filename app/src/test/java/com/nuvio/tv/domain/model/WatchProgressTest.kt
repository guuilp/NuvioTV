package com.nuvio.tv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchProgressTest {

    @Test
    fun `resolveResumePosition returns position when duration and position are both valid`() {
        val wp = watchProgress(
            position = 60000,
            duration = 180000
        )
        val result = wp.resolveResumePosition(actualDuration = 200000)
        assertEquals(60000, result)
    }

    @Test
    fun `resolveResumePosition clamps position to actualDuration`() {
        val wp = watchProgress(
            position = 300000,
            duration = 180000
        )
        val result = wp.resolveResumePosition(actualDuration = 200000)
        assertEquals(200000, result)
    }

    @Test
    fun `resolveResumePosition returns position when duration is zero`() {
        // Regression test for #2580: resume-after-pause bug
        // When saved duration is 0 but position is valid, position must be honored.
        val wp = watchProgress(
            position = 60000,
            duration = 0
        )
        val result = wp.resolveResumePosition(actualDuration = 200000)
        assertEquals(60000, result)
    }

    @Test
    fun `resolveResumePosition returns position when duration is zero and progressPercent is set`() {
        // Regression test for #2580: exact bug scenario
        // When exiting while paused, getEffectiveDuration returns 0,
        // saveWatchProgressInternal sets fallbackPercent = 5f.
        // Position must take priority over progressPercent.
        val wp = watchProgress(
            position = 60000,
            duration = 0,
            progressPercent = 5f
        )
        val result = wp.resolveResumePosition(actualDuration = 200000)
        // Should use position (60000), not 5% of 200000 (= 10000)
        assertEquals(60000, result)
    }

    @Test
    fun `resolveResumePosition uses progressPercent when position is zero`() {
        val wp = watchProgress(
            position = 0,
            duration = 0,
            progressPercent = 50f
        )
        val result = wp.resolveResumePosition(actualDuration = 200000)
        assertEquals(100000, result) // 50% of 200000
    }

    @Test
    fun `resolveResumePosition returns position when actualDuration is zero`() {
        val wp = watchProgress(
            position = 60000,
            duration = 0
        )
        val result = wp.resolveResumePosition(actualDuration = 0)
        assertEquals(60000, result)
    }

    @Test
    fun `resolveResumePosition returns zero when everything is zero`() {
        val wp = watchProgress(
            position = 0,
            duration = 0
        )
        val result = wp.resolveResumePosition(actualDuration = 0)
        assertEquals(0, result)
    }

    @Test
    fun `isInProgress ignores accidental peeks below the resume floor`() {
        val peek = watchProgress(
            position = 4_000,
            duration = 7_200_000
        )
        assertFalse(peek.isInProgress())
    }

    @Test
    fun `isInProgress keeps positions at or above the resume floor`() {
        val genuine = watchProgress(
            position = WatchProgress.MIN_IN_PROGRESS_MS,
            duration = 7_200_000
        )
        assertTrue(genuine.isInProgress())
    }

    @Test
    fun `isInProgress keeps percent-only sources from two percent`() {
        val atFloor = watchProgress(position = 0, duration = 0, progressPercent = 2f)
        val underFloor = watchProgress(position = 0, duration = 0, progressPercent = 1.9f)
        assertTrue(atFloor.isInProgress())
        assertFalse(underFloor.isInProgress())
    }

    @Test
    fun `resolveResumePosition starts fresh below the resume floor`() {
        val peek = watchProgress(
            position = 4_000,
            duration = 7_200_000
        )
        assertEquals(0, peek.resolveResumePosition(actualDuration = 7_200_000))
    }

    @Test
    fun `resolveResumePosition does not clamp legitimate large positions`() {
        val genuine = watchProgress(
            position = 12_345_678,
            duration = 7_200_000
        )
        assertEquals(7_200_000, genuine.resolveResumePosition(actualDuration = 7_200_000))
    }

    private fun watchProgress(
        position: Long,
        duration: Long,
        progressPercent: Float? = null
    ): WatchProgress {
        return WatchProgress(
            contentId = "tt1234567",
            contentType = "movie",
            name = "Test",
            poster = null,
            backdrop = null,
            logo = null,
            videoId = "tt1234567",
            season = null,
            episode = null,
            episodeTitle = null,
            position = position,
            duration = duration,
            lastWatched = 1000L,
            progressPercent = progressPercent
        )
    }
}
