/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.config;

import android.content.Context;

import com.android.launcher3.Utilities;
import com.android.launcher3.uioverrides.DeviceFlag;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines a set of flags used to control various launcher behaviors.
 *
 * <p>All the flags should be defined here with appropriate default values.
 */
public final class FeatureFlags {

    public static final String FLAGS_PREF_NAME = "featureFlags";
    /**
     * True when the build has come from Android Studio and is being used for local debugging.
     */
    public static final boolean IS_STUDIO_BUILD = false; //BuildConfig.DEBUG;
    /**
     * Enable moving the QSB on the 0th screen of the workspace. This is not a configuration feature
     * and should be modified at a project level.
     */
    public static final boolean QSB_ON_FIRST_SCREEN = true;

    // When enabled shows a work profile tab in all apps
    public static final boolean ALL_APPS_TABS_ENABLED = true;
    /**
     * Feature flag to handle define config changes dynamically instead of killing the process.
     * <p>
     * <p>
     * To add a new flag that can be toggled through the flags UI:
     * <p>
     * Declare a new ToggleableFlag below. Give it a unique key (e.g. "QSB_ON_FIRST_SCREEN"),
     * and set a default value for the flag. This will be the default value on Debug builds.
     */
    // When enabled the promise icon is visible in all apps while installation an app.
    public static final BooleanFlag PROMISE_APPS_IN_ALL_APPS = getDebugFlag(
            "PROMISE_APPS_IN_ALL_APPS", true, "Add promise icon in all-apps");
    // When enabled a promise icon is added to the home screen when install session is active.
    public static final BooleanFlag PROMISE_APPS_NEW_INSTALLS = getDebugFlag(
            "PROMISE_APPS_NEW_INSTALLS", true,
            "Adds a promise icon to the home screen for new install sessions.");
    public static final BooleanFlag APPLY_CONFIG_AT_RUNTIME = getDebugFlag(
            "APPLY_CONFIG_AT_RUNTIME", true, "Apply display changes dynamically");
    public static final BooleanFlag QUICKSTEP_SPRINGS = getDebugFlag(
            "QUICKSTEP_SPRINGS", true, "Enable springs for quickstep animations");
    public static final BooleanFlag UNSTABLE_SPRINGS = getDebugFlag(
            "UNSTABLE_SPRINGS", false, "Enable unstable springs for quickstep animations");
    public static final BooleanFlag KEYGUARD_ANIMATION = getDebugFlag(
            "KEYGUARD_ANIMATION", false, "Enable animation for keyguard going away on wallpaper");
    public static final BooleanFlag ADAPTIVE_ICON_WINDOW_ANIM = getDebugFlag(
            "ADAPTIVE_ICON_WINDOW_ANIM", true, "Use adaptive icons for window animations.");
    public static final BooleanFlag ENABLE_QUICKSTEP_LIVE_TILE = getDebugFlag(
            "ENABLE_QUICKSTEP_LIVE_TILE", false, "Enable live tile in Quickstep overview");
    // Keep as DeviceFlag to allow remote disable in emergency.
    /*public static final BooleanFlag ENABLE_SUGGESTED_ACTIONS_OVERVIEW = Utilities.ATLEAST_P ? new DeviceFlag(
            "ENABLE_SUGGESTED_ACTIONS_OVERVIEW", false, "Show chip hints on the overview screen")
            : new BooleanFlag("ENABLE_SUGGESTED_ACTIONS_OVERVIEW", false);*/
    public static final BooleanFlag ENABLE_SUGGESTED_ACTIONS_OVERVIEW =
            new BooleanFlag("ENABLE_SUGGESTED_ACTIONS_OVERVIEW", true);
    /*public static final BooleanFlag FOLDER_NAME_SUGGEST = Utilities.ATLEAST_P ? new DeviceFlag(
            "FOLDER_NAME_SUGGEST", true,
            "Suggests folder names instead of blank text.")
            : new BooleanFlag("FOLDER_NAME_SUGGEST", false);*/
    public static final BooleanFlag FOLDER_NAME_SUGGEST =
            new BooleanFlag("FOLDER_NAME_SUGGEST", true);

    public static final BooleanFlag FOLDER_NAME_MAJORITY_RANKING = getDebugFlag(
            "FOLDER_NAME_MAJORITY_RANKING", true,
            "Suggests folder names based on majority based ranking.");
    /*public static final BooleanFlag APP_SEARCH_IMPROVEMENTS = Utilities.ATLEAST_P ? new DeviceFlag(
            "APP_SEARCH_IMPROVEMENTS", true,
            "Adds localized title and keyword search and ranking")
            : new BooleanFlag("APP_SEARCH_IMPROVEMENTS", false);*/
    public static final BooleanFlag APP_SEARCH_IMPROVEMENTS =
            new BooleanFlag("APP_SEARCH_IMPROVEMENTS", true);
    public static final BooleanFlag ENABLE_PREDICTION_DISMISS = getDebugFlag(
            "ENABLE_PREDICTION_DISMISS", true, "Allow option to dimiss apps from predicted list");
    public static final BooleanFlag ENABLE_QUICK_CAPTURE_GESTURE = getDebugFlag(
            "ENABLE_QUICK_CAPTURE_GESTURE", true, "Swipe from right to left to quick capture");
    public static final BooleanFlag ENABLE_QUICK_CAPTURE_WINDOW = getDebugFlag(
            "ENABLE_QUICK_CAPTURE_WINDOW", false, "Use window to host quick capture");
    public static final BooleanFlag FORCE_LOCAL_OVERSCROLL_PLUGIN = getDebugFlag(
            "FORCE_LOCAL_OVERSCROLL_PLUGIN", false,
            "Use a launcher-provided OverscrollPlugin if available");
    public static final BooleanFlag ASSISTANT_GIVES_LAUNCHER_FOCUS = getDebugFlag(
            "ASSISTANT_GIVES_LAUNCHER_FOCUS", false,
            "Allow Launcher to handle nav bar gestures while Assistant is running over it");
    public static final BooleanFlag ENABLE_HYBRID_HOTSEAT = getDebugFlag(
            "ENABLE_HYBRID_HOTSEAT", true, "Fill gaps in hotseat with predicted apps");
    public static final BooleanFlag HOTSEAT_MIGRATE_TO_FOLDER = getDebugFlag(
            "HOTSEAT_MIGRATE_TO_FOLDER", false, "Should move hotseat items into a folder");
    public static final BooleanFlag ENABLE_DEEP_SHORTCUT_ICON_CACHE = getDebugFlag(
            "ENABLE_DEEP_SHORTCUT_ICON_CACHE", true, "R/W deep shortcut in IconCache");
    public static final BooleanFlag MULTI_DB_GRID_MIRATION_ALGO = getDebugFlag(
            "MULTI_DB_GRID_MIRATION_ALGO", true, "Use the multi-db grid migration algorithm");
    public static final BooleanFlag ENABLE_LAUNCHER_PREVIEW_IN_GRID_PICKER = getDebugFlag(
            "ENABLE_LAUNCHER_PREVIEW_IN_GRID_PICKER", true, "Show launcher preview in grid picker");
    public static final BooleanFlag ENABLE_OVERVIEW_ACTIONS = getDebugFlag(
            "ENABLE_OVERVIEW_ACTIONS", true, "Show app actions instead of the shelf in Overview."
                    + " As part of this decoupling, also distinguish swipe up from nav bar vs above it.");
    // Keep as DeviceFlag for remote disable in emergency.
    /*public static final BooleanFlag ENABLE_OVERVIEW_SELECTIONS = Utilities.ATLEAST_P ? new DeviceFlag(
            "ENABLE_OVERVIEW_SELECTIONS", true, "Show Select Mode button in Overview Actions")
            : new BooleanFlag("ENABLE_OVERVIEW_SELECTIONS", false);*/
    public static final BooleanFlag ENABLE_OVERVIEW_SELECTIONS =
            new BooleanFlag("ENABLE_OVERVIEW_SELECTIONS", true);

    public static final BooleanFlag ENABLE_OVERVIEW_SHARE = getDebugFlag(
            "ENABLE_OVERVIEW_SHARE", false, "Show Share button in Overview Actions");
    public static final BooleanFlag ENABLE_DATABASE_RESTORE = getDebugFlag(
            "ENABLE_DATABASE_RESTORE", true,
            "Enable database restore when new restore session is created");
    public static final BooleanFlag ENABLE_UNIVERSAL_SMARTSPACE = getDebugFlag(
            "ENABLE_UNIVERSAL_SMARTSPACE", false,
            "Replace Smartspace with a version rendered by System UI.");
    public static final BooleanFlag ENABLE_LSQ_VELOCITY_PROVIDER = getDebugFlag(
            "ENABLE_LSQ_VELOCITY_PROVIDER", true,
            "Use Least Square algorithm for motion pause detection.");
    public static final BooleanFlag ALWAYS_USE_HARDWARE_OPTIMIZATION_FOR_FOLDER_ANIMATIONS =
            getDebugFlag(
                    "ALWAYS_USE_HARDWARE_OPTIMIZATION_FOR_FOLDER_ANIMATIONS", false,
                    "Always use hardware optimization for folder animations.");
    public static final BooleanFlag ENABLE_ALL_APPS_EDU = getDebugFlag(
            "ENABLE_ALL_APPS_EDU", true,
            "Shows user a tutorial on how to get to All Apps after X amount of attempts.");
    public static final BooleanFlag SEPARATE_RECENTS_ACTIVITY = getDebugFlag(
            "SEPARATE_RECENTS_ACTIVITY", false,
            "Uses a separate recents activity instead of using the integrated recents+Launcher UI");
    /*public static final BooleanFlag USER_EVENT_DISPATCHER = Utilities.ATLEAST_P ? new DeviceFlag(
            "USER_EVENT_DISPATCHER", true, "User event dispatcher collects logs.")
            : new BooleanFlag("USER_EVENT_DISPATCHER", true);
    public static final BooleanFlag ENABLE_MINIMAL_DEVICE = Utilities.ATLEAST_P ? new DeviceFlag(
            "ENABLE_MINIMAL_DEVICE", true,
            "Allow user to toggle minimal device mode in launcher.")
            : new BooleanFlag("ENABLE_MINIMAL_DEVICE", false);*/
    public static final BooleanFlag USER_EVENT_DISPATCHER =
            new BooleanFlag("USER_EVENT_DISPATCHER", true);
    public static final BooleanFlag ENABLE_MINIMAL_DEVICE =
            new BooleanFlag("ENABLE_MINIMAL_DEVICE", true);
    private static final List<DebugFlag> sDebugFlags = new ArrayList<>();

    private FeatureFlags() {
    }

    public static boolean showFlagTogglerUi(Context context) {
        return Utilities.IS_DEBUG_DEVICE && Utilities.isDevelopersOptionsEnabled(context);
    }

    public static void initialize(Context context) {
        synchronized (sDebugFlags) {
            for (DebugFlag flag : sDebugFlags) {
                flag.initialize(context);
            }
            sDebugFlags.sort((f1, f2) -> f1.key.compareToIgnoreCase(f2.key));
        }
    }

    static List<DebugFlag> getDebugFlags() {
        synchronized (sDebugFlags) {
            return new ArrayList<>(sDebugFlags);
        }
    }

    public static void dump(PrintWriter pw) {
        pw.println("DeviceFlags:");
        synchronized (sDebugFlags) {
            for (DebugFlag flag : sDebugFlags) {
                if (flag instanceof DeviceFlag) {
                    pw.println("  " + flag.toString());
                }
            }
        }
        pw.println("DebugFlags:");
        synchronized (sDebugFlags) {
            for (DebugFlag flag : sDebugFlags) {
                if (!(flag instanceof DeviceFlag)) {
                    pw.println("  " + flag.toString());
                }
            }
        }
    }

    private static BooleanFlag getDebugFlag(String key, boolean defaultValue, String description) {
        return new BooleanFlag(key, defaultValue);
        //return Utilities.IS_DEBUG_DEVICE
        //? new DebugFlag(key, defaultValue, description)
        //: new BooleanFlag(key, defaultValue);
    }

    public static class BooleanFlag {

        public final String key;
        public boolean defaultValue;

        public BooleanFlag(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public boolean get() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return appendProps(new StringBuilder()).toString();
        }

        protected StringBuilder appendProps(StringBuilder src) {
            return src.append(key).append(", defaultValue=").append(defaultValue);
        }

        public void addChangeListener(Context context, Runnable r) {
        }
    }

    public static class DebugFlag extends BooleanFlag {

        public final String description;
        private boolean mCurrentValue;

        public DebugFlag(String key, boolean defaultValue, String description) {
            super(key, defaultValue);
            this.description = description;
            mCurrentValue = this.defaultValue;
            synchronized (sDebugFlags) {
                sDebugFlags.add(this);
            }
        }

        @Override
        public boolean get() {
            return mCurrentValue;
        }

        public void initialize(Context context) {
            mCurrentValue = context.getSharedPreferences(FLAGS_PREF_NAME, Context.MODE_PRIVATE)
                    .getBoolean(key, defaultValue);
        }

        @Override
        protected StringBuilder appendProps(StringBuilder src) {
            return super.appendProps(src).append(", mCurrentValue=").append(mCurrentValue);
        }
    }
}
