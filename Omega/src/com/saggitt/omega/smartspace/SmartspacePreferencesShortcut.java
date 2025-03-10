package com.saggitt.omega.smartspace;

import android.content.Intent;
import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.logging.StatsLogManager;
import com.android.launcher3.userevent.LauncherLogProto;
import com.android.launcher3.views.OptionsPopupView;
import com.saggitt.omega.settings.SettingsActivity;

public class SmartspacePreferencesShortcut extends OptionsPopupView.OptionItem {
    public SmartspacePreferencesShortcut(int labelRes, int iconRes, StatsLogManager.EventEnum eventId, View.OnLongClickListener clickListener) {
        super(R.string.customize, R.drawable.ic_smartspace_preferences, eventId,
                SmartspacePreferencesShortcut::startSmartspacePreferences);
    }

    private static boolean startSmartspacePreferences(View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        launcher.startActivitySafely(view, new Intent(launcher, SettingsActivity.class)
                .putExtra(SettingsActivity.SubSettingsFragment.TITLE, launcher.getString(R.string.home_widget))
                .putExtra(SettingsActivity.SubSettingsFragment.CONTENT_RES_ID, R.xml.omega_preferences_smartspace)
                .putExtra(SettingsActivity.SubSettingsFragment.HAS_PREVIEW, true), null, null);
        return true;
    }
}
