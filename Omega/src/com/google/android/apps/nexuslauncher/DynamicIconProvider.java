package com.google.android.apps.nexuslauncher;

import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.RequiresApi;

import com.android.launcher3.AdaptiveIconCompat;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.icons.IconProvider;
import com.android.launcher3.pm.UserCache;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.google.android.apps.nexuslauncher.clock.DynamicClock;

import java.util.Calendar;
import java.util.List;

public class DynamicIconProvider extends IconProvider {
    public static final String GOOGLE_CALENDAR = "com.google.android.calendar";
    private final BroadcastReceiver mDateChangeReceiver;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private int mDateOfMonth;

    public DynamicIconProvider(Context context) {
        super(context);
        mContext = context;
        mDateChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!Utilities.ATLEAST_NOUGAT) {
                    int dateOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    if (dateOfMonth == mDateOfMonth) {
                        return;
                    }
                    mDateOfMonth = dateOfMonth;
                }
                for (UserHandle user : UserCache.INSTANCE.get(context).getUserProfiles()) {
                    LauncherModel model = LauncherAppState.getInstance(context).getModel();
                    model.onPackageChanged(GOOGLE_CALENDAR, user);
                    List<ShortcutInfo> shortcuts = DeepShortcutManager.getInstance(context).queryForPinnedShortcuts(GOOGLE_CALENDAR, user);
                    if (!shortcuts.isEmpty()) {
                        model.updatePinnedShortcuts(GOOGLE_CALENDAR, shortcuts, user);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (!Utilities.ATLEAST_NOUGAT) {
            intentFilter.addAction(Intent.ACTION_TIME_TICK);
        }
        mContext.registerReceiver(mDateChangeReceiver, intentFilter, null, new Handler(MODEL_EXECUTOR.getLooper()));
        mPackageManager = mContext.getPackageManager();
    }

    private int getDayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
    }

    private int getDayResId(Bundle bundle, Resources resources) {
        if (bundle != null) {
            int dateArrayId = bundle.getInt(GOOGLE_CALENDAR + ".dynamic_icons_nexus_round", 0);
            if (dateArrayId != 0) {
                try {
                    TypedArray dateIds = resources.obtainTypedArray(dateArrayId);
                    int dateId = dateIds.getResourceId(getDayOfMonth(), 0);
                    dateIds.recycle();
                    return dateId;
                } catch (Resources.NotFoundException ignored) {
                }
            }
        }
        return 0;
    }

    private boolean isCalendar(String s) {
        return GOOGLE_CALENDAR.equals(s);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Drawable getIcon(LauncherActivityInfo launcherActivityInfo, int iconDpi, boolean flattenDrawable) {
        Drawable drawable = null;
        String packageName = launcherActivityInfo.getApplicationInfo().packageName;
        if (isCalendar(packageName)) {
            try {
                Bundle metaData = mPackageManager.getActivityInfo(launcherActivityInfo.getComponentName(), PackageManager.GET_META_DATA | PackageManager.GET_UNINSTALLED_PACKAGES).metaData;
                Resources resourcesForApplication = mPackageManager.getResourcesForApplication(packageName);
                int dayResId = getDayResId(metaData, resourcesForApplication);
                if (dayResId != 0) {
                    drawable = AdaptiveIconCompat.wrapNullable(resourcesForApplication.getDrawableForDensity(dayResId, iconDpi));
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } else if (!flattenDrawable &&
                Utilities.ATLEAST_OREO &&
                DynamicClock.DESK_CLOCK.equals(launcherActivityInfo.getComponentName()) &&
                Process.myUserHandle().equals(launcherActivityInfo.getUser())) {
            drawable = DynamicClock.getClock(mContext, iconDpi);
        }
        return drawable == null ? super.getIcon(launcherActivityInfo, iconDpi, flattenDrawable) : drawable;
    }

    @Override
    public String getSystemStateForPackage(String systemState, String packageName) {
        String suffix = isCalendar(packageName) ? " " + getDayOfMonth() : "";
        return super.getSystemStateForPackage(systemState, packageName) + suffix;
    }
}
