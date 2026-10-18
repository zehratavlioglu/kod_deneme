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

          // 2. Sayfa İçeriği
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
                          color: RideLinkColors.darkSurface.withOpacity(0.85),
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(
                            color: RideLinkColors.darkPrimary.withOpacity(0.5), 
                            width: 2,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: RideLinkColors.darkPrimary.withOpacity(0.2),
                              blurRadius: 20,
                              spreadRadius: 2,
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.cell_tower,
                          color: RideLinkColors.darkPrimary,
                          size: 44,
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
                      
                      Text(
                        'Çevrimdışı ve Güvenli Sürüş Ağı',
                        style: TextStyle(
                          color: RideLinkColors.darkText.withOpacity(0.8),
                          fontSize: 14,
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                    ],
                  ),

                  // Alt Kısım / Giriş Butonları
                  Column(
                    children: [
                      AuthButton(
                        text: AppStrings.continueWithGoogle,
                        icon: Icons.g_mobiledata,
                        backgroundColor: RideLinkColors.lightSurface,
                        textColor: RideLinkColors.lightText,
                        onPressed: () {
                          // Google ile giriş aksiyonu
                        },
                      ),
                      
                      const SizedBox(height: 16),

                      AuthButton(
                        text: AppStrings.continueWithApple,
                        icon: Icons.apple,
                        backgroundColor: RideLinkColors.lightSurface,
                        textColor: RideLinkColors.lightText,
                        onPressed: () {
                          // Apple ile giriş aksiyonu
                        },
                      ),
                      const SizedBox(height: 20),
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