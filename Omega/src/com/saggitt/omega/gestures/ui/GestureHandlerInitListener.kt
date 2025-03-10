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

package com.saggitt.omega.gestures.ui

import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.anim.PendingAnimation
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.states.StateAnimationConfig
import com.saggitt.omega.OmegaLauncher
import com.saggitt.omega.gestures.GestureHandler

class GestureHandlerInitListener(private val handler: GestureHandler) : StateManager.StateHandler<LauncherState> {

    fun init(launcher: Launcher, alreadyOnHome: Boolean): Boolean {
        handler.onGestureTrigger((launcher as OmegaLauncher).gestureController)
        return true
    }

    override fun setState(state: LauncherState?) {
        TODO("Not yet implemented")
    }

    override fun setStateWithAnimation(toState: LauncherState?, config: StateAnimationConfig?, animation: PendingAnimation?) {
        TODO("Not yet implemented")
    }

}
