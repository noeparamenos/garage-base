package com.garagebase.features.auth.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garagebase.features.auth.data.AuthRepositoryImpl
import com.garagebase.features.auth.data.PhoneAuthEvent
import com.garagebase.features.auth.data.PhoneAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Estados posibles de la pantalla de autenticación.
 *
 * **Sealed class**: el compilador obliga a cubrir todos los casos en los `when`.
 * - Cada estado corresponde a un momento concreto del flujo de login.
 */
sealed class AuthUiState {
    /** SplashScreen: comprobando si hay una sesión activa en Firebase Auth. */
    object Checking : AuthUiState()

    /** Sin sesión activa → navegar a LoginScreen. */
    object Unauthenticated : AuthUiState()

    /** LoginScreen: esperando que el usuario introduzca el número. */
    object EnteringPhone : AuthUiState()

    /** LoginScreen: SMS en camino, spinner activo. */
    object SendingCode : AuthUiState()

    /**
     * SMS confirmado → navegar a OtpScreen pasando el [verificationId].
     * @param verificationId Token de Firebase necesario para verificar el código OTP.
     */
    data class CodeSent(val verificationId: String) : AuthUiState()

    /** OtpScreen: verificando el código con Firebase, spinner activo. */
    object VerifyingCode : AuthUiState()

    /**
     * Login completado → navegar a GestorHome o ConductorHome.
     * @param isGestor true si el JWT contiene el custom claim `gestor: true`.
     */
    data class Authenticated(val isGestor: Boolean) : AuthUiState()

    /** Error recuperable: se muestra al usuario y puede reintentar. */
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel compartido entre SplashScreen, LoginScreen y OtpScreen.
 *
 * Patrón StateFlow + estados sellados:
 * - Los Composables observan [uiState] con `collectAsState()`.
 * - Reaccionan a cambios de estado en `LaunchedEffect(uiState)` para navegar.
 * - El ViewModel nunca tiene referencia al NavController (solo emite estados. La navegación es responsabilidad del Composable).
 *
 * Las dependencias se instancian internamente porque el proyecto aún no tiene inyección de dependencias (Hilt).
 * - Si se añade en el futuro, el constructor recibirá [AuthRepository] y [PhoneAuthService] como parámetros.
 */
class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepositoryImpl()
    private val phoneAuthService = PhoneAuthService()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)

    /**
     * Estado observable de la UI. Los Composables lo leen con `collectAsState()`.
     *
     * `asStateFlow()` expone el Flow como solo lectura:
     * - solo el ViewModel puede escribir en `_uiState`
     * - los Composables solo pueden leer `uiState`.
     */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    /**
     * Comprueba si hay sesión activa al arrancar la app (SplashScreen).
     *
     * Si hay sesión → lee el claim gestor → emite [AuthUiState.Authenticated].
     * Si no → emite [AuthUiState.Unauthenticated].
     */
    /**
     * Comprueba si hay sesión activa al arrancar la app (SplashScreen).
     *
     * Si hay sesión → lee el claim gestor → emite [AuthUiState.Authenticated].
     * Si no → emite [AuthUiState.Unauthenticated].
     *
     * El [runCatching] protege la llamada a [isGestor]: si el token almacenado es
     * inválido (p.ej. al cambiar entre emulador y Firebase real), `getIdToken()` lanza
     * una excepción. En ese caso cerramos la sesión corrupta y mandamos al usuario
     * al login en lugar de dejar que el proceso muera.
     */
    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Checking
            if (authRepository.currentUserId() == null) {
                _uiState.value = AuthUiState.Unauthenticated
            } else {
                runCatching { authRepository.isGestor() }
                    .onSuccess { isGestor ->
                        _uiState.value = AuthUiState.Authenticated(isGestor)
                    }
                    .onFailure {
                        authRepository.signOut()
                        _uiState.value = AuthUiState.Unauthenticated
                    }
            }
        }
    }

    /**
     * Inicia el envío del SMS de verificación.
     *
     * [Activity] se pasa como parámetro (no se almacena) para evitar memory leaks:
     * - si el ViewModel sobrevive a la Activity (p.ej. al rotar la pantalla), no habría referencia colgante.
     * - Firebase la necesita solo durante la llamada a verifyPhoneNumber.
     *
     * @param phoneNumber Número en formato E.164 (+34...).
     * @param activity Activity activa en el momento de la llamada.
     */
    fun sendVerificationCode(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.SendingCode

            phoneAuthService.sendVerificationCode(phoneNumber, activity)
                .catch { e ->
                    _uiState.value = AuthUiState.Error(
                        e.message ?: "Error al enviar el código. Comprueba el número."
                    )
                }
                .collect { event ->
                    when (event) {
                        is PhoneAuthEvent.CodeSent ->
                            _uiState.value = AuthUiState.CodeSent(event.verificationId)

                        is PhoneAuthEvent.AutoVerified -> {
                            // El dispositivo verificó automáticamente — completamos el login
                            // sin pasar por la pantalla OTP.
                            _uiState.value = AuthUiState.VerifyingCode
                            runCatching { phoneAuthService.signInWithCredential(event.credential) }
                                .onSuccess {
                                    _uiState.value = AuthUiState.Authenticated(authRepository.isGestor())
                                }
                                .onFailure { e ->
                                    _uiState.value = AuthUiState.Error(
                                        e.message ?: "Error de verificación automática."
                                    )
                                }
                        }
                    }
                }
        }
    }

    /**
     * Verifica el código OTP que introdujo el usuario.
     *
     * @param verificationId Token recibido en [AuthUiState.CodeSent], viaja como
     *                       argumento de navegación desde LoginScreen a OtpScreen.
     * @param code Código de 6 dígitos del SMS.
     */
    fun verifyCode(verificationId: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.VerifyingCode
            runCatching { phoneAuthService.signInWithCode(verificationId, code) }
                .onSuccess {
                    _uiState.value = AuthUiState.Authenticated(authRepository.isGestor())
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(
                        e.message ?: "Código incorrecto. Inténtalo de nuevo."
                    )
                }
        }
    }

    /** Permite reintentar tras un error volviendo al estado de entrada de número. */
    fun clearError() {
        _uiState.value = AuthUiState.EnteringPhone
    }
}
