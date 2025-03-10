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

package com.saggitt.omega.dash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class DashListView extends RelativeLayout implements DashItemAdapter.DashItemChangeListener {

    public static float MoveAccumulator = 0;
    public float itemWith = 0;
    public float itemHeight = 0;
    public float layoutWidth;
    public float layoutHeight;
    public float layoutCenter_x;
    public float layoutCenter_y;
    public float radius;
    public ArrayList<View> itemViewList;
    private double intervalAngle = Math.PI / 4;
    private double pre_IntervalAngle = Math.PI / 4;
    private DashItemAdapter dashAdapter;

    public DashListView(Context context) {
        this(context, null, 0);
    }

    public DashListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashListView(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
        init();
    }

    private void init() {
        post(() -> {
            Log.d("CircularListView", "get layout width and height");
            layoutWidth = getWidth();
            layoutHeight = getHeight();
            layoutCenter_x = layoutWidth / 2;
            layoutCenter_y = layoutHeight / 2;
            radius = layoutWidth / 3;
        });

        itemViewList = new ArrayList<>();
    }

    public void setRadius(float r) {
        r = (r < 0) ? 0 : r;
        radius = r;

        if (dashAdapter != null)
            dashAdapter.notifyItemChange();

    }

    public void setAdapter(DashItemAdapter adapter) {
        // register item change listener
        this.dashAdapter = adapter;
        dashAdapter.setOnItemChangeListener(this);
        setItemPosition();
    }

    public double getIntervalAngle() {
        return intervalAngle;
    }

    @Override
    public void onDashItemChange() {
        setItemPosition();
    }

    private void setItemPosition() {
        int itemCount = dashAdapter.getCount();
        int existChildCount = getChildCount();
        boolean isLayoutEmpty = existChildCount == 0;

        pre_IntervalAngle = isLayoutEmpty ? 0 : 2.0f * Math.PI / (double) existChildCount;
        intervalAngle = 2.0f * Math.PI / (double) itemCount;


        // add all item view into parent layout
        for (int i = 0; i < dashAdapter.getCount(); i++) {
            final int idx = i;
            final View item = dashAdapter.getItemAt(i);

            // add item if no parent
            if (item.getParent() == null) {
                item.setVisibility(View.INVISIBLE);
                addView(item);
                System.out.println("do add :" + item);
            }

            // wait for view drawn to get width and height
            item.post(() -> {

                itemWith = item.getWidth();
                itemHeight = item.getHeight();
                /*
                 * position items according to circle formula
                 * margin left -> x = h + r * cos(theta)
                 * margin top -> y = k + r * sin(theta)
                 *
                 */
                ValueAnimator valueAnimator = new ValueAnimator();
                valueAnimator.setFloatValues((float) pre_IntervalAngle, (float) intervalAngle);
                valueAnimator.setDuration(500);
                valueAnimator.setInterpolator(new OvershootInterpolator());
                valueAnimator.addUpdateListener(animation -> {
                    float value = (Float) (animation.getAnimatedValue());
                    LayoutParams params = (LayoutParams) item.getLayoutParams();
                    params.setMargins(
                            (int) (layoutCenter_x - (itemWith / 2) + (radius *
                                    Math.cos(idx * value + DashListView.MoveAccumulator * Math.PI * 2))),
                            (int) (layoutCenter_y - (itemHeight / 2) + (radius *
                                    Math.sin(idx * value + DashListView.MoveAccumulator * Math.PI * 2))),
                            0,
                            0);
                    item.setLayoutParams(params);
                });
                valueAnimator.start();
                item.setVisibility(View.VISIBLE);
            });

        }

        // remove item from parent if it has been remove from list
        for (int i = 0; i < itemViewList.size(); i++) {
            View itemAfterChanged = itemViewList.get(i);
            if (dashAdapter.getAllViews().indexOf(itemAfterChanged) == -1) {
                System.out.println("do remove :" + itemAfterChanged);
                removeView(itemAfterChanged);
            }
        }
        itemViewList = (ArrayList<View>) dashAdapter.getAllViews().clone();
    }

}
