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
package com.saggitt.omega.gestures

import android.annotation.SuppressLint
import android.view.MotionEvent
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.touch.SingleAxisSwipeDetector
import com.android.launcher3.util.TouchController
import com.saggitt.omega.OmegaLauncher
import com.saggitt.omega.gestures.handlers.VerticalSwipeGestureHandler
import java.lang.Math.abs
import java.lang.reflect.InvocationTargetException


class VerticalSwipeGestureController(private val launcher: Launcher) : TouchController, SingleAxisSwipeDetector.Listener {

    enum class GestureState {
        Locked,
        Free,
        NotificationOpened,
        NotificationClosed,
        Triggered
    }

    private val triggerVelocity = 2.25f
    private val notificationsCloseVelocity = 0.35f

    private val controller by lazy { OmegaLauncher.getLauncher(launcher).gestureController }
    private val gesture by lazy { controller.verticalSwipeGesture }
    private val detector by lazy { SingleAxisSwipeDetector(launcher, this, SingleAxisSwipeDetector.VERTICAL) }
    private var noIntercept = false

    private var swipeUpOverride: GestureHandler? = null
    private val hasSwipeUpOverride get() = swipeUpOverride != null
    private var state = GestureState.Free
    private var downTime = 0L
    private var downSent = false
    private var pointerCount = 0

    private var overrideDragging = false

    private var currentMillis = 0L
    private var currentVelocity = 0f
    private var currentDisplacement = 0f

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        downTime = ev.downTime
        val isDown = ev.actionMasked == MotionEvent.ACTION_DOWN
        val overrideAppeared = !hasSwipeUpOverride && controller.getSwipeUpOverride(ev.downTime) != null
        if (isDown || overrideAppeared) {
            swipeUpOverride = if (isDown) {
                downSent = false
                null
            } else {
                controller.getSwipeUpOverride(ev.downTime)
            }
            noIntercept = !canInterceptTouch() && !hasSwipeUpOverride
            if (noIntercept) {
                return false
            }
            detector.setDetectableScrollConditions(getSwipeDirection(ev), false)
        }
        if (noIntercept) {
            return false
        }
        val action = ev.action
        if (!isDown && !downSent) {
            ev.action = MotionEvent.ACTION_DOWN
        }
        downSent = true
        onControllerTouchEvent(ev)
        ev.action = action
        return detector.isDraggingOrSettling
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        pointerCount = ev.pointerCount
        return detector.onTouchEvent(ev)
    }

    private fun canInterceptTouch(): Boolean {
        return AbstractFloatingView.getTopOpenView(launcher) == null &&
                launcher.isInState(LauncherState.NORMAL)
    }

    private fun isOverHotseat(ev: MotionEvent): Boolean {
        val dp = launcher.deviceProfile
        val hotseatHeight = dp.hotseatBarSizePx + dp.insets.bottom
        return ev.y >= launcher.dragLayer.height - hotseatHeight
    }

    private fun getSwipeDirection(ev: MotionEvent): Int {
        return when {
            controller.getSwipeUpOverride(ev.downTime) != null -> {
                if (canInterceptTouch())
                    SingleAxisSwipeDetector.DIRECTION_BOTH
                else
                    SingleAxisSwipeDetector.DIRECTION_POSITIVE
            }
            gesture.customSwipeUp && !isOverHotseat(ev) -> SingleAxisSwipeDetector.DIRECTION_BOTH
            gesture.customDockSwipeUp && isOverHotseat(ev) -> SingleAxisSwipeDetector.DIRECTION_BOTH
            else -> SingleAxisSwipeDetector.DIRECTION_NEGATIVE
        }
    }

    override fun onDragStart(start: Boolean, velocity: Float) {
        state = GestureState.Free
        (swipeUpOverride as? VerticalSwipeGestureHandler)?.onDragStart(start)
        overrideDragging = true
    }

    override fun onDrag(displacement: Float): Boolean {
        return true
    }

    override fun onDrag(displacement: Float, ev: MotionEvent): Boolean {
        val velocity = computeVelocity(displacement - currentDisplacement, ev.eventTime)
        currentDisplacement = displacement
        if (state != GestureState.Locked) {
            val wasFree = state == GestureState.Free
            if (overrideDragging) {
                (swipeUpOverride as? VerticalSwipeGestureHandler)?.onDrag(displacement, velocity)
            }
            if (gesture.customSwipeDown) {
                if (velocity > triggerVelocity && state == GestureState.Free) {
                    state = GestureState.Triggered
                    gesture.onSwipeDown()
                }
            } else {
                if (velocity > triggerVelocity &&
                        (state == GestureState.Free || state == GestureState.NotificationClosed)) {
                    state = if (openNotificationsOrQuickSettings()) GestureState.NotificationOpened else GestureState.Locked
                } else if (velocity < -notificationsCloseVelocity && state == GestureState.NotificationOpened) {
                    state = if (closeNotifications()) GestureState.NotificationClosed else GestureState.Locked
                }
            }

            if (wasFree && state == GestureState.NotificationOpened) {
                sendOnDragEnd(velocity)
            } else if (velocity < -triggerVelocity && state == GestureState.Free) {
                controller.getSwipeUpOverride(downTime)?.let {
                    state = GestureState.Triggered
                    it.onGestureTrigger(controller)
                } ?: if (gesture.customSwipeUp) {
                    state = GestureState.Triggered
                    gesture.onSwipeUp()
                } else if (gesture.customDockSwipeUp) {
                    state = GestureState.Triggered
                    gesture.onDockSwipeUp()
                }
            }
        }
        return true
    }

    private fun computeVelocity(delta: Float, millis: Long): Float {
        val previousMillis = currentMillis
        currentMillis = millis

        val deltaTimeMillis = (currentMillis - previousMillis).toFloat()
        val velocity = if (deltaTimeMillis > 0) delta / deltaTimeMillis else 0f
        currentVelocity = if (abs(currentVelocity) < 0.001f) {
            velocity
        } else {
            val alpha = computeDampeningFactor(deltaTimeMillis)
            interpolate(currentVelocity, velocity, alpha)
        }
        return currentVelocity
    }

    /**
     * Returns a time-dependent dampening factor using delta time.
     */
    private fun computeDampeningFactor(deltaTime: Float): Float {
        return deltaTime / (SCROLL_VELOCITY_DAMPENING_RC + deltaTime)
    }

    /**
     * Returns the linear interpolation between two values
     */
    private fun interpolate(from: Float, to: Float, alpha: Float): Float {
        return (1.0f - alpha) * from + alpha * to
    }

    override fun onDragEnd(velocity: Float) {
        launcher.workspace.postDelayed(detector::finishedScrolling, 200)
        sendOnDragEnd(velocity)
    }

    private fun sendOnDragEnd(velocity: Float) {
        if (overrideDragging) {
            (swipeUpOverride as? VerticalSwipeGestureHandler)?.onDragEnd(velocity)
            overrideDragging = false
        }
    }

    private fun openNotificationsOrQuickSettings(): Boolean {
        return if (pointerCount > 1) openQuickSettings() else openNotifications()
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun openNotifications(): Boolean {
        return try {
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandNotificationsPanel")
                    .invoke(launcher.getSystemService("statusbar"))
            true
        } catch (ex: ClassNotFoundException) {
            false
        } catch (ex: NoSuchMethodException) {
            false
        } catch (ex: IllegalAccessException) {
            false
        } catch (ex: InvocationTargetException) {
            false
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun openQuickSettings(): Boolean {
        return try {
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandSettingsPanel")
                    .invoke(launcher.getSystemService("statusbar"))
            true
        } catch (ex: ClassNotFoundException) {
            false
        } catch (ex: NoSuchMethodException) {
            false
        } catch (ex: IllegalAccessException) {
            false
        } catch (ex: InvocationTargetException) {
            false
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun closeNotifications(): Boolean {
        return try {
            Class.forName("android.app.StatusBarManager")
                    .getMethod("collapsePanels")
                    .invoke(launcher.getSystemService("statusbar"))
            true
        } catch (ex: ClassNotFoundException) {
            false
        } catch (ex: NoSuchMethodException) {
            false
        } catch (ex: IllegalAccessException) {
            false
        } catch (ex: InvocationTargetException) {
            false
        }

    }

    companion object {

        private const val SCROLL_VELOCITY_DAMPENING_RC = 1000f / (2f * Math.PI.toFloat() * 10f)
    }
}
