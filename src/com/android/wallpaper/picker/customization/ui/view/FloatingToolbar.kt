/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.common.ui.view.ItemSpacing
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator

class FloatingToolbar(
    context: Context,
    attrs: AttributeSet?,
) :
    FrameLayout(
        context,
        attrs,
    ) {

    private val tabList: RecyclerView

    init {
        inflate(context, R.layout.floating_toolbar, this)
        tabList =
            requireViewById<RecyclerView>(R.id.tab_list).apply {
                itemAnimator = TabItemAnimator()
                addItemDecoration(ItemSpacing(TAB_SPACE_DP))
            }
    }

    fun setAdapter(floatingToolbarTabAdapter: FloatingToolbarTabAdapter) {
        tabList.adapter = floatingToolbarTabAdapter
    }

    companion object {
        const val TAB_SPACE_DP = 4
    }
}
