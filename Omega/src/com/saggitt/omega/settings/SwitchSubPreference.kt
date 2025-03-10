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

package com.saggitt.omega.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Switch
import androidx.preference.PreferenceViewHolder
import com.android.launcher3.R
import com.saggitt.omega.preferences.SubPreference
import com.saggitt.omega.util.isVisible

class SwitchSubPreference(context: Context, attrs: AttributeSet) : SubPreference(context, attrs) {

    private var switch: Switch? = null
    private var preventPersist = false
    var isChecked: Boolean = false
        set(value) {
            field = value
            switch?.isChecked = value
            divider?.isVisible = value
            if (!preventPersist) persistBoolean(value)
        }

    private var divider: View? = null

    init {
        layoutResource = R.layout.preference_two_target
        widgetLayoutResource = R.layout.preference_widget_master_switch
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        isChecked = getPersistedBoolean(defaultValue as? Boolean ?: false)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetView = holder.findViewById(android.R.id.widget_frame)
        widgetView?.setOnClickListener {
            preventPersist = true
            isChecked = !isChecked
            if (!callChangeListener(isChecked)) {
                isChecked = !isChecked
            } else {
                persistBoolean(isChecked)
            }
            preventPersist = false
        }

        val switch = holder.findViewById(R.id.switchWidget) as Switch
        switch.contentDescription = title
        switch.isChecked = isChecked
        this.switch = switch

        divider = holder.findViewById(R.id.two_target_divider)
        divider?.isVisible = isChecked

        // Add listener again as switch was probably still null when it first fired
        //ColorEngine.getInstance(context).addColorChangeListeners(this, ColorEngine.Resolvers.ACCENT)
    }

    override fun start(context: Context) {
        if (isChecked) {
            super.start(context)
        } else {
            isChecked = true
        }
    }
}

