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

package com.saggitt.omega.theme.ui

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.annotation.Keep
import com.android.launcher3.R
import com.saggitt.omega.OmegaPreferences
import com.saggitt.omega.preferences.CustomDialogPreference
import com.saggitt.omega.theme.ThemeManager
import com.saggitt.omega.util.hasFlag
import com.saggitt.omega.util.hasFlags
import com.saggitt.omega.util.omegaPrefs

@Keep
class ThemePreference(context: Context, attrs: AttributeSet?) : CustomDialogPreference(context, attrs),
        OmegaPreferences.OnPreferenceChangeListener {

    private val prefs = context.omegaPrefs

    override fun onAttached() {
        super.onAttached()

        prefs.addOnPreferenceChangeListener("pref_launcherTheme", this)
    }

    override fun onDetached() {
        super.onDetached()

        prefs.removeOnPreferenceChangeListener("pref_launcherTheme", this)
    }

    override fun onValueChanged(key: String, prefs: OmegaPreferences, force: Boolean) {
        reloadSummary()
    }

    private fun reloadSummary() {
        val theme = prefs.launcherTheme

        val forceDark = theme.hasFlag(ThemeManager.THEME_DARK)
        val forceDarkText = theme.hasFlag(ThemeManager.THEME_DARK_TEXT)
        val followWallpaper = theme.hasFlag(ThemeManager.THEME_FOLLOW_WALLPAPER)
        val followNightMode = theme.hasFlag(ThemeManager.THEME_FOLLOW_NIGHT_MODE)
        val followDaylight = theme.hasFlag(ThemeManager.THEME_FOLLOW_DAYLIGHT)

        val light = !theme.hasFlag(ThemeManager.THEME_DARK_MASK)
        val useBlack = theme.hasFlags(ThemeManager.THEME_USE_BLACK, ThemeManager.THEME_DARK_MASK)

        val themeDesc = ArrayList<Int>()
        when {
            forceDark && useBlack -> themeDesc.add(R.string.theme_black)
            forceDark -> themeDesc.add(R.string.theme_dark)
            followNightMode -> themeDesc.add(R.string.theme_dark_theme_mode_follow_system)
            followDaylight -> themeDesc.add(R.string.theme_dark_theme_mode_follow_daylight)
            followWallpaper -> themeDesc.add(R.string.theme_dark_theme_mode_follow_wallpaper)
            light -> themeDesc.add(R.string.theme_light)
        }
        if (useBlack && !forceDark) {
            themeDesc.add(R.string.theme_black)
        }
        if (forceDarkText) {
            themeDesc.add(R.string.theme_with_dark_text)
        }

        val res = context.resources
        val strings = ArrayList<String>()
        themeDesc.mapTo(strings) { res.getString(it) }
        for (i in (1 until strings.size)) {
            strings[i] = strings[i].toLowerCase()
        }
        summary = TextUtils.join(", ", strings)
    }
}
