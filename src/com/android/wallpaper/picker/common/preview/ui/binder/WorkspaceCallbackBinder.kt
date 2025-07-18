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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.os.Bundle
import android.os.Message
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel

/**
 * This interface takes care the communication with the remote view from an external app. We send
 * data through [Message].
 */
interface WorkspaceCallbackBinder {

    suspend fun bind(
        workspaceCallback: Message,
        viewModel: CustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        screen: Screen,
        clockViewFactory: ClockViewFactory,
    )

    companion object {
        fun Message.sendMessage(what: Int, data: Bundle) {
            this.replyTo.send(
                Message().apply {
                    this.what = what
                    this.data = data
                }
            )
        }
    }
}
