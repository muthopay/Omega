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

package com.saggitt.omega.feed;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

public class WidgetPickerAdapter extends
        RecyclerView.Adapter<WidgetPickerAdapter.WidgetsViewHolder> {

    private Context mContext;
    private List<Item> mItems = new ArrayList<>();

    private OnClickListener mOnClickListener;
    private static final String TAG = "AddedWidgetsAdapter";

    public WidgetPickerAdapter(Context context) {
        this.mContext = context;
        mOnClickListener = (OnClickListener) mContext;
    }

    @NonNull
    @Override
    public WidgetsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_all_widget, viewGroup,
                false);
        WidgetsViewHolder widgetsViewHolder = new WidgetsViewHolder(view);
        widgetsViewHolder.itemView.setOnClickListener(v -> {
            int position = widgetsViewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                mOnClickListener.onClick(mItems.get(position));
            }
        });
        return widgetsViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull WidgetsViewHolder widgetsViewHolder, int i) {
        Item info = mItems.get(i);
        widgetsViewHolder.icon.setImageDrawable(info.icon);
        widgetsViewHolder.label.setText(info.label);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setItems(List<Item> items) {
        this.mItems = items;
        notifyDataSetChanged();
    }

    public static class WidgetsViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView label;

        public WidgetsViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.widget_icon);
            label = itemView.findViewById(R.id.widget_label);
        }

    }

    public static class Item {
        public UserHandle profile;
        CharSequence label;
        Drawable icon;
        String packageName;
        String className;
        Bundle extras;

        /**
         * Create a list item from given label and icon.
         */
        Item(CharSequence label, Drawable icon) {
            this.label = label;
            this.icon = icon;
        }

        /**
         * Create a list item and fill it with details from the given
         * {@link ResolveInfo} object.
         */
        Item(PackageManager pm, ResolveInfo resolveInfo) {
            label = resolveInfo.loadLabel(pm);
            if (label == null && resolveInfo.activityInfo != null) {
                label = resolveInfo.activityInfo.name;
            }

            icon = resolveInfo.loadIcon(pm);
            packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            className = resolveInfo.activityInfo.name;
        }

        /**
         * Build the {@link Intent} described by this item. If this item
         * can't create a valid {@link android.content.ComponentName}, it will return
         * {@link Intent#ACTION_CREATE_SHORTCUT} filled with the item label.
         */
        Intent getIntent(Intent baseIntent) {
            Intent intent = new Intent(baseIntent);
            if (packageName != null && className != null) {
                // Valid package and class, so fill details as normal intent
                intent.setClassName(packageName, className);
                if (extras != null) {
                    intent.putExtras(extras);
                }
            } else {
                // No valid package or class, so treat as shortcut with label
                intent.setAction(Intent.ACTION_CREATE_SHORTCUT);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            }
            return intent;
        }

        public CharSequence getLabel() {
            return label;
        }
    }

    interface OnClickListener {
        void onClick(Item item);
    }
}
