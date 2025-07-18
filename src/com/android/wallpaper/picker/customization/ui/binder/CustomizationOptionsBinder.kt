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
import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2

interface CustomizationOptionsBinder {

    /**
     * @param navigateToWallpaperCategoriesScreen This is a callback that should be implemented by
     *   the hosting Fragment or Activity. This callback should navigate to the wallpaper categories
     *   screen. The input [Screen] of this callback indicate the entrypoint to the wallpaper
     *   categories screen.
     */
    fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
        navigateToPackThemeActivity: () -> Unit,
    )

    fun bindClockPreview(
        context: Context,
        clockHostView: View,
        clockFaceClickDelegateView: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    )

    /** @param activity The activity is used for building the dialog. */
    fun bindDiscardChangesDialog(
        customizationOptionsViewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
    )
}
