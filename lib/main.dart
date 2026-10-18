import 'package:flutter/material.dart';
import 'app/router/app_router.dart'; // go_router dosyamızı buraya bağlıyoruz

void main() {
  runApp(const RideLinkApp());
}

class RideLinkApp extends StatelessWidget {
  const RideLinkApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'RideLink',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
      ),
      routerConfig: appRouter, // MaterialApp.router kullanarak rotaları appRouter'dan alıyoruz
    );
  }
}