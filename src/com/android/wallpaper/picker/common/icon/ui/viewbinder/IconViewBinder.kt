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
 *
 */

package com.android.wallpaper.picker.common.icon.ui.viewbinder

import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text

object IconViewBinder {
    fun bind(
        view: ImageView,
        viewModel: Icon,
    ) {
        when (viewModel) {
            is Icon.Resource -> {
                val drawable =
                    AppCompatResources.getDrawable(view.context.applicationContext, viewModel.res)
                view.setImageDrawable(drawable)
            }
            is Icon.Loaded -> view.setImageDrawable(viewModel.drawable)
        }

        view.contentDescription =
            when (viewModel.contentDescription) {
                is Text.Resource ->
                    view.context.getString((viewModel.contentDescription as Text.Resource).res)
                is Text.Loaded -> (viewModel.contentDescription as Text.Loaded).text
                null -> null
            }
    }
}
