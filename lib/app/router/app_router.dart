import 'package:go_router/go_router.dart';

import '../../features/riding_session/presentation/screens/home_screen.dart';

final appRouter = GoRouter(
  routes: [GoRoute(path: '/', builder: (_, _) => const HomeScreen())],
);
