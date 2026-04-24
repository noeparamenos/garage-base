package com.garagebase.features.auth.domain

/**
 * Contrato de dominio para consultar el estado de autenticación.
 *
 * Solo operaciones de lectura sin dependencias de Android SDK:
 * son puras, testables y agnósticas de Firebase.
 *
 * El flujo de verificación por teléfono (necesita Activity) vive en la capa de datos
 * porque está acoplado al Android SDK.
 */
interface AuthRepository {

    /** UID del usuario con sesión activa, o null si no hay sesión. */
    fun currentUserId(): String?

    /**
     * Comprueba si el usuario autenticado tiene el custom claim `gestor: true`.
     *
     * - Lee el token JWT cacheado sin forzar un refresco de red (parámetro `false`).
     * - El claim ya debe estar presente desde el login — si no, devuelve false.
     */
    suspend fun isGestor(): Boolean
}
