package com.garagebase.features.auth.data

import com.garagebase.features.auth.domain.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [AuthRepository] usando Firebase Auth y Firestore.
 *
 * `FirebaseAuth.getInstance()` y `FirebaseFirestore.getInstance()` devuelven cada una
 * su propio singleton gestionado por el SDK: `auth` y `firestore` son objetos distintos,
 * pero siempre la misma instancia de cada tipo dentro del proceso.
 * No necesitamos inyectarlas con Hilt salvo que queramos testeabilidad avanzada.
 */
class AuthRepositoryImpl : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun currentUserId(): String? = auth.currentUser?.uid

    /**
     * Comprueba si el conductor actual tiene rol gestor leyendo su documento en Firestore.
     *
     * - campo `rol` del documento `/conductores/{uid}` en lugar de custom claims JWT,
     * - permite gestionar el rol directamente desde la consola de Firebase sin necesidad del Admin SDK.
     *
     * [getString] devuelve null si el campo no existe, lo que convierte el == en false.
     */
    override suspend fun isGestor(): Boolean {
        // ? -> Si hay sesion activa busca el uid
        // ?: si el uid es null devuelve false
        val uid = auth.currentUser?.uid ?: return false
        // `.get()` devuelve un `Task<DocumentSnapshot>`.
        // `.await()` suspende la coroutine hasta que el `Task` completa, sin bloquear el hilo.
        val doc = firestore.collection("conductores").document(uid).get().await()
        // [getString] devuelve null si el campo no existe
        return doc.getString("rol") == "gestor"
    }

    override fun signOut() {
        auth.signOut()
    }
}
