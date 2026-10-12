package com.lideatech.ridelink.bridge

import android.content.Intent
import androidx.core.content.ContextCompat
import com.lideatech.ridelink.core.RideLinkRuntime
import com.lideatech.ridelink.service.RidingForegroundService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class RideLinkMethodHandler(
    private val activity: FlutterActivity,
    private val events: RideLinkEventHandler,
) : MethodChannel.MethodCallHandler {
    private val runtime get() = RideLinkRuntime.initialize(activity.applicationContext)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "getStatus" -> result.success(runtime.status())
                "startRidingMode" -> {
                    val debugPhoneAudio = call.argument<Boolean>("debugPhoneAudio") ?: false
                    runtime.validateStart(debugPhoneAudio)
                    ContextCompat.startForegroundService(
                        activity,
                        Intent(activity, RidingForegroundService::class.java).apply {
                            action = RidingForegroundService.ACTION_START
                            putExtra(RidingForegroundService.EXTRA_DEBUG_PHONE_AUDIO, debugPhoneAudio)
                        },
                    )
                    result.success(null)
                }
                "stopRidingMode" -> {
                    activity.startService(
                        Intent(activity, RidingForegroundService::class.java).apply {
                            action = RidingForegroundService.ACTION_STOP
                        },
                    )
                    result.success(null)
                }
                "setAutoMatch" -> {
                    runtime.setAutoMatch(call.argument<Boolean>("enabled") ?: false)
                    result.success(null)
                }
                "setMuted" -> {
                    runtime.setMuted(call.argument<Boolean>("muted") ?: false)
                    result.success(null)
                }
                "disconnect" -> {
                    runtime.disconnect(userInitiated = true)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        } catch (error: SecurityException) {
            result.error("PERMISSION_DENIED", error.message, null)
        } catch (error: IllegalStateException) {
            result.error(error.message ?: "INVALID_STATE", error.localizedMessage, null)
        } catch (error: Throwable) {
            events.emit(mapOf("type" to "error", "message" to (error.localizedMessage ?: "Native hata")))
            result.error("NATIVE_ERROR", error.localizedMessage, null)
        }
    }

    companion object {
        const val CHANNEL = "com.lideatech.ridelink/methods"
    }
}
