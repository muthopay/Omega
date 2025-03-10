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

package com.android.systemui.shared.system;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;

import com.android.internal.os.SomeArgs;
import com.android.systemui.shared.recents.model.ThumbnailData;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all the task stack listeners
 */
public class TaskStackChangeListeners extends TaskStackListener {

    private static final String TAG = TaskStackChangeListeners.class.getSimpleName();

    /**
     * List of {@link TaskStackChangeListener} registered from {@link #addListener}.
     */
    private final List<TaskStackChangeListener> mTaskStackListeners = new ArrayList<>();
    private final List<TaskStackChangeListener> mTmpListeners = new ArrayList<>();

    protected final Handler mHandler;
    private boolean mRegistered;

    public TaskStackChangeListeners(Looper looper) {
        mHandler = new H(looper);
    }

    public void addListener(IActivityManager am, TaskStackChangeListener listener) {
        synchronized (mTaskStackListeners) {
            mTaskStackListeners.add(listener);
        }
        if (!mRegistered) {
            // Register mTaskStackListener to IActivityManager only once if needed.
            try {
                ActivityTaskManager.getService().registerTaskStackListener(this);
                mRegistered = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to call registerTaskStackListener", e);
            }
        }
    }

    public void removeListener(TaskStackChangeListener listener) {
        boolean isEmpty;
        synchronized (mTaskStackListeners) {
            mTaskStackListeners.remove(listener);
            isEmpty = mTaskStackListeners.isEmpty();
        }
        if (isEmpty && mRegistered) {
            // Unregister mTaskStackListener once we have no more listeners
            try {
                ActivityTaskManager.getService().unregisterTaskStackListener(this);
                mRegistered = false;
            } catch (Exception e) {
                Log.w(TAG, "Failed to call unregisterTaskStackListener", e);
            }
        }
    }

    @Override
    public void onTaskStackChanged() throws RemoteException {
        // Call the task changed callback for the non-ui thread listeners first. Copy to a set of
        // temp listeners so that we don't lock on mTaskStackListeners while calling all the
        // callbacks. This call is always on the same binder thread, so we can just synchronize
        // on the copying of the listener list.
        synchronized (mTaskStackListeners) {
            mTmpListeners.addAll(mTaskStackListeners);
        }
        for (int i = mTmpListeners.size() - 1; i >= 0; i--) {
            mTmpListeners.get(i).onTaskStackChangedBackground();
        }
        mTmpListeners.clear();

        mHandler.removeMessages(H.ON_TASK_STACK_CHANGED);
        mHandler.sendEmptyMessage(H.ON_TASK_STACK_CHANGED);
    }

    @Override
    public void onActivityPinned(String packageName, int userId, int taskId, int stackId)
            throws RemoteException {
        mHandler.removeMessages(H.ON_ACTIVITY_PINNED);
        mHandler.obtainMessage(H.ON_ACTIVITY_PINNED,
                new PinnedActivityInfo(packageName, userId, taskId, stackId)).sendToTarget();
    }

    @Override
    public void onActivityUnpinned() throws RemoteException {
        mHandler.removeMessages(H.ON_ACTIVITY_UNPINNED);
        mHandler.sendEmptyMessage(H.ON_ACTIVITY_UNPINNED);
    }

    @Override
    public void onActivityRestartAttempt(RunningTaskInfo task, boolean homeTaskVisible,
                                         boolean clearedTask, boolean wasVisible) throws RemoteException {
        final SomeArgs args = SomeArgs.obtain();
        args.arg1 = task;
        args.argi1 = homeTaskVisible ? 1 : 0;
        args.argi2 = clearedTask ? 1 : 0;
        args.argi3 = wasVisible ? 1 : 0;
        mHandler.removeMessages(H.ON_ACTIVITY_RESTART_ATTEMPT);
        mHandler.obtainMessage(H.ON_ACTIVITY_RESTART_ATTEMPT, args).sendToTarget();
    }

    @Override
    public void onActivityForcedResizable(String packageName, int taskId, int reason)
            throws RemoteException {
        mHandler.obtainMessage(H.ON_ACTIVITY_FORCED_RESIZABLE, taskId, reason, packageName)
                .sendToTarget();
    }

    @Override
    public void onActivityDismissingDockedStack() throws RemoteException {
        mHandler.sendEmptyMessage(H.ON_ACTIVITY_DISMISSING_DOCKED_STACK);
    }

    @Override
    public void onActivityLaunchOnSecondaryDisplayFailed(RunningTaskInfo taskInfo,
                                                         int requestedDisplayId) throws RemoteException {
        mHandler.obtainMessage(H.ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED, requestedDisplayId,
                0 /* unused */,
                taskInfo).sendToTarget();
    }

    @Override
    public void onActivityLaunchOnSecondaryDisplayRerouted(RunningTaskInfo taskInfo,
                                                           int requestedDisplayId) throws RemoteException {
        mHandler.obtainMessage(H.ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_REROUTED,
                requestedDisplayId, 0 /* unused */, taskInfo).sendToTarget();
    }

    @Override
    public void onTaskProfileLocked(int taskId, int userId) throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_PROFILE_LOCKED, taskId, userId).sendToTarget();
    }

    @Override
    public void onTaskSnapshotChanged(int taskId, TaskSnapshot snapshot) throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_SNAPSHOT_CHANGED, taskId, 0, snapshot).sendToTarget();
    }

    @Override
    public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_CREATED, taskId, 0, componentName).sendToTarget();
    }

    @Override
    public void onTaskRemoved(int taskId) throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_REMOVED, taskId, 0).sendToTarget();
    }

    @Override
    public void onTaskMovedToFront(RunningTaskInfo taskInfo)
            throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_MOVED_TO_FRONT, taskInfo).sendToTarget();
    }

    @Override
    public void onBackPressedOnTaskRoot(RunningTaskInfo taskInfo) throws RemoteException {
        mHandler.obtainMessage(H.ON_BACK_PRESSED_ON_TASK_ROOT, taskInfo).sendToTarget();
    }

    @Override
    public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation)
            throws RemoteException {
        mHandler.obtainMessage(H.ON_ACTIVITY_REQUESTED_ORIENTATION_CHANGE, taskId,
                requestedOrientation).sendToTarget();
    }

    @Override
    public void onSizeCompatModeActivityChanged(int displayId, IBinder activityToken)
            throws RemoteException {
        mHandler.obtainMessage(H.ON_SIZE_COMPAT_MODE_ACTIVITY_CHANGED, displayId, 0 /* unused */,
                activityToken).sendToTarget();
    }

    @Override
    public void onSingleTaskDisplayDrawn(int displayId) throws RemoteException {
        mHandler.obtainMessage(H.ON_SINGLE_TASK_DISPLAY_DRAWN, displayId,
                0 /* unused */).sendToTarget();
    }

    @Override
    public void onSingleTaskDisplayEmpty(int displayId) throws RemoteException {
        mHandler.obtainMessage(H.ON_SINGLE_TASK_DISPLAY_EMPTY, displayId,
                0 /* unused */).sendToTarget();
    }

    @Override
    public void onTaskDisplayChanged(int taskId, int newDisplayId) throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_DISPLAY_CHANGED, taskId, newDisplayId).sendToTarget();
    }

    @Override
    public void onRecentTaskListUpdated() throws RemoteException {
        mHandler.obtainMessage(H.ON_TASK_LIST_UPDATED).sendToTarget();
    }

    @Override
    public void onRecentTaskListFrozenChanged(boolean frozen) {
        mHandler.obtainMessage(H.ON_TASK_LIST_FROZEN_UNFROZEN, frozen ? 1 : 0, 0 /* unused */)
                .sendToTarget();
    }

    @Override
    public void onTaskDescriptionChanged(RunningTaskInfo taskInfo) {
        mHandler.obtainMessage(H.ON_TASK_DESCRIPTION_CHANGED, taskInfo).sendToTarget();
    }

    @Override
    public void onActivityRotation(int displayId) {
        mHandler.obtainMessage(H.ON_ACTIVITY_ROTATION, displayId, 0 /* unused */)
                .sendToTarget();
    }

    protected final class H extends Handler {
        private static final int ON_TASK_STACK_CHANGED = 1;
        private static final int ON_TASK_SNAPSHOT_CHANGED = 2;
        private static final int ON_ACTIVITY_PINNED = 3;
        private static final int ON_ACTIVITY_RESTART_ATTEMPT = 4;
        private static final int ON_ACTIVITY_FORCED_RESIZABLE = 6;
        private static final int ON_ACTIVITY_DISMISSING_DOCKED_STACK = 7;
        private static final int ON_TASK_PROFILE_LOCKED = 8;
        private static final int ON_ACTIVITY_UNPINNED = 10;
        protected static final int ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED = 11;
        private static final int ON_TASK_CREATED = 12;
        private static final int ON_TASK_REMOVED = 13;
        protected static final int ON_TASK_MOVED_TO_FRONT = 14;
        private static final int ON_ACTIVITY_REQUESTED_ORIENTATION_CHANGE = 15;
        protected static final int ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_REROUTED = 16;
        protected static final int ON_SIZE_COMPAT_MODE_ACTIVITY_CHANGED = 17;
        protected static final int ON_BACK_PRESSED_ON_TASK_ROOT = 18;
        private static final int ON_SINGLE_TASK_DISPLAY_DRAWN = 19;
        protected static final int ON_TASK_DISPLAY_CHANGED = 20;
        private static final int ON_TASK_LIST_UPDATED = 21;
        private static final int ON_SINGLE_TASK_DISPLAY_EMPTY = 22;
        private static final int ON_TASK_LIST_FROZEN_UNFROZEN = 23;
        private static final int ON_TASK_DESCRIPTION_CHANGED = 24;
        private static final int ON_ACTIVITY_ROTATION = 25;


        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            synchronized (mTaskStackListeners) {
                switch (msg.what) {
                    case ON_TASK_STACK_CHANGED: {
                        Trace.beginSection("onTaskStackChanged");
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskStackChanged();
                        }
                        Trace.endSection();
                        break;
                    }
                    case ON_TASK_SNAPSHOT_CHANGED: {
                        Trace.beginSection("onTaskSnapshotChanged");
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskSnapshotChanged(msg.arg1,
                                    new ThumbnailData((TaskSnapshot) msg.obj));
                        }
                        Trace.endSection();
                        break;
                    }
                    case ON_ACTIVITY_PINNED: {
                        final PinnedActivityInfo info = (PinnedActivityInfo) msg.obj;
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onActivityPinned(
                                    info.mPackageName, info.mUserId, info.mTaskId, info.mStackId);
                        }
                        break;
                    }
                    case ON_ACTIVITY_UNPINNED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onActivityUnpinned();
                        }
                        break;
                    }
                    case ON_ACTIVITY_RESTART_ATTEMPT: {
                        final SomeArgs args = (SomeArgs) msg.obj;
                        final RunningTaskInfo task = (RunningTaskInfo) args.arg1;
                        final boolean homeTaskVisible = args.argi1 != 0;
                        final boolean clearedTask = args.argi2 != 0;
                        final boolean wasVisible = args.argi3 != 0;
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onActivityRestartAttempt(task,
                                    homeTaskVisible, clearedTask, wasVisible);
                        }
                        break;
                    }
                    case ON_ACTIVITY_FORCED_RESIZABLE: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onActivityForcedResizable(
                                    (String) msg.obj, msg.arg1, msg.arg2);
                        }
                        break;
                    }
                    case ON_ACTIVITY_DISMISSING_DOCKED_STACK: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onActivityDismissingDockedStack();
                        }
                        break;
                    }
                    case ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED: {
                        final RunningTaskInfo info = (RunningTaskInfo) msg.obj;
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i)
                                    .onActivityLaunchOnSecondaryDisplayFailed(info);
                        }
                        break;
                    }
                    case ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_REROUTED: {
                        final RunningTaskInfo info = (RunningTaskInfo) msg.obj;
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i)
                                    .onActivityLaunchOnSecondaryDisplayRerouted(info);
                        }
                        break;
                    }
                    case ON_TASK_PROFILE_LOCKED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskProfileLocked(msg.arg1, msg.arg2);
                        }
                        break;
                    }
                    case ON_TASK_CREATED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskCreated(msg.arg1,
                                    (ComponentName) msg.obj);
                        }
                        break;
                    }
                    case ON_TASK_REMOVED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskRemoved(msg.arg1);
                        }
                        break;
                    }
                    case ON_TASK_MOVED_TO_FRONT: {
                        final RunningTaskInfo info = (RunningTaskInfo) msg.obj;
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskMovedToFront(info);
                        }
                        break;
                    }
                    case ON_ACTIVITY_REQUESTED_ORIENTATION_CHANGE: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i)
                                    .onActivityRequestedOrientationChanged(msg.arg1, msg.arg2);
                        }
                        break;
                    }
                    case ON_SIZE_COMPAT_MODE_ACTIVITY_CHANGED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onSizeCompatModeActivityChanged(
                                    msg.arg1, (IBinder) msg.obj);
                        }
                        break;
                    }
                    case ON_BACK_PRESSED_ON_TASK_ROOT: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onBackPressedOnTaskRoot(
                                    (RunningTaskInfo) msg.obj);
                        }
                        break;
                    }
                    case ON_SINGLE_TASK_DISPLAY_DRAWN: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onSingleTaskDisplayDrawn(msg.arg1);
                        }
                        break;
                    }
                    case ON_SINGLE_TASK_DISPLAY_EMPTY: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onSingleTaskDisplayEmpty(
                                    msg.arg1);
                        }
                        break;
                    }
                    case ON_TASK_DISPLAY_CHANGED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskDisplayChanged(msg.arg1, msg.arg2);
                        }
                        break;
                    }
                    case ON_TASK_LIST_UPDATED: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onRecentTaskListUpdated();
                        }
                        break;
                    }
                    case ON_TASK_LIST_FROZEN_UNFROZEN: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onRecentTaskListFrozenChanged(msg.arg1 != 0);
                        }
                        break;
                    }
                    case ON_TASK_DESCRIPTION_CHANGED: {
                        final RunningTaskInfo info = (RunningTaskInfo) msg.obj;
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onTaskDescriptionChanged(info);
                        }
                        break;
                    }
                    case ON_ACTIVITY_ROTATION: {
                        for (int i = mTaskStackListeners.size() - 1; i >= 0; i--) {
                            mTaskStackListeners.get(i).onActivityRotation(msg.arg1);
                        }
                        break;
                    }
                }
            }
            if (msg.obj instanceof SomeArgs) {
                ((SomeArgs) msg.obj).recycle();
            }
        }
    }

    private static class PinnedActivityInfo {
        final String mPackageName;
        final int mUserId;
        final int mTaskId;
        final int mStackId;

        PinnedActivityInfo(String packageName, int userId, int taskId, int stackId) {
            mPackageName = packageName;
            mUserId = userId;
            mTaskId = taskId;
            mStackId = stackId;
        }
    }
}
