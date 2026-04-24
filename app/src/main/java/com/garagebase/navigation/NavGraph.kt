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
import com.garagebase.features.gestor.ui.ConductorDetalleScreen
import com.garagebase.features.gestor.ui.ConductoresScreen
import com.garagebase.features.gestor.ui.GestorHomeScreen
import com.garagebase.features.gestor.ui.IncidenciasGestorScreen
import com.garagebase.features.gestor.ui.VehiculoDetalleScreen
import com.garagebase.features.gestor.ui.VehiculosScreen

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
 *
 * Los ViewModels de las pantallas del gestor se crean dentro de cada Composable:
 * - Su ciclo de vida queda ligado al NavBackStackEntry de la pantalla (se destruyen al salir).
 * - Los que necesitan el ID del vehículo reciben el SavedStateHandle automáticamente desde
 *   Navigation, sin necesitar un ViewModelProvider.Factory explícito.
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
            GestorHomeScreen(navController = navController)
        }

        composable(Screen.GestorConductores.route) {
            ConductoresScreen(navController = navController)
        }

        // El conductorId se extrae del SavedStateHandle en GestorConductorDetalleViewModel.
        composable(
            route = Screen.GestorConductorDetalle.route,
            arguments = listOf(navArgument("conductorId") { type = NavType.StringType }),
        ) {
            ConductorDetalleScreen(navController = navController)
        }

        composable(Screen.GestorVehiculos.route) {
            VehiculosScreen(navController = navController)
        }

        // El vehiculoId se extrae del SavedStateHandle en GestorVehiculoDetalleViewModel.
        composable(
            route = Screen.GestorVehiculoDetalle.route,
            arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType }),
        ) {
            VehiculoDetalleScreen(navController = navController)
        }

        composable(Screen.GestorIncidencias.route) {
            IncidenciasGestorScreen(navController = navController)
        }

        composable(Screen.ConductorHome.route) {
            ConductorHomeScreen()
        }
    }
}
