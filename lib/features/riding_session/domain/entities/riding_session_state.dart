import '../../../nearby/domain/entities/nearby_device.dart';

class RidingSessionState {
  const RidingSessionState({
    this.isLoading = false,
    this.isRiding = false,
    this.autoMatch = false,
    this.debugPhoneAudio = false,
    this.isMuted = false,
    this.bluetoothEnabled = false,
    this.wifiEnabled = false,
    this.microphoneGranted = false,
    this.nearbyGranted = false,
    this.playServicesAvailable = false,
    this.intercomName,
    this.audioRoute = 'Bilinmiyor',
    this.microphoneReady = false,
    this.connectionState = 'idle',
    this.peerName,
    this.sentPackets = 0,
    this.receivedPackets = 0,
    this.lostPackets = 0,
    this.bufferedPackets = 0,
    this.devices = const [],
    this.events = const [],
    this.error,
  });

  final bool isLoading;
  final bool isRiding;
  final bool autoMatch;
  final bool debugPhoneAudio;
  final bool isMuted;
  final bool bluetoothEnabled;
  final bool wifiEnabled;
  final bool microphoneGranted;
  final bool nearbyGranted;
  final bool playServicesAvailable;
  final String? intercomName;
  final String audioRoute;
  final bool microphoneReady;
  final String connectionState;
  final String? peerName;
  final int sentPackets;
  final int receivedPackets;
  final int lostPackets;
  final int bufferedPackets;
  final List<NearbyDevice> devices;
  final List<String> events;
  final String? error;

  RidingSessionState copyWith({
    bool? isLoading,
    bool? isRiding,
    bool? autoMatch,
    bool? debugPhoneAudio,
    bool? isMuted,
    bool? bluetoothEnabled,
    bool? wifiEnabled,
    bool? microphoneGranted,
    bool? nearbyGranted,
    bool? playServicesAvailable,
    String? intercomName,
    bool clearIntercomName = false,
    String? audioRoute,
    bool? microphoneReady,
    String? connectionState,
    String? peerName,
    bool clearPeerName = false,
    int? sentPackets,
    int? receivedPackets,
    int? lostPackets,
    int? bufferedPackets,
    List<NearbyDevice>? devices,
    List<String>? events,
    String? error,
    bool clearError = false,
  }) => RidingSessionState(
    isLoading: isLoading ?? this.isLoading,
    isRiding: isRiding ?? this.isRiding,
    autoMatch: autoMatch ?? this.autoMatch,
    debugPhoneAudio: debugPhoneAudio ?? this.debugPhoneAudio,
    isMuted: isMuted ?? this.isMuted,
    bluetoothEnabled: bluetoothEnabled ?? this.bluetoothEnabled,
    wifiEnabled: wifiEnabled ?? this.wifiEnabled,
    microphoneGranted: microphoneGranted ?? this.microphoneGranted,
    nearbyGranted: nearbyGranted ?? this.nearbyGranted,
    playServicesAvailable: playServicesAvailable ?? this.playServicesAvailable,
    intercomName: clearIntercomName ? null : intercomName ?? this.intercomName,
    audioRoute: audioRoute ?? this.audioRoute,
    microphoneReady: microphoneReady ?? this.microphoneReady,
    connectionState: connectionState ?? this.connectionState,
    peerName: clearPeerName ? null : peerName ?? this.peerName,
    sentPackets: sentPackets ?? this.sentPackets,
    receivedPackets: receivedPackets ?? this.receivedPackets,
    lostPackets: lostPackets ?? this.lostPackets,
    bufferedPackets: bufferedPackets ?? this.bufferedPackets,
    devices: devices ?? this.devices,
    events: events ?? this.events,
    error: clearError ? null : error ?? this.error,
  );
}
