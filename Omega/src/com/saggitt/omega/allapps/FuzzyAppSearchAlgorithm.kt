/*
 *     Copyright (C) 2019 Lawnchair Team.
 *
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

package com.saggitt.omega.allapps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import com.android.launcher3.AppFilter
import com.android.launcher3.LauncherAppState
import com.android.launcher3.allapps.search.AllAppsSearchBarController
import com.android.launcher3.allapps.search.SearchAlgorithm
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.pm.UserCache
import com.saggitt.omega.search.SearchProviderController
import com.saggitt.omega.search.WebSearchProvider
import com.saggitt.omega.util.omegaPrefs
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.ToStringFunction

class FuzzyAppSearchAlgorithm(private val context: Context, private val apps: List<AppInfo>) :
        SearchAlgorithm {

    private var resultHandler: Handler = Handler()
    private var baseFilter: AppFilter = OmegaAppFilter(context)

    override fun doSearch(query: String, callback: AllAppsSearchBarController.Callbacks) {
        val res = query(context, query, apps, baseFilter).map { it.toComponentKey() }
        val suggestions = getSuggestions(query)
        resultHandler.post {
            callback.onSearchResult(query, ArrayList(res), suggestions)
        }
    }

    override fun cancel(interruptActiveRequests: Boolean) {
        if (interruptActiveRequests) {
            resultHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun getSuggestions(query: String): List<String> {
        val provider = SearchProviderController.getInstance(context).searchProvider
        return (provider as? WebSearchProvider)?.getSuggestions(query) ?: emptyList()
    }

    companion object {
        const val MIN_SCORE = 65

        @JvmStatic
        fun getApps(context: Context, defaultApps: List<AppInfo>,
                    filter: AppFilter): List<AppInfo> {
            if (!context.omegaPrefs.searchHiddenApps) {
                return defaultApps
            }
            val iconCache = LauncherAppState.getInstance(context).iconCache
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            return UserCache.INSTANCE.get(context).userProfiles.flatMap { user ->
                val duplicatePreventionCache = mutableListOf<ComponentName>()
                launcherApps.getActivityList(null, user).filter { info ->
                    filter.shouldShowApp(info.componentName, user) &&
                            !duplicatePreventionCache.contains(info.componentName)
                }.map { info ->
                    duplicatePreventionCache.add(info.componentName)
                    AppInfo(context, info, user).apply {
                        iconCache.getTitleAndIcon(this, false)
                    }
                }
            }
        }

        @JvmStatic
        fun query(context: Context, query: String, defaultApps: List<AppInfo>,
                  filter: AppFilter): List<AppInfo> {
            return FuzzySearch.extractAll(query, getApps(context, defaultApps, filter),
                    ToStringFunction<AppInfo> { item ->
                        item?.title.toString()
                    }, WinklerWeightedRatio(), MIN_SCORE)
                    .sortedBy { it.referent.title.toString() }
                    .sortedByDescending { it.score }
                    .map { it.referent }
        }
    }
}