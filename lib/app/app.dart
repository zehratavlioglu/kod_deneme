import 'package:flutter/material.dart';

import 'router/app_router.dart';
import 'theme/app_theme.dart';

class RideLinkApp extends StatelessWidget {
  const RideLinkApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'RideLink',
    debugShowCheckedModeBanner: false,
    theme: AppTheme.dark,
    routerConfig: appRouter,
  );
}
