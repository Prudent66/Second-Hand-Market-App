package com.secondhandmarket.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secondhandmarket.ui.screens.AdminDashboardScreen
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
    const val ADMIN = "admin"

    fun otpVerification(phoneNumber: String): String =
        "otp_verification/${Uri.encode(phoneNumber)}"
}

private const val TRANSITION_MS = 320
private const val FADE_MS = 220

/**
 * Navigation flow:
 * landing -> otp_verification -> profile_setup -> dashboard
 * The dashboard can reveal the hidden admin console by long-pressing its title.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LANDING,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 6 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeIn(animationSpec = tween(FADE_MS))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 6 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeOut(animationSpec = tween(FADE_MS))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 6 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeIn(animationSpec = tween(FADE_MS))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 6 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeOut(animationSpec = tween(FADE_MS))
        }
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
                },
                onBack = { navController.popBackStack() }
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
            DashboardScreen(
                onAdminClick = { navController.navigate(Routes.ADMIN) }
            )
        }

        composable(Routes.ADMIN) {
            AdminDashboardScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
