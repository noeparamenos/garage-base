package com.garagebase.features.auth.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Eventos que puede emitir el flujo de verificación por teléfono.
 */
sealed class PhoneAuthEvent {

    /**
     * Flujo normal: Firebase envió el SMS y confirma el [verificationId].
     * La app navega a OtpScreen para que el usuario introduzca el código.
     */
    data class CodeSent(val verificationId: String) : PhoneAuthEvent()

    /**
     * Verificación automática: en algunos dispositivos, Firebase valida el número
     * directamente sin SMS (el dispositivo reconoce el código de la SIM).
     * También ocurre en el emulador. La [credential] se usa directamente para el login.
     */
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneAuthEvent()
}

/**
 * Servicio de la capa de datos que envuelve la API de verificación por teléfono de Firebase.
 *
 * Firebase Phone Auth es callback-based, no suspendible.
 * - Usamos `callbackFlow` para transformar esos callbacks en un Flow (mismo patrón que usamos en `addSnapshotListener`).
 *
 * Vive en `data/` porque depende de [Activity] (Android SDK). No puede estar en `domain/`.
 */
class PhoneAuthService {

    private val auth = FirebaseAuth.getInstance()

    /**
     * Inicia la verificación del número de teléfono.
     *
     * @param phoneNumber Número en formato E.164 (ej. +34612345678).
     * @param activity Activity activa, requerida por Firebase para reCAPTCHA.
     *                 Se pasa como parámetro pero NO se almacena (evita memory leaks).
     * @return Flow que emite un único [PhoneAuthEvent] y luego se cierra.
     */
    fun sendVerificationCode(phoneNumber: String, activity: Activity): Flow<PhoneAuthEvent> =
        callbackFlow {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Verificación automática: enviamos la credencial y cerramos el Flow.
                    trySend(PhoneAuthEvent.AutoVerified(credential))
                    close()
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Error (número inválido, cuota superada…): cerramos con excepción.
                    // El ViewModel lo captura con `.catch { }`.
                    close(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    // Flujo normal: pasamos el verificationId. El Flow queda abierto
                    // hasta que awaitClose lo cierre al cancelar el colector.
                    //
                    // `trySend` en lugar de `send`: estamos en un callback no-suspendible,
                    // así que no podemos usar `suspend fun send()`. `trySend` es síncrono y
                    // devuelve un resultado (éxito/fallo) sin bloquear. En un callbackFlow
                    // con buffer por defecto (64 elementos) prácticamente nunca falla.
                    trySend(PhoneAuthEvent.CodeSent(verificationId))
                }
            }

            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
            )

            // awaitClose se ejecuta cuando el Flow se cancela (el ViewModel sale de scope).
            awaitClose()
        }

    /**
     * Completa el login con el código que introdujo el usuario.
     *
     * @param verificationId Recibido en [PhoneAuthEvent.CodeSent].
     * @param code Código de 6 dígitos del SMS.
     */
    suspend fun signInWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential).await()
    }

    /**
     * Completa el login con una credencial de verificación automática.
     *
     * @param credential Recibida en [PhoneAuthEvent.AutoVerified].
     */
    suspend fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).await()
    }
}
