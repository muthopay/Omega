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

package com.saggitt.omega.settings.controllers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.saggitt.omega.preferences.PreferenceController;
import com.saggitt.omega.util.Config;

import org.jetbrains.annotations.NotNull;

public class HomeWidgetController extends PreferenceController {
    private Context mContext;

    public HomeWidgetController(@NotNull Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isVisible() {
        PackageManager pm = mContext.getPackageManager();
        return isPackageInstalled(Config.GOOGLE_QSB, pm);
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
