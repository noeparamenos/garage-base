package com.garagebase.features.auth.data

import com.garagebase.features.auth.domain.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [AuthRepository] usando Firebase Auth y Firestore.
 */
class AuthRepositoryImpl : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun currentUserId(): String? = auth.currentUser?.uid

    /**
     * Comprueba si el conductor actual tiene rol gestor leyendo su documento en Firestore.
     *
     * Se usa el campo `rol` del documento `/conductores/{uid}` en lugar de custom claims JWT,
     * lo que permite gestionar el rol directamente desde la consola de Firebase
     * sin necesidad del Admin SDK.
     *
     * [getString] devuelve null si el campo no existe, lo que convierte el == en false.
     */
    override suspend fun isGestor(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = firestore.collection("conductores").document(uid).get().await()
        return doc.getString("rol") == "gestor"
    }

    override fun signOut() {
        auth.signOut()
    }
}
