package com.garagebase.features.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.garagebase.navigation.Screen
import com.garagebase.ui.theme.GarageBaseTheme

// ─── Capa stateful ────────────────────────────────────────────────────────────

/**
 * Orquestador de SplashScreen: observa el estado del ViewModel y navega
 * en cuanto la comprobación de sesión termina.
 *
 * `popUpTo + inclusive = true` elimina SplashScreen del backstack: el usuario
 * no puede volver aquí con el botón Atrás.
 */
@Composable
fun SplashScreen(viewModel: AuthViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is AuthUiState.Authenticated -> {
                val destino = if (state.isGestor) Screen.GestorHome.route
                              else Screen.ConductorHome.route
                navController.navigate(destino) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    SplashScreenContent()
}

// ─── Capa stateless ───────────────────────────────────────────────────────────

/** Spinner centrado mientras se comprueba la sesión. */
@Composable
internal fun SplashScreenContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Splash — comprobando sesión")
@Composable
private fun SplashPreview() {
    GarageBaseTheme {
        SplashScreenContent()
    }
}
