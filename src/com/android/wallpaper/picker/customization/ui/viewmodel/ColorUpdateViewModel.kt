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

package com.android.wallpaper.picker.customization.ui.viewmodel

import android.content.Context
import android.content.res.Configuration
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.Style
import com.android.wallpaper.R
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine

@ActivityScoped
class ColorUpdateViewModel @Inject constructor(@ApplicationContext private val context: Context) {
    private val _systemColorsUpdated: MutableSharedFlow<Unit> =
        MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }
    val systemColorsUpdated = _systemColorsUpdated.asSharedFlow()

    private val previewingColorScheme: MutableStateFlow<DynamicScheme?> = MutableStateFlow(null)

    private val _colorPrimary = MutableStateFlow(context.getColor(R.color.system_primary))
    val colorPrimary =
        combine(_colorPrimary, previewingColorScheme) { systemColor, previewScheme ->
            if (previewScheme != null) {
                val dynamicColor = MaterialDynamicColors().primary()
                previewScheme.getArgb(dynamicColor)
            } else systemColor
        }

    private val _colorSecondaryContainer =
        MutableStateFlow(context.getColor(R.color.system_secondary_container))
    val colorSecondaryContainer =
        combine(_colorSecondaryContainer, previewingColorScheme) { systemColor, previewScheme ->
            if (previewScheme != null) {
                val dynamicColor = MaterialDynamicColors().secondaryContainer()
                previewScheme.getArgb(dynamicColor)
            } else systemColor
        }

    private val _colorSurfaceContainer =
        MutableStateFlow(context.getColor(R.color.system_surface_container))
    val colorSurfaceContainer =
        combine(_colorSurfaceContainer, previewingColorScheme) { systemColor, previewScheme ->
            if (previewScheme != null) {
                val dynamicColor = MaterialDynamicColors().surfaceContainer()
                previewScheme.getArgb(dynamicColor)
            } else systemColor
        }

    fun previewColors(colorSeed: Int, style: Style) {
        val isDarkMode =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        previewingColorScheme.value = ColorScheme(colorSeed, isDarkMode, style).materialScheme
    }

    fun resetPreview() {
        previewingColorScheme.value = null
    }

    fun updateColors() {
        _systemColorsUpdated.tryEmit(Unit)
        _colorPrimary.value = context.getColor(R.color.system_primary)
        _colorSecondaryContainer.value = context.getColor(R.color.system_secondary_container)
        _colorSurfaceContainer.value = context.getColor(R.color.system_surface_container)
    }
}
