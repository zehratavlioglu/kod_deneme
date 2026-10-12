import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ridelink/core/platform/ridelink_platform.dart';
import 'package:ridelink/features/riding_session/presentation/providers/riding_session_controller.dart';
import 'package:ridelink/features/riding_session/presentation/screens/home_screen.dart';

class _FakePlatform extends RideLinkPlatform {
  const _FakePlatform();

  @override
  Stream<Map<String, Object?>> get events => const Stream.empty();

  @override
  Future<Map<String, Object?>> status() async => {
    'bluetoothEnabled': true,
    'wifiEnabled': true,
    'microphoneGranted': true,
    'nearbyGranted': true,
    'playServicesAvailable': true,
    'audioRoute': 'Telefon',
  };
}

void main() {
  testWidgets('ana ekran sistem ve sürüş kontrollerini gösterir', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          rideLinkPlatformProvider.overrideWithValue(const _FakePlatform()),
        ],
        child: const MaterialApp(home: HomeScreen()),
      ),
    );
    await tester.pump();
    expect(find.text('Sistem durumu'), findsOneWidget);
    expect(find.text('İnterkom'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Sürüş Modunu Başlat'),
      400,
    );
    expect(find.text('Sürüş Modunu Başlat'), findsOneWidget);
  });
}
