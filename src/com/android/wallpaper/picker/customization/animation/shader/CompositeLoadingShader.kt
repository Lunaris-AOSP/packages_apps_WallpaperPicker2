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
package com.android.wallpaper.picker.customization.animation.shader

import android.graphics.RuntimeShader
import com.android.systemui.surfaceeffects.shaderutil.ShaderUtilLibrary

/**
 * Shader rendered when images are loading. Contains:
 * - Sparkles
 * - Clouds
 * - Displacement map
 */
class CompositeLoadingShader(type: Type) : RuntimeShader(getShader(type)) {
    // language=AGSL
    companion object {
        private const val UNIFORMS =
            """
            uniform shader in_background;
            uniform shader in_sparkleMask;
            uniform shader in_colorMask;
            uniform half in_alpha;
            layout(color) uniform vec4 in_screenColor;
        """

        private const val SPARKLE_TURBULENCE_SHADER =
            """ vec4 main(vec2 p) {
            half4 bgColor = in_background.eval(p);
            half3 sparkleMask = in_sparkleMask.eval(p).rgb;
            half3 colorMask = in_colorMask.eval(p).rgb;

            float sparkleAlpha = smoothstep(0, 0.75, in_alpha);
            half3 effect = screen(screen(bgColor.rgb, in_screenColor.rgb), colorMask * 0.22)
                            + sparkleMask * sparkleAlpha;
            return mix(bgColor, vec4(effect, 1.), in_alpha);
        }
        """

        private const val SURFACE_TURBULENCE_SHADER =
            """ vec4 main(vec2 p) {
            half4 bgColor = in_background.eval(p);
            half4 colorMask = in_colorMask.eval(p);

            // Apply src_over blend mode
            half4 effect = half4(mix(bgColor.rgb, colorMask.rgb, colorMask.a), 1.0);
            return mix(bgColor, effect, in_alpha);
        }
        """

        enum class Type {
            SPARKLE_TURBULENCE,
            SURFACE_TURBULENCE,
        }

        fun getShader(type: Type): String {
            return when (type) {
                Type.SPARKLE_TURBULENCE ->
                    UNIFORMS + ShaderUtilLibrary.SHADER_LIB + SPARKLE_TURBULENCE_SHADER
                Type.SURFACE_TURBULENCE ->
                    UNIFORMS + ShaderUtilLibrary.SHADER_LIB + SURFACE_TURBULENCE_SHADER
            }
        }
    }

    /** Sets the overall opacity of the effect. */
    fun setAlpha(alpha: Float) {
        setFloatUniform("in_alpha", alpha)
    }

    /** Sets the color that is applied with screen blending on top of the background image. */
    fun setScreenColor(color: Int) {
        setColorUniform("in_screenColor", color)
    }

    /**
     * Sets the sparkle layer. Expected to get the color tinted sparkles turbulence noise shader.
     */
    fun setSparkle(sparkleTurbulenceMask: RuntimeShader) {
        setInputShader("in_sparkleMask", sparkleTurbulenceMask)
    }

    /** Sets the color layer. Expected to get the color tinted turbulence noise shader. */
    fun setColorTurbulenceMask(colorTurbulenceMask: RuntimeShader) {
        setInputShader("in_colorMask", colorTurbulenceMask)
    }
}
