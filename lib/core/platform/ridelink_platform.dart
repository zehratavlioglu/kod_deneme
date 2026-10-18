import 'package:flutter/services.dart';

class RideLinkPlatform {
  const RideLinkPlatform();

  static const _methods = MethodChannel('com.lideatech.ridelink/methods');
  static const _events = EventChannel('com.lideatech.ridelink/events');

  Stream<Map<String, Object?>> get events => _events
      .receiveBroadcastStream()
      .where((event) => event is Map)
      .map((event) => Map<String, Object?>.from(event as Map));

  Future<Map<String, Object?>> status() async =>
      await _methods.invokeMapMethod<String, Object?>('getStatus') ?? const {};

  Future<void> startRidingMode({required bool debugPhoneAudio}) =>
      _methods.invokeMethod<void>('startRidingMode', {
        'debugPhoneAudio': debugPhoneAudio,
      });
  Future<void> stopRidingMode() =>
      _methods.invokeMethod<void>('stopRidingMode');
  Future<void> setAutoMatch(bool enabled) =>
      _methods.invokeMethod<void>('setAutoMatch', {'enabled': enabled});
  Future<void> setMuted(bool muted) =>
      _methods.invokeMethod<void>('setMuted', {'muted': muted});
  Future<void> disconnect() => _methods.invokeMethod<void>('disconnect');
}