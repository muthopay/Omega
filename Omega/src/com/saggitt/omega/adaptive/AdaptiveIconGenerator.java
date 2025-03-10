/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.adaptive;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;

import com.android.launcher3.AdaptiveIconCompat;
import com.android.launcher3.Utilities;
import com.android.launcher3.icons.ColorExtractor;
import com.android.launcher3.icons.FixedScaleDrawable;
import com.android.launcher3.icons.IconNormalizer;
import com.android.launcher3.icons.LauncherIcons;
import com.saggitt.omega.OmegaPreferences;
import com.saggitt.omega.icons.CustomIconProvider;

// TODO: Make this thing async somehow (maybe using some drawable wrappers?)
public class AdaptiveIconGenerator {

    // Average number of derived colors (based on averages with ~100 icons and performance testing)
    private static final int NUMBER_OF_COLORS_GUESSTIMATION = 45;

    // Found after some experimenting, might be improved with some more testing
    private static final float FULL_BLEED_ICON_SCALE = 1.44f;
    // Found after some experimenting, might be improved with some more testing
    private static final float NO_MIXIN_ICON_SCALE = 1.40f;
    // Icons with less than 5 colors are considered as "single color"
    private static final int SINGLE_COLOR_LIMIT = 5;
    // Minimal alpha to be considered opaque
    private static final int MIN_VISIBLE_ALPHA = 0xEF;

    private final Context context;
    private Drawable icon;
    private final Drawable roundIcon;

    private final boolean extractColor;
    private final boolean treatWhite;

    private boolean ranLoop;
    private final boolean shouldWrap;
    private int backgroundColor = Color.WHITE;
    private boolean isFullBleed;
    private boolean noMixinNeeded;
    private boolean fullBleedChecked;
    private boolean matchesMaskShape;
    private boolean isBackgroundWhite;
    private float scale;
    private int height;
    private float aHeight;
    private int width;
    private float aWidth;
    private Drawable result;

    private AdaptiveIconCompat tmp;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public AdaptiveIconGenerator(Context context, @NonNull Drawable icon, @Nullable Drawable roundIcon) {
        this.context = context;
        this.icon = AdaptiveIconCompat.wrap(icon);
        this.roundIcon = AdaptiveIconCompat.wrapNullable(roundIcon);
        OmegaPreferences prefs = Utilities.getOmegaPrefs(context);
        shouldWrap = prefs.getEnableLegacyTreatment();
        extractColor = shouldWrap && prefs.getColorizedLegacyTreatment();
        treatWhite = extractColor && prefs.getEnableWhiteOnlyTreatment();
    }

    private void loop() {
        if (Utilities.ATLEAST_OREO && shouldWrap) {
            if (roundIcon != null && roundIcon instanceof AdaptiveIconCompat) {
                icon = roundIcon;
            }
            Drawable extractee = icon;
            if (extractee instanceof AdaptiveIconCompat) {
                if (!treatWhite) {
                    onExitLoop();
                    return;
                }
                AdaptiveIconCompat aid = (AdaptiveIconCompat) extractee;
                // we still check this separately as this is the only information we need from the background
                if (!ColorExtractor.isSingleColor(aid.getBackground(), Color.WHITE)) {
                    onExitLoop();
                    return;
                }
                isBackgroundWhite = true;
                extractee = aid.getForeground();
            }

            if (extractee == null) {
                Log.e("AdaptiveIconGenerator", "extractee is null, skipping.");
                onExitLoop();
                return;
            }

            LauncherIcons li = LauncherIcons.obtain(context);
            IconNormalizer normalizer = li.getNormalizer();
            li.recycle();

            boolean[] outShape = new boolean[1];
            RectF bounds = new RectF();

            initTmpIfNeeded();
            scale = normalizer.getScale(extractee, bounds, tmp.getIconMask(), outShape);
            matchesMaskShape = outShape[0];

            if (extractee instanceof ColorDrawable) {
                isFullBleed = true;
                fullBleedChecked = true;
            }

            width = extractee.getIntrinsicWidth();
            height = extractee.getIntrinsicHeight();
            aWidth = width * (1 - (bounds.left + bounds.right));
            aHeight = height * (1 - (bounds.top + bounds.bottom));

            // Check if the icon is squareish
            final float ratio = aHeight / aWidth;
            boolean isSquareish = 0.999 < ratio && ratio < 1.0001;
            boolean almostSquarish = isSquareish || (0.97 < ratio && ratio < 1.005);
            if (!isSquareish) {
                isFullBleed = false;
                fullBleedChecked = true;
            }

            final Bitmap bitmap = Utilities.drawableToBitmap(extractee);
            if (bitmap == null) {
                onExitLoop();
                return;
            }

            if (!bitmap.hasAlpha()) {
                isFullBleed = true;
                fullBleedChecked = true;
            }

            final int size = height * width;
            SparseIntArray rgbScoreHistogram = new SparseIntArray(NUMBER_OF_COLORS_GUESSTIMATION);
            final int[] pixels = new int[size];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            /*
             *   Calculate the number of padding pixels around the actual icon (i)
             *   +----------------+
             *   |      top       |
             *   +---+--------+---+
             *   |   |        |   |
             *   | l |    i   | r |
             *   |   |        |   |
             *   +---+--------+---+
             *   |     bottom     |
             *   +----------------+
             */
            float adjHeight = height - bounds.top - bounds.bottom;
            float l = bounds.left * width * adjHeight;
            float top = bounds.top * height * width;
            float r = bounds.right * width * adjHeight;
            float bottom = bounds.bottom * height * width;
            int addPixels = Math.round(l + top + r + bottom);

            // Any icon with less than 10% transparent pixels (padding excluded) is considered "full-bleed-ish"
            final int maxTransparent = (int) (round(size * .10) + addPixels);
            // Any icon with less than 27% transparent pixels (padding excluded) doesn't need a color mix-in
            final int noMixinScore = (int) (round(size * .27) + addPixels);

            int highScore = 0;
            int bestRGB = 0;
            int transparentScore = 0;
            for (int pixel : pixels) {
                int alpha = 0xFF & (pixel >> 24);
                if (alpha < MIN_VISIBLE_ALPHA) {
                    // Drop mostly-transparent pixels.
                    transparentScore++;
                    if (transparentScore > maxTransparent) {
                        isFullBleed = false;
                        fullBleedChecked = true;
                        if (!extractColor && transparentScore > noMixinScore) {
                            break;
                        }
                    }
                    continue;
                }
                // Reduce color complexity.
                int rgb = ColorExtractor.posterize(pixel);
                if (rgb < 0) {
                    // Defensively avoid array bounds violations.
                    continue;
                }
                int currentScore = rgbScoreHistogram.get(rgb) + 1;
                rgbScoreHistogram.append(rgb, currentScore);
                if (currentScore > highScore) {
                    highScore = currentScore;
                    bestRGB = rgb;
                }
            }

            // add back the alpha channel
            bestRGB |= 0xff << 24;

            // not yet checked = not set to false = has to be full bleed, isBackgroundWhite = true = is adaptive
            isFullBleed |= !fullBleedChecked && !isBackgroundWhite;

            // return early if a mix-in isnt needed
            noMixinNeeded = !isFullBleed && !isBackgroundWhite && almostSquarish && transparentScore <= noMixinScore;
            if (isFullBleed || noMixinNeeded) {
                backgroundColor = bestRGB;
                onExitLoop();
                return;
            }

            if (!extractColor) {
                backgroundColor = Color.WHITE;
                onExitLoop();
                return;
            }

            // "single color"
            final int numColors = rgbScoreHistogram.size();
            boolean singleColor = numColors <= SINGLE_COLOR_LIMIT;

            // Convert to HSL to get the lightness and adjust the color
            final float[] hsl = new float[3];
            ColorUtils.colorToHSL(bestRGB, hsl);
            float lightness = hsl[2];

            boolean light = lightness > .5;
            // Apply dark background to mostly white icons
            boolean veryLight = lightness > .75 && singleColor;
            // Apply light background to mostly dark icons
            boolean veryDark = lightness < .35 && singleColor;

            // Generate pleasant pastel colors for saturated icons
            if (hsl[1] > .5f && lightness > .2) {
                hsl[1] = 1f;
                hsl[2] = .875f;
                backgroundColor = ColorUtils.HSLToColor(hsl);
                onExitLoop();
                return;
            }

            // Adjust color to reach suitable contrast depending on the relationship between the colors
            final int opaqueSize = size - transparentScore;
            final float pxPerColor = opaqueSize / (float) numColors;
            float mixRatio = min(max(pxPerColor / highScore, .15f), .7f);

            // Vary color mix-in based on lightness and amount of colors
            int fill = (light && !veryLight) || veryDark ? 0xFFFFFFFF : 0xFF333333;
            backgroundColor = ColorUtils.blendARGB(bestRGB, fill, mixRatio);
        }
        onExitLoop();
    }

    private void onExitLoop() {
        ranLoop = true;
        result = genResult();
    }

    private Drawable genResult() {
        if (!Utilities.ATLEAST_OREO || !shouldWrap) {
            if (roundIcon != null) {
                if (icon instanceof AdaptiveIconCompat &&
                        !(roundIcon instanceof AdaptiveIconCompat)) {
                    return icon;
                }
                return roundIcon;
            }
            return icon;
        }
        if (icon instanceof AdaptiveIconCompat) {
            if (!treatWhite || !isBackgroundWhite) {
                return icon;
            }
            if (((AdaptiveIconCompat) icon).getBackground() instanceof ColorDrawable) {
                AdaptiveIconCompat mutIcon = (AdaptiveIconCompat) icon.mutate();
                ((ColorDrawable) mutIcon.getBackground()).setColor(backgroundColor);
                return mutIcon;
            }
            return new AdaptiveIconCompat(new ColorDrawable(backgroundColor), ((AdaptiveIconCompat) icon).getForeground());
        }
        initTmpIfNeeded();
        ((FixedScaleDrawable) tmp.getForeground()).setDrawable(icon);
        if (matchesMaskShape || isFullBleed || noMixinNeeded) {
            float scale;
            if (noMixinNeeded) {
                float upScale = min(width / aWidth, height / aHeight);
                scale = NO_MIXIN_ICON_SCALE * upScale;
            } else {
                float upScale = max(width / aWidth, height / aHeight);
                scale = FULL_BLEED_ICON_SCALE * upScale;
            }
            ((FixedScaleDrawable) tmp.getForeground()).setScale(scale);
        } else {
            ((FixedScaleDrawable) tmp.getForeground()).setScale(scale);
        }
        ((ColorDrawable) tmp.getBackground()).setColor(backgroundColor);
        return tmp;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initTmpIfNeeded() {
        if (tmp == null) {
            tmp = CustomIconProvider.getAdaptiveIconDrawableWrapper(context);
            tmp.setBounds(0, 0, 1, 1);
        }
    }

    public Drawable getResult() {
        if (!ranLoop) {
            loop();
        }
        return result;
    }
}