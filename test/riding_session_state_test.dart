import 'package:flutter_test/flutter_test.dart';
import 'package:ridelink/features/riding_session/domain/entities/riding_session_state.dart';

void main() {
  test('copyWith yalnızca seçilen alanları değiştirir', () {
    const initial = RidingSessionState(autoMatch: false, sentPackets: 4);
    final updated = initial.copyWith(autoMatch: true);
    expect(updated.autoMatch, isTrue);
    expect(updated.sentPackets, 4);
  });
}
