package com.nuvio.tv.ui.screens.home

object HeroBackdropState {
    @Volatile
    var currentHeroBackdropUrl: String? = null
        private set

    /** Logo URL of the hero/card currently being handed off to the detail screen. */
    @Volatile
    var currentHeroLogoUrl: String? = null
        private set

    /** Last backdrop URL that was actually displayed — survives navigation for seamless back transitions. */
    @Volatile
    var lastDisplayedUrl: String? = null

    fun update(url: String?) {
        currentHeroBackdropUrl = url
        if (!url.isNullOrBlank()) {
            lastDisplayedUrl = url
        }
    }

    fun updateLogo(url: String?) {
        currentHeroLogoUrl = url?.normalizedImageUrl()
    }

    fun consumeAndClear(): String? {
        val url = currentHeroBackdropUrl
        currentHeroBackdropUrl = null
        return url
    }
}

/** Normalizes image URLs so the same asset with different query parameters matches. */
internal fun String.normalizedImageUrl(): String = trim().removeSuffix("/").substringBefore("?")
