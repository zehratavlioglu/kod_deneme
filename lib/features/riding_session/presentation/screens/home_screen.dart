import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/riding_session_controller.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(ridingSessionProvider);
    final controller = ref.read(ridingSessionProvider.notifier);
    return Scaffold(
      appBar: AppBar(
        title: const Text('RIDELINK'),
        actions: [
          IconButton(
            onPressed: controller.refresh,
            icon: const Icon(Icons.sync),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: controller.refresh,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _HeroStatus(state: state.connectionState, riding: state.isRiding),
            const SizedBox(height: 12),
            if (state.error != null)
              Card(
                color: Theme.of(context).colorScheme.errorContainer,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(state.error!),
                ),
              ),
            const SizedBox(height: 12),
            _Section(
              title: 'Sistem durumu',
              children: [
                _StatusRow('Bluetooth', state.bluetoothEnabled),
                _StatusRow('Wi‑Fi', state.wifiEnabled),
                _StatusRow('Yakındaki cihaz izinleri', state.nearbyGranted),
                _StatusRow('Mikrofon izni', state.microphoneGranted),
                _StatusRow('Google Play services', state.playServicesAvailable),
              ],
            ),
            const SizedBox(height: 12),
            _Section(
              title: 'İnterkom',
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.headset_mic),
                  title: Text(state.intercomName ?? 'İnterkom bağlı değil'),
                  subtitle: Text('Aktif rota: ${state.audioRoute}'),
                  trailing: Icon(
                    state.microphoneReady ? Icons.mic : Icons.mic_off,
                    color: state.microphoneReady ? Colors.greenAccent : null,
                  ),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Telefon sesiyle test'),
                  subtitle: const Text('İnterkom yokken debug amaçlı'),
                  value: state.debugPhoneAudio,
                  onChanged: state.isRiding
                      ? null
                      : controller.setDebugPhoneAudio,
                ),
              ],
            ),
            const SizedBox(height: 12),
            _Section(
              title: 'Yakındaki sürücüler (${state.devices.length})',
              children: state.devices.isEmpty
                  ? const [Text('Henüz RideLink cihazı bulunmadı.')]
                  : state.devices.map((device) {
                      final length = device.endpointId.length.clamp(0, 6);
                      return ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.two_wheeler),
                        title: Text(device.displayName),
                        subtitle: Text(
                          '${device.status.name} · ${device.endpointId.substring(0, length)}',
                        ),
                      );
                    }).toList(),
            ),
            const SizedBox(height: 12),
            _Section(
              title: 'Kontroller',
              children: [
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Otomatik eşleşme'),
                  subtitle: const Text(
                    'Yakındaki uygun sürücülere otomatik katıl',
                  ),
                  value: state.autoMatch,
                  onChanged: controller.setAutoMatch,
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Mikrofonu sustur'),
                  value: state.isMuted,
                  onChanged: state.isRiding ? controller.setMuted : null,
                ),
                const SizedBox(height: 8),
                FilledButton.icon(
                  onPressed: state.isLoading
                      ? null
                      : state.isRiding
                      ? controller.stop
                      : controller.start,
                  icon: Icon(state.isRiding ? Icons.stop : Icons.play_arrow),
                  label: Text(
                    state.isRiding
                        ? 'Sürüş Modunu Durdur'
                        : 'Sürüş Modunu Başlat',
                  ),
                ),
                if (state.connectionState == 'connected') ...[
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: controller.disconnect,
                    icon: const Icon(Icons.call_end),
                    label: const Text('Görüşmeyi Sonlandır'),
                  ),
                ],
              ],
            ),
            const SizedBox(height: 12),
            _Section(
              title: 'Geliştirici telemetrisi',
              children: [
                Text(
                  'Gönderilen: ${state.sentPackets}  Alınan: ${state.receivedPackets}  '
                  'Kayıp: ${state.lostPackets}  Buffer: ${state.bufferedPackets}',
                ),
                const Divider(height: 24),
                ...state.events
                    .take(20)
                    .map(
                      (event) => Padding(
                        padding: const EdgeInsets.only(bottom: 6),
                        child: Text(
                          event,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ),
                    ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _HeroStatus extends StatelessWidget {
  const _HeroStatus({required this.state, required this.riding});
  final String state;
  final bool riding;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(
      borderRadius: BorderRadius.circular(22),
      gradient: const LinearGradient(
        colors: [Color(0xFFED5A24), Color(0xFF7D2A13)],
      ),
    ),
    child: Row(
      children: [
        Icon(riding ? Icons.two_wheeler : Icons.power_settings_new, size: 42),
        const SizedBox(width: 16),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              riding ? 'SÜRÜŞ MODU AÇIK' : 'HAZIR',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            Text('Bağlantı: $state'),
          ],
        ),
      ],
    ),
  );
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          ...children,
        ],
      ),
    ),
  );
}

class _StatusRow extends StatelessWidget {
  const _StatusRow(this.label, this.ok);
  final String label;
  final bool ok;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 5),
    child: Row(
      children: [
        Icon(
          ok ? Icons.check_circle : Icons.error_outline,
          color: ok ? Colors.greenAccent : Colors.orangeAccent,
          size: 20,
        ),
        const SizedBox(width: 10),
        Expanded(child: Text(label)),
        Text(ok ? 'Hazır' : 'Gerekli'),
      ],
    ),
  );
}
