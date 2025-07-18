/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.ui.view.transformer.PreviewPagerPageTransformer
import com.android.wallpaper.picker.preview.ui.view.adapters.SinglePreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.view.pagetransformers.PreviewCardPageTransformer
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds single preview home screen and lock screen tabs view pager. */
object PreviewPagerBinder {

    @SuppressLint("WrongConstant")
    fun bind(
        applicationContext: Context,
        mainScope: CoroutineScope,
        viewLifecycleOwner: LifecycleOwner,
        previewsViewPager: ViewPager2,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        previewDisplaySize: Point,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        navigate: (View) -> Unit,
    ) {
        previewsViewPager.apply {
            adapter = SinglePreviewPagerAdapter { viewHolder, position ->
                PreviewTooltipBinder.bindSmallPreviewTooltip(
                    tooltipStub =
                        viewHolder.itemView.requireViewById(R.id.small_preview_tooltip_stub),
                    viewModel = wallpaperPreviewViewModel.smallTooltipViewModel,
                    lifecycleOwner = viewLifecycleOwner,
                )

                SmallPreviewBinder.bind(
                    applicationContext = applicationContext,
                    view = viewHolder.itemView.requireViewById(R.id.preview),
                    viewModel = wallpaperPreviewViewModel,
                    screen = wallpaperPreviewViewModel.smallPreviewTabs[position],
                    displaySize = previewDisplaySize,
                    deviceDisplayType = DeviceDisplayType.SINGLE,
                    mainScope = mainScope,
                    viewLifecycleOwner = viewLifecycleOwner,
                    currentNavDestId = R.id.smallPreviewFragment,
                    transition = transition,
                    transitionConfig = transitionConfig,
                    isFirstBindingDeferred = isFirstBindingDeferred,
                    wallpaperConnectionUtils = wallpaperConnectionUtils,
                    navigate = navigate,
                )
            }
            offscreenPageLimit = SinglePreviewPagerAdapter.PREVIEW_PAGER_ITEM_COUNT
            // the over scroll animation needs to be disabled for the RecyclerView that is contained
            // in the ViewPager2 rather than the ViewPager2 itself
            val child: View = getChildAt(0)
            if (child is RecyclerView) {
                child.overScrollMode = View.OVER_SCROLL_NEVER
                // Remove clip children to enable child card view to display fully during scaling
                // shared element transition.
                child.clipChildren = false
            }

            // When pager's height changes, request transform to recalculate the preview offset
            // to make sure correct space between the previews.
            // TODO (b/348462236): figure out how to scale surface view content with layout change
            addOnLayoutChangeListener { view, _, _, _, _, _, topWas, _, bottomWas ->
                val isHeightChanged = (bottomWas - topWas) != view.height
                if (isHeightChanged) {
                    requestTransform()
                }
            }
        }

        // Only when pager is laid out, we can get the width and set the preview's offset correctly
        previewsViewPager.doOnLayout {
            val pageTransformer =
                if (BaseFlags.get().isNewPickerUi()) PreviewPagerPageTransformer(previewDisplaySize)
                else PreviewCardPageTransformer(previewDisplaySize)
            (it as ViewPager2).setPageTransformer(pageTransformer)
        }

        // Wrap in doOnPreDraw for emoji wallpaper creation case, to make sure recycler view with
        // previews have finished layout before calling registerOnPageChangeCallback and
        // setCurrentItem.
        // TODO (b/339679893): investigate to see if there is a better solution
        previewsViewPager.doOnPreDraw {
            previewsViewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        wallpaperPreviewViewModel.setSmallPreviewSelectedTabIndex(position)
                    }
                }
            )

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    wallpaperPreviewViewModel.smallPreviewSelectedTabIndex.collect {
                        if (previewsViewPager.currentItem != it) {
                            previewsViewPager.setCurrentItem(it, /* smoothScroll= */ true)
                        }
                    }
                }
            }
        }
    }
}
