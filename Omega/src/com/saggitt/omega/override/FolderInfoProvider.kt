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

package com.saggitt.omega.override

import android.content.Context
import com.android.launcher3.R
import com.android.launcher3.model.ModelWriter
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.saggitt.omega.iconpack.IconPackManager
import com.saggitt.omega.util.SingletonHolder
import com.saggitt.omega.util.ensureOnMainThread
import com.saggitt.omega.util.omegaPrefs
import com.saggitt.omega.util.useApplicationContext

class FolderInfoProvider(context: Context) : CustomInfoProvider<FolderInfo>(context) {

    private val prefs = context.omegaPrefs

    override fun getTitle(info: FolderInfo): String {
        return info.title.toString()
    }

    override fun getDefaultTitle(info: FolderInfo): String {
        return context.getString(R.string.folder_hint_text)
    }

    override fun getCustomTitle(info: FolderInfo): String? {
        return info.title.toString()
    }

    override fun setTitle(info: FolderInfo, title: String?, modelWriter: ModelWriter) {
        info.setTitle(title ?: "", modelWriter)
    }

    override fun setIcon(info: FolderInfo, entry: IconPackManager.CustomIconEntry?) {
        prefs.customAppIcon[info.toComponentKey()] = entry
        info.onIconChanged()
    }

    override fun getIcon(info: FolderInfo): IconPackManager.CustomIconEntry? {
        return prefs.customAppIcon[info.toComponentKey()]
    }

    override fun supportsSwipeUp(info: FolderInfo) = info.container != ItemInfo.NO_ID

    override fun supportsIcon() = true

    override fun setSwipeUpAction(info: FolderInfo, action: String?) {
        info.setSwipeUpAction(context, action)
    }

    override fun getSwipeUpAction(info: FolderInfo): String? {
        return info.swipeUpAction
    }

    companion object : SingletonHolder<FolderInfoProvider, Context>(ensureOnMainThread(
            useApplicationContext(::FolderInfoProvider)))
}
