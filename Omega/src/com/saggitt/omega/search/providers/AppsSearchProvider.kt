/*
 *  This file is part of Omega Launcher
 *  Copyright (c) 2021   Saul Henriquez
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.search.providers

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.saggitt.omega.search.SearchProvider

@Keep
class AppsSearchProvider(context: Context) : SearchProvider(context) {

    override val name: String = context.getString(R.string.search_provider_appsearch)
    override val supportsVoiceSearch = false
    override val supportsAssistant = false
    override val supportsFeed = false
    override val packageName: String
        get() = "AppsSearchProvider"
    var prefs = Utilities.getOmegaPrefs(context)
    override fun startSearch(callback: (intent: Intent) -> Unit) {
        val launcher = LauncherAppState.getInstanceNoCreate().launcher
        launcher.stateManager.goToState(LauncherState.ALL_APPS, true) {
            launcher.appsView.searchUiManager.startSearch()
        }
    }

    override fun getIcon(): Drawable = context.getDrawable(R.drawable.ic_search)!!.mutate().apply {
        setTint(prefs.accentColor)
    }

}