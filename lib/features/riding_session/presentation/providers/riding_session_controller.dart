import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';

import '../../../../core/platform/ridelink_platform.dart';
import '../../../nearby/domain/entities/nearby_device.dart';
import '../../domain/entities/riding_session_state.dart';

final rideLinkPlatformProvider = Provider((ref) => const RideLinkPlatform());
final ridingSessionProvider =
    NotifierProvider<RidingSessionController, RidingSessionState>(
      RidingSessionController.new,
    );

class RidingSessionController extends Notifier<RidingSessionState> {
  StreamSubscription<Map<String, Object?>>? _subscription;
  RideLinkPlatform get _platform => ref.read(rideLinkPlatformProvider);

  @override
  RidingSessionState build() {
    _subscription = _platform.events.listen(_onEvent, onError: _onStreamError);
    ref.onDispose(() => _subscription?.cancel());
    Future<void>.microtask(refresh);
    return const RidingSessionState();
  }

  Future<void> refresh() async {
    try {
      _applyStatus(await _platform.status());
    } catch (error) {
      _recordError(_friendlyError(error));
    }
  }

  Future<void> start() async {
    state = state.copyWith(isLoading: true, clearError: true);
    final permissions = await [
      Permission.microphone,
      // Nearby Connections requires location permission through Android 11.
      // The manifest caps it at API 31, so modern Android versions do not
      // receive an unnecessary location permission prompt.
      Permission.locationWhenInUse,
      Permission.bluetoothScan,
      Permission.bluetoothAdvertise,
      Permission.bluetoothConnect,
      Permission.nearbyWifiDevices,
      Permission.notification,
    ].request();
    if (permissions[Permission.microphone] != PermissionStatus.granted) {
      state = state.copyWith(
        isLoading: false,
        error: 'Mikrofon izni olmadan sürüş modu başlatılamaz.',
      );
      return;
    }
    try {
      await _platform.startRidingMode(debugPhoneAudio: state.debugPhoneAudio);
      state = state.copyWith(isLoading: false, isRiding: true);
      await refresh();
    } catch (error) {
      state = state.copyWith(isLoading: false, error: _friendlyError(error));
    }
  }

  Future<void> stop() async {
    final previous = state;
    state = state.copyWith(
      isRiding: false,
      connectionState: 'idle',
      clearPeerName: true,
    );
    try {
      await _platform.stopRidingMode();
      state = state.copyWith(clearError: true);
    } catch (error) {
      state = previous.copyWith(error: _friendlyError(error));
    }
  }

  Future<void> setAutoMatch(bool value) async {
    await _guard(() => _platform.setAutoMatch(value));
    state = state.copyWith(autoMatch: value);
  }

  void setDebugPhoneAudio(bool value) {
    if (!state.isRiding) state = state.copyWith(debugPhoneAudio: value);
  }

  Future<void> setMuted(bool value) async {
    await _guard(() => _platform.setMuted(value));
    state = state.copyWith(isMuted: value);
  }

  Future<void> disconnect() async {
    final previous = state;
    state = state.copyWith(
      connectionState: 'disconnected',
      clearPeerName: true,
      clearError: true,
    );
    try {
      await _platform.disconnect();
    } catch (error) {
      state = previous.copyWith(error: _friendlyError(error));
    }
  }

  Future<void> _guard(Future<void> Function() action) async {
    try {
      await action();
      state = state.copyWith(clearError: true);
    } catch (error) {
      _recordError(_friendlyError(error));
    }
  }

  void _onEvent(Map<String, Object?> event) {
    final type = event['type'] as String? ?? 'event';
    final message = event['message'] as String? ?? type;
    final log = <String>[
      '${DateTime.now().toIso8601String()}  $message',
      ...state.events,
    ].take(100).toList(growable: false);
    if (type == 'status') {
      _applyStatus(event, events: log);
    } else {
      state = state.copyWith(
        events: log,
        error: type == 'error' ? message : null,
      );
    }
  }

  void _applyStatus(Map<String, Object?> value, {List<String>? events}) {
    final rawDevices = value['devices'] as List<Object?>? ?? const [];
    state = state.copyWith(
      isRiding: value['isRiding'] as bool? ?? state.isRiding,
      autoMatch: value['autoMatch'] as bool? ?? state.autoMatch,
      bluetoothEnabled:
          value['bluetoothEnabled'] as bool? ?? state.bluetoothEnabled,
      wifiEnabled: value['wifiEnabled'] as bool? ?? state.wifiEnabled,
      microphoneGranted:
          value['microphoneGranted'] as bool? ?? state.microphoneGranted,
      nearbyGranted: value['nearbyGranted'] as bool? ?? state.nearbyGranted,
      playServicesAvailable:
          value['playServicesAvailable'] as bool? ??
          state.playServicesAvailable,
      intercomName: value['intercomName'] as String?,
      clearIntercomName: value['intercomName'] == null,
      audioRoute: value['audioRoute'] as String? ?? state.audioRoute,
      microphoneReady:
          value['microphoneReady'] as bool? ?? state.microphoneReady,
      connectionState:
          value['connectionState'] as String? ?? state.connectionState,
      peerName: value['peerName'] as String?,
      clearPeerName: value['peerName'] == null,
      sentPackets: value['sentPackets'] as int? ?? state.sentPackets,
      receivedPackets:
          value['receivedPackets'] as int? ?? state.receivedPackets,
      lostPackets: value['lostPackets'] as int? ?? state.lostPackets,
      bufferedPackets:
          value['bufferedPackets'] as int? ?? state.bufferedPackets,
      devices: rawDevices
          .whereType<Map>()
          .map((raw) {
            final map = Map<String, Object?>.from(raw);
            return NearbyDevice(
              endpointId: map['endpointId'] as String? ?? '?',
              displayName: map['name'] as String? ?? 'RideLink sürücüsü',
              status: NearbyDeviceStatus.values.firstWhere(
                (status) => status.name == map['status'],
                orElse: () => NearbyDeviceStatus.failed,
              ),
              lastSeen: DateTime.fromMillisecondsSinceEpoch(
                map['lastSeen'] as int? ?? 0,
              ),
            );
          })
          .toList(growable: false),
      events: events,
    );
  }

  void _onStreamError(Object error) => _recordError(_friendlyError(error));
  void _recordError(String message) => state = state.copyWith(error: message);

  String _friendlyError(Object error) {
    final text = error.toString();
    if (text.contains('INTERCOM_NOT_CONNECTED')) {
      return 'Bluetooth interkom bulunamadı. Test için telefon sesini seçebilirsiniz.';
    }
    if (text.contains('PERMISSION_DENIED')) {
      return 'Gerekli yakındaki cihaz veya mikrofon izni verilmedi.';
    }
    if (text.contains('NEARBY_UNAVAILABLE')) {
      return 'Google Play services Nearby bu cihazda kullanılamıyor.';
    }
    return 'İşlem tamamlanamadı: $text';
  }
}