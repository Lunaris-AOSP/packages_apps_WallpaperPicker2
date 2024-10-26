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
package com.android.wallpaper.picker.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.LifecycleOwner
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope

/** Binds single preview home screen and lock screen tabs view pager. */
object PreviewPagerBinder2 {

    private val pagerItems = linkedSetOf(R.id.lock_preview, R.id.home_preview)

    fun bind(
        applicationContext: Context,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        smallPreview: MotionLayout,
        viewModel: WallpaperPreviewViewModel,
        previewDisplaySize: Point,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        navigate: (View) -> Unit,
    ) {
        val previewPager = smallPreview.requireViewById<MotionLayout>(R.id.preview_pager)
        pagerItems.forEach {
            val container = previewPager.requireViewById<View>(it)
            PreviewTooltipBinder.bindSmallPreviewTooltip(
                tooltipStub = container.requireViewById(R.id.small_preview_tooltip_stub),
                viewModel = viewModel.smallTooltipViewModel,
                lifecycleOwner = lifecycleOwner,
            )

            SmallPreviewBinder.bind(
                applicationContext = applicationContext,
                view = container.requireViewById(R.id.preview),
                smallPreview = smallPreview,
                previewPager = previewPager,
                viewModel = viewModel,
                screen = viewModel.smallPreviewTabs[pagerItems.indexOf(it)],
                displaySize = previewDisplaySize,
                deviceDisplayType = DeviceDisplayType.SINGLE,
                mainScope = mainScope,
                viewLifecycleOwner = lifecycleOwner,
                currentNavDestId = R.id.smallPreviewFragment,
                transition = transition,
                transitionConfig = transitionConfig,
                isFirstBindingDeferred = isFirstBindingDeferred,
                wallpaperConnectionUtils = wallpaperConnectionUtils,
                navigate = navigate,
            )
        }
    }
}
