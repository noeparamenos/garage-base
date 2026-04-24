package com.garagebase.features.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.garagebase.navigation.Screen
import com.garagebase.ui.theme.GarageBaseTheme

// ─── Capa stateful ────────────────────────────────────────────────────────────

/**
 * Orquestador de OtpScreen.
 *
 * @param verificationId Token de Firebase recibido como argumento de navegación
 *                       desde [LoginScreen]. Se pasa al ViewModel al verificar el código.
 */
@Composable
fun OtpScreen(
    verificationId: String,
    viewModel: AuthViewModel,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            val destino = if ((uiState as AuthUiState.Authenticated).isGestor)
                Screen.GestorHome.route else Screen.ConductorHome.route
            // popUpTo elimina la pila de auth: Atrás no vuelve al login
            navController.navigate(destino) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    OtpScreenContent(
        uiState = uiState,
        onVerificar = { code -> viewModel.verifyCode(verificationId, code) },
    )
}

// ─── Capa stateless ───────────────────────────────────────────────────────────

/**
 * Contenido visual de la pantalla OTP.
 *
 * @param uiState Estado actual del flujo.
 * @param onVerificar Callback que recibe el código de 6 dígitos y lanza la verificación.
 */
@Composable
internal fun OtpScreenContent(
    uiState: AuthUiState,
    onVerificar: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val cargando = uiState is AuthUiState.VerifyingCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Código de verificación", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Introduce el código de 6 dígitos que hemos enviado por SMS.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Código") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onVerificar(code) },
            enabled = code.length == 6 && !cargando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Verificar")
            }
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "OTP — esperando código")
@Composable
private fun OtpPreviewInicial() {
    GarageBaseTheme {
        OtpScreenContent(
            uiState = AuthUiState.VerifyingCode.let { AuthUiState.EnteringPhone },
            onVerificar = {},
        )
    }
}

@Preview(showBackground = true, name = "OTP — verificando")
@Composable
private fun OtpPreviewCargando() {
    GarageBaseTheme {
        OtpScreenContent(
            uiState = AuthUiState.VerifyingCode,
            onVerificar = {},
        )
    }
}

@Preview(showBackground = true, name = "OTP — código incorrecto")
@Composable
private fun OtpPreviewError() {
    GarageBaseTheme {
        OtpScreenContent(
            uiState = AuthUiState.Error("Código incorrecto. Inténtalo de nuevo."),
            onVerificar = {},
        )
    }
}
