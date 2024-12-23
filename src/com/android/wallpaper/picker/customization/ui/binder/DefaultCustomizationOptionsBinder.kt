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

package com.android.wallpaper.picker.customization.ui.binder

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.DefaultCustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class DefaultCustomizationOptionsBinder @Inject constructor() : CustomizationOptionsBinder {

    override fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
    ) {
        val lockWallpaperEntry =
            lockScreenCustomizationOptionEntries
                .find {
                    it.first ==
                        DefaultCustomizationOptionUtil.DefaultLockCustomizationOption.WALLPAPER
                }
                ?.second ?: return

        val homeWallpaperEntry =
            homeScreenCustomizationOptionEntries
                .find {
                    it.first ==
                        DefaultCustomizationOptionUtil.DefaultHomeCustomizationOption.WALLPAPER
                }
                ?.second ?: return

        val moreWallpapersLock: TextView = lockWallpaperEntry.requireViewById(R.id.more_wallpapers)
        val moreWallpapersHome: TextView = homeWallpaperEntry.requireViewById(R.id.more_wallpapers)

        bindWallpaperCarousel(
            wallpaperCarouselLock = lockWallpaperEntry.requireViewById(R.id.wallpaper_carousel),
            wallpaperCarouselHome = homeWallpaperEntry.requireViewById(R.id.wallpaper_carousel),
            viewModel = viewModel.customizationOptionsViewModel.wallpaperCarouselViewModel,
            lifecycleOwner = lifecycleOwner,
        )

        moreWallpapersLock.setOnClickListener {
            navigateToWallpaperCategoriesScreen.invoke(Screen.LOCK_SCREEN)
        }

        moreWallpapersHome.setOnClickListener {
            navigateToWallpaperCategoriesScreen.invoke(Screen.HOME_SCREEN)
        }

        ColorUpdateBinder.bind(
            setColor = { color ->
                moreWallpapersLock.apply {
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

        ColorUpdateBinder.bind(
            setColor = { color ->
                moreWallpapersHome.apply {
                    setTextColor(color)
                    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
                }
            },
            color = colorUpdateViewModel.colorPrimary,
            shouldAnimate = {
                viewModel.selectedPreviewScreen.value == Screen.HOME_SCREEN &&
                    viewModel.customizationOptionsViewModel.selectedOption.value == null
            },
            lifecycleOwner = lifecycleOwner,
        )
    }

    private fun bindWallpaperCarousel(
        wallpaperCarouselLock: RecyclerView,
        wallpaperCarouselHome: RecyclerView,
        viewModel: WallpaperCarouselViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.wallpaperCarouselItems.collect {
                        wallpaperCarouselLock.apply {
                            adapter = CuratedPhotosAdapter(it)
                            layoutManager = CarouselLayoutManager()
                        }
                        if (wallpaperCarouselLock.onFlingListener == null) {
                            CarouselSnapHelper().attachToRecyclerView(wallpaperCarouselLock)
                        }

                        wallpaperCarouselHome.apply {
                            adapter = CuratedPhotosAdapter(it)
                            layoutManager = CarouselLayoutManager()
                        }
                        if (wallpaperCarouselHome.onFlingListener == null) {
                            CarouselSnapHelper().attachToRecyclerView(wallpaperCarouselHome)
                        }
                    }
                }
            }
        }
    }

    override fun bindClockPreview(
        context: Context,
        clockHostView: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        // Do nothing intended
    }

    override fun bindDiscardChangesDialog(
        customizationOptionsViewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
    ) {
        var discardChangesDialog: AlertDialog? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                customizationOptionsViewModel.discardChangesDialogViewModel.collect { viewModel ->
                    if (viewModel != null) {
                        val (onDismiss, onKeepEditing, onDiscard) = viewModel
                        val dialog =
                            discardChangesDialog
                                ?: AlertDialog.Builder(activity)
                                    .setMessage(R.string.discard_changes_dialog_message)
                                    .setOnDismissListener { onDismiss.invoke() }
                                    .setPositiveButton(
                                        R.string.discard_changes_dialog_button_keep_editing
                                    ) { _, _ ->
                                        onKeepEditing.invoke()
                                    }
                                    .setNegativeButton(
                                        R.string.discard_changes_dialog_button_discard
                                    ) { _, _ ->
                                        onDiscard.invoke()
                                    }
                                    .create()
                                    .also { discardChangesDialog = it }
                        dialog.show()
                    } else {
                        discardChangesDialog?.dismiss()
                        discardChangesDialog = null
                    }
                }
            }
        }
    }
}
