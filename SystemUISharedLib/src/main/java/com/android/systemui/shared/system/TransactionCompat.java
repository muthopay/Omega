/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.shared.system;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;

public class TransactionCompat {

    final Transaction mTransaction;

    final float[] mTmpValues = new float[9];

    public TransactionCompat() {
        mTransaction = new Transaction();
    }

    public void apply() {
        mTransaction.apply();
    }

    public TransactionCompat show(SurfaceControlCompat surfaceControl) {
        mTransaction.show(surfaceControl.mSurfaceControl);
        return this;
    }

    public TransactionCompat hide(SurfaceControlCompat surfaceControl) {
        mTransaction.hide(surfaceControl.mSurfaceControl);
        return this;
    }

    public TransactionCompat setPosition(SurfaceControlCompat surfaceControl, float x, float y) {
        mTransaction.setPosition(surfaceControl.mSurfaceControl, x, y);
        return this;
    }

    public static void deferTransactionUntil(Transaction t, SurfaceControl surfaceControl,
                                             SurfaceControl barrier, long frameNumber) {
        t.deferTransactionUntil(surfaceControl, barrier, frameNumber);
    }

    public TransactionCompat setLayer(SurfaceControlCompat surfaceControl, int z) {
        mTransaction.setLayer(surfaceControl.mSurfaceControl, z);
        return this;
    }

    public TransactionCompat setAlpha(SurfaceControlCompat surfaceControl, float alpha) {
        mTransaction.setAlpha(surfaceControl.mSurfaceControl, alpha);
        return this;
    }

    public static void setRelativeLayer(Transaction t, SurfaceControl surfaceControl,
                                        SurfaceControl relativeTo, int z) {
        t.setRelativeLayer(surfaceControl, relativeTo, z);
    }

    public TransactionCompat setMatrix(SurfaceControlCompat surfaceControl, Matrix matrix) {
        mTransaction.setMatrix(surfaceControl.mSurfaceControl, matrix, mTmpValues);
        return this;
    }

    public TransactionCompat setWindowCrop(SurfaceControlCompat surfaceControl, Rect crop) {
        mTransaction.setWindowCrop(surfaceControl.mSurfaceControl, crop);
        return this;
    }

    @Deprecated
    public static void setEarlyWakeup(Transaction t) {
    }

    public TransactionCompat setSize(SurfaceControlCompat surfaceControl, int w, int h) {
        mTransaction.setBufferSize(surfaceControl.mSurfaceControl, w, h);
        return this;
    }

    public TransactionCompat setMatrix(SurfaceControlCompat surfaceControl, float dsdx, float dtdx,
                                       float dtdy, float dsdy) {
        mTransaction.setMatrix(surfaceControl.mSurfaceControl, dsdx, dtdx, dtdy, dsdy);
        return this;
    }

    public TransactionCompat setCornerRadius(SurfaceControlCompat surfaceControl, float radius) {
        mTransaction.setCornerRadius(surfaceControl.mSurfaceControl, radius);
        return this;
    }

    public TransactionCompat setColor(SurfaceControlCompat surfaceControl, float[] color) {
        mTransaction.setColor(surfaceControl.mSurfaceControl, color);
        return this;
    }

    public TransactionCompat setBackgroundBlurRadius(SurfaceControlCompat surfaceControl,
                                                     int radius) {
        mTransaction.setBackgroundBlurRadius(surfaceControl.mSurfaceControl, radius);
        return this;
    }

    public TransactionCompat deferTransactionUntil(SurfaceControlCompat surfaceControl,
                                                   SurfaceControl barrier, long frameNumber) {
        mTransaction.deferTransactionUntil(surfaceControl.mSurfaceControl, barrier,
                frameNumber);
        return this;
    }

    @Deprecated
    public TransactionCompat setEarlyWakeup() {
        return this;
    }
}
