package com.secondhandmarket.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secondhandmarket.ui.screens.DashboardScreen
import com.secondhandmarket.ui.screens.LandingScreen
import com.secondhandmarket.ui.screens.OtpVerificationScreen
import com.secondhandmarket.ui.screens.ProfileSetupScreen

/** Route names for the app's navigation graph. */
object Routes {
    const val LANDING = "landing"
    const val OTP_VERIFICATION = "otp_verification/{phoneNumber}"
    const val PROFILE_SETUP = "profile_setup"
    const val DASHBOARD = "dashboard"

    fun otpVerification(phoneNumber: String): String =
        "otp_verification/${Uri.encode(phoneNumber)}"
}

/**
 * Navigation flow:
 * landing -> otp_verification -> profile_setup -> dashboard
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LANDING
    ) {
        composable(Routes.LANDING) {
            LandingScreen(
                onOtpSent = { phoneNumber ->
                    navController.navigate(Routes.otpVerification(phoneNumber))
                }
            )
        }

        composable(
            route = Routes.OTP_VERIFICATION,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber").orEmpty()
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                onVerified = {
                    navController.navigate(Routes.PROFILE_SETUP) {
                        popUpTo(Routes.LANDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onCompleted = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LANDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen()
        }
    }
}
