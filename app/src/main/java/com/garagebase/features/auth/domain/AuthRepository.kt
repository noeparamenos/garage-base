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
     * Comprueba si el usuario autenticado tiene rol de gestor.
     *
     * Suspendible porque puede implicar una llamada de red (la implementación decide
     * si lee un token cacheado, un documento en Firestore, etc.).
     * Devuelve false si no hay sesión activa.
     */
    suspend fun isGestor(): Boolean

    /**
     * Cierra la sesión actual borrando las credenciales almacenadas localmente.
     *
     * Se usa cuando el token guardado es inválido (p.ej. al cambiar entre el
     * emulador local y Firebase real) para evitar que la app quede atascada
     * en un estado de sesión corrupta.
     */
    fun signOut()
}
