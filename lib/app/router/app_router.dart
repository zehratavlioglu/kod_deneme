import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:ridelink/features/auth/pages/login_page.dart'; // Paket yolu kullanarak doğrudan bağlıyoruz

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => LoginPage(),
    ),
  ],
);