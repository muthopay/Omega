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

package com.saggitt.omega.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.android.launcher3.R

abstract class SingleDimensionGridSizePreference(context: Context, attrs: AttributeSet?, private val gridSize: GridSize) :
        DialogPreference(context, attrs) {
    val defaultSize by lazy { gridSize.numRowsOriginal }

    init {
        updateSummary()
    }

    fun getSize(): Int {
        return gridSize.fromPref(gridSize.numRows, defaultSize)
    }

    fun setSize(rows: Int) {
        gridSize.numRowsPref = if (rows > 0) rows else defaultSize
        updateSummary()
    }

    private fun updateSummary() {
        val value = getSize()
        summary = "$value"
    }

    override fun getDialogLayoutResource() = R.layout.preference_grid_size
}