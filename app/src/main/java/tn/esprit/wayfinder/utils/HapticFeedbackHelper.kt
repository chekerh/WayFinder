package tn.esprit.wayfinder.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

object HapticFeedbackHelper {
    /**
     * Trigger haptic feedback for button press
     */
    fun triggerButtonPress(context: Context) {
        triggerHaptic(context, HapticType.BUTTON_PRESS)
    }

    /**
     * Trigger haptic feedback for card selection
     */
    fun triggerCardSelection(context: Context) {
        triggerHaptic(context, HapticType.CARD_SELECTION)
    }

    /**
     * Trigger haptic feedback for achievement unlock
     */
    fun triggerAchievement(context: Context) {
        triggerHaptic(context, HapticType.ACHIEVEMENT)
    }

    /**
     * Trigger haptic feedback for swipe action
     */
    fun triggerSwipe(context: Context) {
        triggerHaptic(context, HapticType.SWIPE)
    }

    /**
     * Trigger haptic feedback for success action
     */
    fun triggerSuccess(context: Context) {
        triggerHaptic(context, HapticType.SUCCESS)
    }

    private fun triggerHaptic(context: Context, type: HapticType) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HapticType.BUTTON_PRESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticType.CARD_SELECTION -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticType.ACHIEVEMENT -> VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 100, 50, 100, 50),
                    -1
                )
                HapticType.SWIPE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticType.SUCCESS -> VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 50, 30),
                    -1
                )
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private enum class HapticType {
        BUTTON_PRESS,
        CARD_SELECTION,
        ACHIEVEMENT,
        SWIPE,
        SUCCESS
    }
}

