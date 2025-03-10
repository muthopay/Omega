/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.launcher3.statemanager;

import static com.android.launcher3.LauncherState.FLAG_NON_INTERACTIVE;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.CallSuper;

import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.LauncherRootView;
import com.android.launcher3.Utilities;
import com.android.launcher3.statemanager.StateManager.AtomicAnimationFactory;
import com.android.launcher3.statemanager.StateManager.StateHandler;
import com.android.launcher3.views.BaseDragLayer;

/**
 * Abstract activity with state management
 *
 * @param <STATE_TYPE> Type of state object
 */
public abstract class StatefulActivity<STATE_TYPE extends BaseState<STATE_TYPE>>
        extends BaseDraggingActivity {

    public final Handler mHandler = new Handler();
    private boolean mDeferredResumePending;
    private final Runnable mHandleDeferredResume = this::handleDeferredResume;
    private LauncherRootView mRootView;

    /**
     * Create handlers to control the property changes for this activity
     */
    protected abstract StateHandler<STATE_TYPE>[] createStateHandlers();

    /**
     * Returns true if the activity is in the provided state
     */
    public boolean isInState(STATE_TYPE state) {
        return getStateManager().getState() == state;
    }

    /**
     * Returns the state manager for this activity
     */
    public abstract StateManager<STATE_TYPE> getStateManager();

    protected void inflateRootView(int layoutId) {
        mRootView = (LauncherRootView) LayoutInflater.from(this).inflate(layoutId, null);
        mRootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public final LauncherRootView getRootView() {
        return mRootView;
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return mRootView.findViewById(id);
    }

    /**
     * Called when transition to the state starts
     */
    @CallSuper
    public void onStateSetStart(STATE_TYPE state) {
        if (mDeferredResumePending) {
            handleDeferredResume();
        }
    }

    /**
     * Called when transition to state ends
     */
    public void onStateSetEnd(STATE_TYPE state) {
    }

    /**
     * Creates a factory for atomic state animations
     */
    public AtomicAnimationFactory<STATE_TYPE> createAtomicAnimationFactory() {
        return new AtomicAnimationFactory(0);
    }

    @Override
    public void reapplyUi() {
        reapplyUi(true /* cancelCurrentAnimation */);
    }

    /**
     * Re-applies if any state transition is not running, optionally cancelling
     * the transition if requested.
     */
    public void reapplyUi(boolean cancelCurrentAnimation) {
        getRootView().dispatchInsets();
        getStateManager().reapplyState(cancelCurrentAnimation);
    }

    @Override
    protected void onStop() {
        BaseDragLayer dragLayer = getDragLayer();
        final boolean wasActive = isUserActive();
        final STATE_TYPE origState = getStateManager().getState();
        final int origDragLayerChildCount = dragLayer.getChildCount();
        super.onStop();

        getStateManager().moveToRestState();

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);

        if (wasActive) {
            // The expected condition is that this activity is stopped because the device goes to
            // sleep and the UI may have noticeable changes.
            dragLayer.post(() -> {
                if ((!getStateManager().isInStableState(origState)
                        // The drag layer may be animating (e.g. dismissing QSB).
                        || dragLayer.getAlpha() < 1
                        // Maybe an ArrowPopup is closed.
                        || dragLayer.getChildCount() != origDragLayerChildCount)) {
                    onUiChangedWhileSleeping();
                }
            });
        }
    }

    /**
     * Called if the Activity UI changed while the activity was not visible
     */
    protected void onUiChangedWhileSleeping() {
    }

    private void handleDeferredResume() {
        if (hasBeenResumed() && !getStateManager().getState().hasFlag(FLAG_NON_INTERACTIVE)) {
            onDeferredResumed();
            addActivityFlags(ACTIVITY_STATE_DEFERRED_RESUMED);

            mDeferredResumePending = false;
        } else {
            mDeferredResumePending = true;
        }
    }

    /**
     * Called want the activity has stayed resumed for 1 frame.
     */
    protected void onDeferredResumed() {
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler.removeCallbacks(mHandleDeferredResume);
        Utilities.postAsyncCallback(mHandler, mHandleDeferredResume);
    }
}
