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

package com.saggitt.omega.dash.provider

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import com.android.launcher3.R
import com.saggitt.omega.dash.DashProvider
import com.saggitt.omega.settings.SettingsActivity

class EditDash(context: Context) : DashProvider(context) {
    override val name = context.getString(R.string.edit_dash)
    override val description = context.getString(R.string.edit_dash_summary)

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getIcon(): Drawable? {
        return context.getDrawable(R.drawable.ic_edit_no_shadow).apply {
            this?.setTint(darkenColor(accentColor))
        }
    }

    override fun runAction(context: Context?) {
        val fragment = "com.saggitt.omega.dash.DashFragment"
        SettingsActivity.startFragment(context, fragment, R.string.edit_dash)
    }
}