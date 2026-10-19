import 'package:flutter/material.dart';
import 'package:ridelink/core/constants/app_colors.dart';
import 'package:ridelink/core/constants/app_strings.dart';
import 'package:ridelink/features/auth/widgets/auth_button.dart';

class LoginPage extends StatelessWidget {
  const LoginPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          // 1. Arka Plan Dağ/Arazi Görseli
          Image.asset(
            'ridelink_image/splash_screen.jpeg',
            fit: BoxFit.cover,
          ),
          
          // Yazıların ve butonların net okunması için hafif koyu şeffaf katman
          Container(
            color: RideLinkColors.darkBackground.withOpacity(0.55),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 24.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const SizedBox(height: 10),
                  
                  // Üst Kısım / Logo ve İsim Grubu
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 88,
                        height: 88,
                        decoration: BoxDecoration(
                          color: RideLinkColors.darkSurface,
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(
                            color: RideLinkColors.darkPrimary.withOpacity(0.5), 
                            width: 2,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: RideLinkColors.darkPrimary.withOpacity(0.3),
                              blurRadius: 20,
                              spreadRadius: 2,
                            ),
                          ],
                        ),
                        child: CustomPaint(
                          painter: RadarLogoPainter(color: RideLinkColors.darkPrimary),
                        ),
                      ),
                      const SizedBox(height: 20),
                      const Text(
                        AppStrings.appName,
                        style: TextStyle(
                          color: RideLinkColors.darkText,
                          fontSize: 28,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 3.0,
                        ),
                      ),
                      const SizedBox(height: 8),
                    ],
                  ),

                  // Alt Kısım / Giriş Butonları
                  Column(
                    children: [
                      AuthButton(
                        text: AppStrings.continueWithGoogle,
                        icon: Icons.g_mobiledata,
                        iconSize: 32.0, 
                        onPressed: () {},
                      ),
                      const SizedBox(height: 20),
                      AuthButton(
                        text: AppStrings.continueWithApple,
                        icon: Icons.apple,
                        iconSize: 32.0, 
                        onPressed: () {},
                      ),
                      const SizedBox(height: 50),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// İki tarafa simetrik bakan yayları ve parlayan noktayı çizen güncellenmiş painter
class RadarLogoPainter extends CustomPainter {
  final Color color;
  RadarLogoPainter({required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = 3.0
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final center = Offset(size.width / 2, size.height / 2);

    // Sağ İç Yay
    canvas.drawArc(Rect.fromCircle(center: center, radius: 14), -0.8, 1.6, false, paint);
    // Sol İç Yay
    canvas.drawArc(Rect.fromCircle(center: center, radius: 14), 2.3, 1.6, false, paint);

    // Sağ Dış Yay
    canvas.drawArc(Rect.fromCircle(center: center, radius: 24), -0.8, 1.6, false, paint);
    // Sol Dış Yay
    canvas.drawArc(Rect.fromCircle(center: center, radius: 24), 2.3, 1.6, false, paint);

    // Merkezdeki parlayan nokta
    final dotPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    canvas.drawCircle(center, 5.5, dotPaint);

    // Noktanın etrafındaki parıltı (Glow) efekti
    final glowPaint = Paint()
      ..color = color.withOpacity(0.4)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6);
    
    canvas.drawCircle(center, 8, glowPaint);
  }
  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}