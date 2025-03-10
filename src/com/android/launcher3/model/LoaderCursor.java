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

package com.android.launcher3.model;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import androidx.annotation.VisibleForTesting;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.icons.IconCache;
import com.android.launcher3.icons.LauncherIcons;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.ContentWriter;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.util.IntSparseArrayMap;
import com.saggitt.omega.OmegaPreferences;

import java.net.URISyntaxException;
import java.security.InvalidParameterException;

import static android.graphics.BitmapFactory.decodeByteArray;

/**
 * Extension of {@link Cursor} with utility methods for workspace loading.
 */
public class LoaderCursor extends CursorWrapper {

    private static final String TAG = "LoaderCursor";

    public final LongSparseArray<UserHandle> allUsers;

    private final Uri mContentUri;
    private final Context mContext;
    private final PackageManager mPM;
    private final IconCache mIconCache;
    private final InvariantDeviceProfile mIDP;

    private final IntArray itemsToRemove = new IntArray();
    private final IntArray restoredRows = new IntArray();
    private final IntSparseArrayMap<GridOccupancy> occupied = new IntSparseArrayMap<>();

    private final int iconPackageIndex;
    private final int iconResourceIndex;
    private final int iconIndex;
    private final int customIconIndex;
    public final int titleIndex;

    private final int idIndex;
    private final int containerIndex;
    private final int itemTypeIndex;
    private final int screenIndex;
    private final int cellXIndex;
    private final int cellYIndex;
    private final int profileIdIndex;
    private final int restoredIndex;
    private final int intentIndex;

    // Properties loaded per iteration
    public long serialNumber;
    public UserHandle user;
    public int id;
    public int container;
    public int itemType;
    public int restoreFlag;

    private final OmegaPreferences prefs;

    public LoaderCursor(Cursor cursor, Uri contentUri, LauncherAppState app,
                        UserManagerState userManagerState) {
        super(cursor);

        allUsers = userManagerState.allUsers;
        mContentUri = contentUri;
        mContext = app.getContext();
        mIconCache = app.getIconCache();
        mIDP = app.getInvariantDeviceProfile();
        mPM = mContext.getPackageManager();

        // Init column indices
        iconIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
        customIconIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CUSTOM_ICON);
        iconPackageIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
        iconResourceIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
        titleIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);

        idIndex = getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
        containerIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        itemTypeIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
        screenIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        cellXIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        cellYIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        profileIdIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.PROFILE_ID);
        restoredIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.RESTORED);
        intentIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);

        prefs = Utilities.getOmegaPrefs(mContext);
    }

    @Override
    public boolean moveToNext() {
        boolean result = super.moveToNext();
        if (result) {
            // Load common properties.
            itemType = getInt(itemTypeIndex);
            container = getInt(containerIndex);
            id = getInt(idIndex);
            serialNumber = getInt(profileIdIndex);
            user = allUsers.get(serialNumber);
            restoreFlag = getInt(restoredIndex);
        }
        return result;
    }

    public Intent parseIntent() {
        String intentDescription = getString(intentIndex);
        try {
            return TextUtils.isEmpty(intentDescription) ?
                    null : Intent.parseUri(intentDescription, 0);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error parsing Intent");
            return null;
        }
    }

    @VisibleForTesting
    public WorkspaceItemInfo loadSimpleWorkspaceItem() {
        final WorkspaceItemInfo info = new WorkspaceItemInfo();
        info.intent = new Intent();
        // Non-app shortcuts are only supported for current user.
        info.user = user;
        info.itemType = itemType;
        info.title = getTitle();
        // the fallback icon
        if (!loadIcon(info)) {
            info.bitmap = mIconCache.getDefaultIcon(info.user);
        }

        // TODO: If there's an explicit component and we can't install that, delete it.

        return info;
    }

    /**
     * Loads the icon from the cursor and updates the {@param info} if the icon is an app resource.
     */
    protected boolean loadIcon(WorkspaceItemInfo info) {
        try (LauncherIcons li = LauncherIcons.obtain(mContext)) {
            if (itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                String packageName = getString(iconPackageIndex);
                String resourceName = getString(iconResourceIndex);
                if (!TextUtils.isEmpty(packageName) || !TextUtils.isEmpty(resourceName)) {
                    info.iconResource = new ShortcutIconResource();
                    info.iconResource.packageName = packageName;
                    info.iconResource.resourceName = resourceName;
                    BitmapInfo iconInfo = li.createIconBitmap(info.iconResource);
                    if (iconInfo != null) {
                        info.bitmap = iconInfo;
                        return true;
                    }
                }
            }

            // Failed to load from resource, try loading from DB.
            byte[] data = getBlob(iconIndex);
            try {
                info.bitmap = li.createIconBitmap(decodeByteArray(data, 0, data.length));
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode byte array for info " + info, e);
                return false;
            }
        }
    }

    public Bitmap loadCustomIcon(WorkspaceItemInfo info) {
        byte[] data = getBlob(customIconIndex);
        try {
            if (data != null) {
                return LauncherIcons.obtain(mContext).createIconBitmap(
                        BitmapFactory.decodeByteArray(data, 0, data.length)).icon;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load custom icon for info " + info, e);
            return null;
        }
    }

    /**
     * Returns the title or empty string
     */
    private String getTitle() {
        String title = getString(titleIndex);
        return TextUtils.isEmpty(title) ? "" : Utilities.trim(title);
    }

    /**
     * Make an WorkspaceItemInfo object for a restored application or shortcut item that points
     * to a package that is not yet installed on the system.
     */
    public WorkspaceItemInfo getRestoredItemInfo(Intent intent) {
        final WorkspaceItemInfo info = new WorkspaceItemInfo();
        info.user = user;
        info.intent = intent;

        // the fallback icon
        if (!loadIcon(info)) {
            mIconCache.getTitleAndIcon(info, false /* useLowResIcon */);
        }

        if (hasRestoreFlag(WorkspaceItemInfo.FLAG_RESTORED_ICON)) {
            String title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                info.title = Utilities.trim(title);
            }
        } else if (hasRestoreFlag(WorkspaceItemInfo.FLAG_AUTOINSTALL_ICON)) {
            if (TextUtils.isEmpty(info.title)) {
                info.title = getTitle();
            }
        } else {
            throw new InvalidParameterException("Invalid restoreType " + restoreFlag);
        }

        info.contentDescription = mPM.getUserBadgedLabel(info.title, info.user);
        info.itemType = itemType;
        info.status = restoreFlag;
        return info;
    }

    /**
     * Make an WorkspaceItemInfo object for a shortcut that is an application.
     */
    public WorkspaceItemInfo getAppShortcutInfo(
            Intent intent, boolean allowMissingTarget, boolean useLowResIcon) {
        if (user == null) {
            Log.d(TAG, "Null user found in getShortcutInfo");
            return null;
        }

        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            Log.d(TAG, "Missing component found in getShortcutInfo");
            return null;
        }

        Intent newIntent = new Intent(Intent.ACTION_MAIN, null);
        newIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        newIntent.setComponent(componentName);
        LauncherActivityInfo lai = mContext.getSystemService(LauncherApps.class)
                .resolveActivity(newIntent, user);
        if ((lai == null) && !allowMissingTarget) {
            Log.d(TAG, "Missing activity found in getShortcutInfo: " + componentName);
            return null;
        }

        final WorkspaceItemInfo info = new WorkspaceItemInfo();
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        info.user = user;
        info.intent = newIntent;

        mIconCache.getTitleAndIcon(info, lai, useLowResIcon);
        if (mIconCache.isDefaultIcon(info.bitmap, user)) {
            loadIcon(info);
        }

        if (lai != null) {
            AppInfo.updateRuntimeFlagsForActivityTarget(info, lai);
        }

        // from the db
        if (TextUtils.isEmpty(info.title)) {
            info.title = getTitle();
        }

        // fall back to the class name of the activity
        if (info.title == null) {
            info.title = componentName.getClassName();
        }

        info.contentDescription = mPM.getUserBadgedLabel(info.title, info.user);
        return info;
    }

    /**
     * Returns a {@link ContentWriter} which can be used to update the current item.
     */
    public ContentWriter updater() {
       return new ContentWriter(mContext, new ContentWriter.CommitParams(
               BaseColumns._ID + "= ?", new String[]{Integer.toString(id)}));
    }

    /**
     * Marks the current item for removal
     */
    public void markDeleted(String reason) {
        FileLog.e(TAG, reason);
        itemsToRemove.add(id);
    }

    /**
     * Removes any items marked for removal.
     * @return true is any item was removed.
     */
    public boolean commitDeleted() {
        if (itemsToRemove.size() > 0) {
            // Remove dead items
            mContext.getContentResolver().delete(mContentUri, Utilities.createDbSelectionQuery(
                    LauncherSettings.Favorites._ID, itemsToRemove), null);
            return true;
        }
        return false;
    }

    /**
     * Marks the current item as restored
     */
    public void markRestored() {
        if (restoreFlag != 0) {
            restoredRows.add(id);
            restoreFlag = 0;
        }
    }

    public boolean hasRestoreFlag(int flagMask) {
        return (restoreFlag & flagMask) != 0;
    }

    public void commitRestoredItems() {
        if (restoredRows.size() > 0) {
            // Update restored items that no longer require special handling
            ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites.RESTORED, 0);
            mContext.getContentResolver().update(mContentUri, values,
                    Utilities.createDbSelectionQuery(
                            LauncherSettings.Favorites._ID, restoredRows), null);
        }
    }

    /**
     * Returns true is the item is on workspace or hotseat
     */
    public boolean isOnWorkspaceOrHotseat() {
        return container == LauncherSettings.Favorites.CONTAINER_DESKTOP ||
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT;
    }

    /**
     * Applies the following properties:
     * {@link ItemInfo#id}
     * {@link ItemInfo#container}
     * {@link ItemInfo#screenId}
     * {@link ItemInfo#cellX}
     * {@link ItemInfo#cellY}
     */
    public void applyCommonProperties(ItemInfo info) {
        info.id = id;
        info.container = container;
        info.screenId = getInt(screenIndex);
        info.cellX = getInt(cellXIndex);
        info.cellY = getInt(cellYIndex);
    }

    /**
     * Adds the {@param info} to {@param dataModel} if it does not overlap with any other item,
     * otherwise marks it for deletion.
     */
    public void checkAndAddItem(ItemInfo info, BgDataModel dataModel) {
        if (checkItemPlacement(info)) {
            dataModel.addItem(mContext, info, false);
        } else {
            markDeleted("Item position overlap");
        }
    }

    /**
     * check & update map of what's occupied; used to discard overlapping/invalid items
     */
    protected boolean checkItemPlacement(ItemInfo item) {
        int containerIndex = item.screenId;
        if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            final GridOccupancy hotseatOccupancy =
                    occupied.get(LauncherSettings.Favorites.CONTAINER_HOTSEAT);

            int hotseatRows = Utilities.getOmegaPrefs(mContext).getDockRowsCount();
            int hotseatSize = mIDP.numHotseatIcons;
            int hotseatX = (int) (item.screenId % hotseatSize);
            int hotseatY = (int) (item.screenId / hotseatSize);

            if (item.screenId >= mIDP.numHotseatIcons) {
                Log.e(TAG, "Error loading shortcut " + item
                        + " into hotseat position " + item.screenId
                        + ", position out of bounds: (0 to " + (mIDP.numHotseatIcons - 1)
                        + ")");
                return false;
            }

            if (hotseatOccupancy != null) {
                if (hotseatOccupancy.cells[hotseatX][hotseatY]) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                            + " into position (" + item.screenId + ":" + item.cellX + ","
                            + item.cellY + ") already occupied");
                    return false;
                } else {
                    hotseatOccupancy.cells[hotseatX][hotseatY] = true;
                    return true;
                }
            } else {
                final GridOccupancy occupancy = new GridOccupancy(hotseatSize, hotseatRows);
                occupancy.cells[hotseatX][hotseatY] = true;
                occupied.put(LauncherSettings.Favorites.CONTAINER_HOTSEAT, occupancy);
                return true;
            }
        } else if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // Skip further checking if it is not the hotseat or workspace container
            return true;
        }

        final int countX = mIDP.numColumns;
        final int countY = mIDP.numRows;
        if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                item.cellX < 0 || item.cellY < 0 ||
                item.cellX + item.spanX > countX || item.cellY + item.spanY > countY) {
            Log.e(TAG, "Error loading shortcut " + item
                    + " into cell (" + containerIndex + "-" + item.screenId + ":"
                    + item.cellX + "," + item.cellY
                    + ") out of screen bounds ( " + countX + "x" + countY + ")");
            return false;
        }

        if (!occupied.containsKey(item.screenId)) {
            GridOccupancy screen = new GridOccupancy(countX + 1, countY + 1);
            if (item.screenId == Workspace.FIRST_SCREEN_ID) {
                // Mark the first row as occupied (if the feature is enabled)
                // in order to account for the QSB.
                screen.markCells(0, 0, countX + 1, 1, FeatureFlags.QSB_ON_FIRST_SCREEN);
            }
            occupied.put(item.screenId, screen);
        }
        final GridOccupancy occupancy = occupied.get(item.screenId);

        // Check if any workspace icons overlap with each other
        if (occupancy.isRegionVacant(item.cellX, item.cellY, item.spanX, item.spanY)) {
            occupancy.markCells(item, true);
            return true;
        } else {
            Log.e(TAG, "Error loading shortcut " + item
                    + " into cell (" + containerIndex + "-" + item.screenId + ":"
                    + item.cellX + "," + item.cellX + "," + item.spanX + "," + item.spanY
                    + ") already occupied");
            return prefs.getAllowOverlap();
        }
    }
}
