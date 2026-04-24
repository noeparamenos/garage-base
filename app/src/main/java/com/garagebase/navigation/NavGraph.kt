package com.garagebase.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.garagebase.features.auth.ui.AuthViewModel
import com.garagebase.features.auth.ui.LoginScreen
import com.garagebase.features.auth.ui.OtpScreen
import com.garagebase.features.auth.ui.SplashScreen
import com.garagebase.features.conductores.ui.ConductorHomeScreen
import com.garagebase.features.gestor.ui.GestorHomeScreen

/**
 * Grafo de navegación principal de la app.
 *
 * El [AuthViewModel] se crea aquí con `viewModel()` para que Splash, Login y Otp
 * compartan la misma instancia — y por tanto el mismo estado de autenticación.
 * Su ciclo de vida queda ligado a este Composable (la Activity), no a cada pantalla.
 *
 * `NavHost` define el destino inicial y registra todos los destinos posibles.
 * La navegación entre ellos es responsabilidad de cada Composable (no del NavGraph),
 * que recibe el [navController] y llama a `navigate()` según el estado del ViewModel.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(viewModel = authViewModel, navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(viewModel = authViewModel, navController = navController)
        }

        // Destino con argumento: `verificationId` viaja en la URL de navegación.
        // navArgument declara el tipo y permite a Navigation extraerlo del backstack entry.
        // Ruta resultante: "otp/eyJhbGciOi..."
        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("verificationId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            OtpScreen(
                verificationId = verificationId,
                viewModel = authViewModel,
                navController = navController,
            )
        }

        composable(Screen.GestorHome.route) {
            GestorHomeScreen()
        }

        composable(Screen.ConductorHome.route) {
            ConductorHomeScreen()
        }
    }
}
