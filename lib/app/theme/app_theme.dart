import 'package:flutter/material.dart';

abstract final class AppTheme {
  static ThemeData get dark => ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFFFF6B35),
      brightness: Brightness.dark,
      surface: const Color(0xFF111318),
    ),
    scaffoldBackgroundColor: const Color(0xFF090B0F),
    cardTheme: const CardThemeData(
      color: Color(0xFF151820),
      margin: EdgeInsets.zero,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(54)),
    ),
  );
}
