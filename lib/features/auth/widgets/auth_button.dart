import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';

class AuthButton extends StatelessWidget {
  final String text;
  final IconData icon; // Direkt ikon alacak
  final double iconSize; // Boyutu buradan ayarlayacağız
  final VoidCallback onPressed;
  final Color backgroundColor;
  final Color textColor;
  final Color? iconColor; // İkonun kendi rengi (opsiyonel)

  const AuthButton({
    Key? key,
    required this.text,
    required this.icon,
    this.iconSize = 24.0,
    required this.onPressed,
    this.backgroundColor = RideLinkColors.lightSurface,
    this.textColor = RideLinkColors.lightText,
    this.iconColor,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 54,
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: backgroundColor,
          foregroundColor: textColor,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        onPressed: onPressed,
          child: Row(
          mainAxisAlignment: MainAxisAlignment.start, // Sola yaslar
          children: [
            const SizedBox(width: 24), // Sol kenardan biraz boşluk bırakır
            Icon(
              icon,
              size: iconSize,
              color: iconColor ?? textColor,
            ),
            const Spacer(), // İkon ile yazı arasında esnek boşluk bırakarak yazıyı ortalar
            Text(
              text,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: textColor,
              ),
            ),
            const Spacer(), // Yazıyı tam butonun ortasında tutmak için sağa da simetri verir
            const SizedBox(width: 24), // İkonun kapladığı alan kadar sağa denge payı
          ],
        ),
      ),
    );
  }
}