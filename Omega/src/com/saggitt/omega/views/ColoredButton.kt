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

package com.saggitt.omega.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.widget.Button
import com.android.launcher3.Utilities
import com.saggitt.omega.util.getTabRipple

class ColoredButton(context: Context, attrs: AttributeSet) : Button(context, attrs) {

    var color: Int = 0

    private val defaultColor = currentTextColor

    fun reset() {
        color = Utilities.getOmegaPrefs(context).accentColor
        setTextColor()
        setRippleColor()
    }

    fun refreshTextColor() {
        setTextColor()
    }

    private fun setTextColor() {
        val stateList = ColorStateList(arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()),
                intArrayOf(
                        color,
                        defaultColor))
        setTextColor(stateList)
    }

    private fun setRippleColor() {
        background = RippleDrawable(getTabRipple(context, color), null, null)
    }
}
