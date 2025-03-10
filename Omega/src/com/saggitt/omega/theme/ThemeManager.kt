/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.uioverrides.WallpaperColorInfo
import com.saggitt.omega.BlankActivity
import com.saggitt.omega.omegaApp
import com.saggitt.omega.twilight.TwilightListener
import com.saggitt.omega.twilight.TwilightManager
import com.saggitt.omega.twilight.TwilightState
import com.saggitt.omega.util.*
import com.saggitt.omega.util.Config.REQUEST_PERMISSION_LOCATION_ACCESS

class ThemeManager(val context: Context) : WallpaperColorInfo.OnChangeListener, TwilightListener {

    private val app = context.omegaApp
    private val wallpaperColorInfo = WallpaperColorInfo.getInstance(context)!!
    private val listeners = HashSet<ThemeOverride>()
    private val prefs = context.omegaPrefs
    private var themeFlags = 0
    private var usingNightMode = context.resources.configuration.usingNightMode
        set(value) {
            if (field != value) {
                field = value
                updateTheme()
            }
        }

    val isDark get() = themeFlags and THEME_DARK != 0
    val supportsDarkText get() = themeFlags and THEME_DARK_TEXT != 0
    val displayName: String
        get() {
            val values = context.resources.getIntArray(R.array.themeValues)
            val strings = context.resources.getStringArray(R.array.themes)
            val index = values.indexOf(themeFlags)
            return strings.getOrNull(index) ?: context.resources.getString(R.string.theme_auto)
        }
    private val twilightManager by lazy { TwilightManager.getInstance(context) }
    private val handler = Handler()
    private var listenToTwilight = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    twilightManager.registerListener(this, handler)
                    onTwilightStateChanged(twilightManager.lastTwilightState)
                } else {
                    twilightManager.unregisterListener(this)
                }
            }
        }
    private var isDuringNight = false
        set(value) {
            field = value
            if (!prefs.launcherTheme.hasFlag(THEME_FOLLOW_DAYLIGHT)) return
            if (themeFlags.hasFlag(THEME_DARK) != value) {
                updateTheme()
            }
        }

    init {
        updateTheme()
        wallpaperColorInfo.addOnChangeListener(this)
    }

    fun addOverride(themeOverride: ThemeOverride) {
        synchronized(listeners) {
            removeDeadListeners()
            listeners.add(themeOverride)
        }
        themeOverride.applyTheme(themeFlags)
    }

    fun removeOverride(themeOverride: ThemeOverride) {
        synchronized(listeners) {
            listeners.remove(themeOverride)
        }
    }

    fun getCurrentFlags() = themeFlags

    private fun removeDeadListeners() {
        val it = listeners.iterator()
        while (it.hasNext()) {
            if (!it.next().isAlive) {
                it.remove()
            }
        }
    }

    override fun onExtractedColorsChanged(ignore: WallpaperColorInfo?) {
        updateTheme()
    }

    fun updateTheme() {
        val theme = updateTwilightState(prefs.launcherTheme)
        val isBlack = isBlack(theme)

        val isDark = when {
            theme.hasFlag(THEME_FOLLOW_NIGHT_MODE) -> usingNightMode
            theme.hasFlag(THEME_FOLLOW_WALLPAPER) -> wallpaperColorInfo.isDark
            theme.hasFlag(THEME_FOLLOW_DAYLIGHT) -> isDuringNight
            else -> theme.hasFlag(THEME_DARK)
        }

        val supportsDarkText = when {
            theme.hasFlag(THEME_DARK_TEXT) -> true
            theme.hasFlag(THEME_FOLLOW_WALLPAPER) -> wallpaperColorInfo.supportsDarkText()
            else -> wallpaperColorInfo.supportsDarkText()
        }

        val darkMainColor = wallpaperColorInfo.isMainColorDark

        var newFlags = 0
        if (supportsDarkText) newFlags = newFlags or THEME_DARK_TEXT
        if (isDark) newFlags = newFlags or THEME_DARK
        if (isBlack) newFlags = newFlags or THEME_USE_BLACK
        if (darkMainColor) newFlags = newFlags or THEME_DARK_MAIN_COLOR
        if (newFlags == themeFlags) return
        themeFlags = newFlags
        reloadActivities()
        synchronized(listeners) {
            removeDeadListeners()
            listeners.forEach { it.onThemeChanged(themeFlags) }
        }
    }

    private fun updateTwilightState(theme: Int): Int {
        if (!theme.hasFlag(THEME_FOLLOW_DAYLIGHT)) {
            listenToTwilight = false
            return theme
        }
        if (twilightManager.isAvailable) {
            listenToTwilight = true
            return theme
        }

        BlankActivity.requestPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                REQUEST_PERMISSION_LOCATION_ACCESS) { granted ->
            if (granted) {
                listenToTwilight = true
            } else {
                prefs.launcherTheme = theme.removeFlag(THEME_DARK_MASK)
            }
        }

        return theme.removeFlag(THEME_DARK_MASK)
    }

    override fun onTwilightStateChanged(state: TwilightState?) {
        isDuringNight = state?.isNight == true
    }

    private fun reloadActivities() {
        HashSet(app.activityHandler.activities).forEach {
            if (it is ThemeableActivity) {
                it.onThemeChanged()
            } else {
                it.recreate()
            }
        }
    }

    fun updateNightMode(newConfig: Configuration) {
        usingNightMode = newConfig.usingNightMode
    }

    interface ThemeableActivity {
        fun onThemeChanged()
    }

    companion object : SingletonHolder<ThemeManager, Context>(ensureOnMainThread(useApplicationContext(::ThemeManager))) {

        const val THEME_FOLLOW_WALLPAPER = 1         // 000001 = 1
        const val THEME_DARK_TEXT = 1 shl 1          // 000010 = 2
        const val THEME_DARK = 1 shl 2               // 000100 = 4
        const val THEME_USE_BLACK = 1 shl 3          // 001000 = 8
        const val THEME_FOLLOW_NIGHT_MODE = 1 shl 4  // 010000 = 16
        const val THEME_FOLLOW_DAYLIGHT = 1 shl 5    // 100000 = 32
        const val THEME_DARK_MAIN_COLOR = 1 shl 6    // 1000000 = 64

        const val THEME_AUTO_MASK = THEME_FOLLOW_WALLPAPER or THEME_FOLLOW_NIGHT_MODE or THEME_FOLLOW_DAYLIGHT
        const val THEME_DARK_MASK = THEME_DARK or THEME_AUTO_MASK

        fun isDarkText(flags: Int) = (flags and THEME_DARK_TEXT) != 0
        fun isDark(flags: Int) = (flags and THEME_DARK) != 0
        fun isBlack(flags: Int) = (flags and THEME_USE_BLACK) != 0
        fun isDarkMainColor(flags: Int) = (flags and THEME_DARK_MAIN_COLOR) != 0

        fun getDefaultTheme(): Int {
            return if (Utilities.ATLEAST_Q) {
                THEME_FOLLOW_NIGHT_MODE
            } else {
                THEME_FOLLOW_WALLPAPER
            }
        }
    }
}