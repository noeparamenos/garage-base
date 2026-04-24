package com.garagebase.features.auth.data

import com.garagebase.features.auth.domain.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [AuthRepository] usando Firebase Auth.
 */
class AuthRepositoryImpl : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    override fun currentUserId(): String? = auth.currentUser?.uid

    /**
     * Lee el custom claim `gestor` del JWT del usuario actual.
     *
     * **`getIdToken(false)`** devuelve el token cacheado sin hacer una llamada de red si todavía es válido
     * - Firebase renueva los tokens automáticamente cada hora.
     *
     * El claim es  `Any?` porque el SDK lo deserializa genéricamente.
     * El cast `as? Boolean` devuelve null si el campo no existe o no es booleano,
     * y el operador `?: false` lo convierte en false.
     */
    override suspend fun isGestor(): Boolean {
        val result = auth.currentUser?.getIdToken(false)?.await() ?: return false
        return result.claims["gestor"] as? Boolean ?: false
    }

    override fun signOut() {
        auth.signOut()
    }
}
