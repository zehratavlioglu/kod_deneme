import 'package:flutter/material.dart';

abstract final class RideLinkColors {
  // Dark theme
  static const darkBackground = Color(0xFF151C25);
  static const darkSurface = Color(0xFF222E3B);
  static const darkPrimary = Color(0xFF30D5C8);
  static const darkSecondary = Color(0xFF8DAFC0);
  static const darkText = Color(0xFFF0F5F6);
  static const darkMutedText = Color(0xFFB1C0C8);
  static const darkBorder = Color(0xFF394653);
  static const darkAccent = Color(0xFFFFB454);

  // Light theme
  static const lightBackground = Color(0xFFF4F7F8);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightPrimary = Color(0xFF087F7B);
  static const lightSecondary = Color(0xFF456C7E);
  static const lightText = Color(0xFF182832);
  static const lightMutedText = Color(0xFF536875);
  static const lightBorder = Color(0xFFD5E0E5);
  static const lightAccent = Color(0xFFAC5700);

  // Semantic colors
  static const connected = Color(0xFF23B99C);
  static const discovering = Color(0xFF4999E8);
  static const warning = Color(0xFFE9A23B);
  static const error = Color(0xFFEB6868);
}