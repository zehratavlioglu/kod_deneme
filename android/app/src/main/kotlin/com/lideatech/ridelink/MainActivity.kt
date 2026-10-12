package com.lideatech.ridelink

import com.lideatech.ridelink.bridge.RideLinkEventHandler
import com.lideatech.ridelink.bridge.RideLinkMethodHandler
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private lateinit var eventHandler: RideLinkEventHandler

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        eventHandler = RideLinkEventHandler(applicationContext)
        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            RideLinkEventHandler.CHANNEL,
        ).setStreamHandler(eventHandler)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            RideLinkMethodHandler.CHANNEL,
        ).setMethodCallHandler(RideLinkMethodHandler(this, eventHandler))
    }
}
