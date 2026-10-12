package com.lideatech.ridelink.bridge

import android.content.Context
import com.lideatech.ridelink.core.RideLinkRuntime
import io.flutter.plugin.common.EventChannel

class RideLinkEventHandler(private val context: Context) : EventChannel.StreamHandler {
    private var sink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        sink = events
        RideLinkRuntime.initialize(context).setEventListener(::emit)
        emit(RideLinkRuntime.initialize(context).status() + mapOf("type" to "status"))
    }

    override fun onCancel(arguments: Any?) {
        sink = null
        RideLinkRuntime.initialize(context).setEventListener(null)
    }

    fun emit(event: Map<String, Any?>) {
        sink?.success(event)
    }

    companion object {
        const val CHANNEL = "com.lideatech.ridelink/events"
    }
}
