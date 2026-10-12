enum NearbyDeviceStatus {
  discovered,
  connecting,
  authenticationRequired,
  connected,
  disconnected,
  failed,
}

class NearbyDevice {
  const NearbyDevice({
    required this.endpointId,
    required this.displayName,
    required this.status,
    required this.lastSeen,
  });

  final String endpointId;
  final String displayName;
  final NearbyDeviceStatus status;
  final DateTime lastSeen;
}
