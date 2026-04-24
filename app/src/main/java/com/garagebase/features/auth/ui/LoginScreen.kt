package com.garagebase.features.auth.ui

import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.TextButton
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
// Única responsabilidad: conectar el ViewModel con el contenido y gestionar
// los efectos secundarios de navegación. No contiene lógica de UI.

/**
 * Orquestador de LoginScreen: extrae estado del ViewModel, escucha cambios
 * con [LaunchedEffect] y navega cuando corresponde.
 *
 * Nunca renderiza UI directamente — delega todo en [LoginScreenContent].
 */
@Composable
fun LoginScreen(viewModel: AuthViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current

    // LaunchedEffect(uiState): se re-ejecuta cada vez que el estado cambia.
    // Separar la navegación aquí (en el stateful) mantiene el Content libre
    // de NavController, lo que lo hace previewable y testeable en aislamiento.
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.CodeSent) {
            val verificationId = (uiState as AuthUiState.CodeSent).verificationId
            navController.navigate(Screen.Otp.createRoute(verificationId))
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onEnviarCodigo = { phoneNumber ->
            activity?.let { viewModel.sendVerificationCode(phoneNumber, it) }
        },
        onReintentar = viewModel::clearError,
    )
}

// ─── Capa stateless ───────────────────────────────────────────────────────────
// Solo sabe pintar. Recibe valores y lambdas — sin ViewModel, sin NavController.
// `internal`: visible para tests del módulo, no forma parte de la API pública.

/**
 * Contenido visual de la pantalla de login.
 *
 * [phoneNumber] se gestiona localmente con `remember` porque es estado efímero
 * de UI (lo que el usuario está escribiendo). No necesita sobrevivir a una
 * recomposición externa — si el usuario sale y vuelve, es razonable que se limpie.
 *
 * @param uiState Estado actual del flujo de autenticación.
 * @param onEnviarCodigo Callback que recibe el número escrito y lanza la verificación.
 * @param onReintentar Callback para volver al estado inicial tras un error.
 */
@Composable
internal fun LoginScreenContent(
    uiState: AuthUiState,
    onEnviarCodigo: (String) -> Unit,
    onReintentar: () -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("+34") }
    val cargando = uiState is AuthUiState.SendingCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "GarageBase", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gestión de flota",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Número de teléfono") },
            placeholder = { Text("612 345 678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onEnviarCodigo(phoneNumber.trim()) },
            enabled = phoneNumber.isNotBlank() && !cargando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Enviar código")
            }
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onReintentar) {
                Text("Reintentar")
            }
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────
// `private`: solo visibles en este archivo. Cada una muestra un estado distinto
// para que el panel de previews de Android Studio actúe como documentación visual.

@Preview(showBackground = true, name = "Login — estado inicial")
@Composable
private fun LoginPreviewInicial() {
    GarageBaseTheme {
        LoginScreenContent(
            uiState = AuthUiState.EnteringPhone,
            onEnviarCodigo = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — enviando código")
@Composable
private fun LoginPreviewCargando() {
    GarageBaseTheme {
        LoginScreenContent(
            uiState = AuthUiState.SendingCode,
            onEnviarCodigo = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — error")
@Composable
private fun LoginPreviewError() {
    GarageBaseTheme {
        LoginScreenContent(
            uiState = AuthUiState.Error("Número inválido. Usa el formato +34 612 345 678"),
            onEnviarCodigo = {},
            onReintentar = {},
        )
    }
}
