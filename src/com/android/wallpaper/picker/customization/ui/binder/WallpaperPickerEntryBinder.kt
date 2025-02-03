/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui.binder

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.android.wallpaper.picker.customization.ui.view.WallpaperPickerEntry
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel.NavigationEvent.NavigateToPreviewScreen
import com.android.wallpaper.picker.data.WallpaperModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import kotlinx.coroutines.launch

object WallpaperPickerEntryBinder {

    fun bind(
        view: WallpaperPickerEntry,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
    ) {

        bindWallpaperCarousel(
            wallpaperCarousel = view.wallpaperCarousel,
            viewModel = viewModel.customizationOptionsViewModel.wallpaperCarouselViewModel,
            lifecycleOwner = lifecycleOwner,
            navigateToPreviewScreen = navigateToPreviewScreen,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedPreviewScreen.collect { previewScreen ->
                    view.collapsedButton.setOnClickListener {
                        navigateToWallpaperCategoriesScreen.invoke(previewScreen)
                    }
                    view.moreWallpapersButton.setOnClickListener {
                        navigateToWallpaperCategoriesScreen.invoke(previewScreen)
                    }
                }
            }
        }

        bindButtonTextColorUpdate(
            buttonText = view.moreWallpapersButton,
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            lifecycleOwner = lifecycleOwner,
        )

        bindButtonTextColorUpdate(
            buttonText = view.collapsedButton,
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            lifecycleOwner = lifecycleOwner,
        )
    }

    private fun bindButtonTextColorUpdate(
        buttonText: TextView,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        ColorUpdateBinder.bind(
            setColor = { color ->
                buttonText.apply {
                    setTextColor(color)
                    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
                }
            },
            color = colorUpdateViewModel.colorPrimary,
            shouldAnimate = {
                viewModel.selectedPreviewScreen.value == Screen.LOCK_SCREEN &&
                    viewModel.customizationOptionsViewModel.selectedOption.value == null
            },
            lifecycleOwner = lifecycleOwner,
        )
    }

    private fun bindWallpaperCarousel(
        wallpaperCarousel: RecyclerView,
        viewModel: WallpaperCarouselViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.wallpaperCarouselItems.collect {
                        wallpaperCarousel.apply {
                            adapter = CuratedPhotosAdapter(it)
                            layoutManager = CarouselLayoutManager()
                        }
                        if (wallpaperCarousel.onFlingListener == null) {
                            CarouselSnapHelper().attachToRecyclerView(wallpaperCarousel)
                        }
                    }
                }

                launch {
                    viewModel.navigationEvents.collect { navigationEvent ->
                        when (navigationEvent) {
                            is NavigateToPreviewScreen -> {
                                navigateToPreviewScreen?.invoke(navigationEvent.wallpaperModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
