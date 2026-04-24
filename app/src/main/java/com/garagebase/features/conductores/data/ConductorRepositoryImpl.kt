package com.garagebase.features.conductores.data

import com.garagebase.core.model.Conductor
import com.garagebase.features.conductores.domain.ConductorRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [ConductorRepository] usando Cloud Firestore.
 * - Accede a la colección `/conductores` y convierte cada documento a [Conductor]
 * mediante [ConductorDto].
 *
 * **callbackFlow:** forma de convertir una API basada en callbacks (listeners de Firestore) en un Flow.
 * - Abrimos un listener de Firestore dentro del bloque `callbackFlow`.
 * - Cada vez que Firestore notifica un cambio, llamamos a `trySend()` para emitir el valor.
 * - Cuando el colector cancela el Flow (ej: al salir de la pantalla), se ejecuta
 *   `awaitClose`, donde eliminamos el listener para no dejar conexiones abiertas.
 */
class ConductorRepositoryImpl : ConductorRepository {

    /** Referencia a la colección raíz de conductores en Firestore. */
    private val col = Firebase.firestore.collection("conductores")

    /**
     * Escucha en tiempo real el perfil de un conductor por su ID.
     *
     * **`addSnapshotListener` de Firestore**
     * - Mantiene una conexión WebSocket activa y notifica cada vez que el documento cambia.
     * - Emite `null` si el documento no existe todavía.
     *
     * @param id UID de Firebase Auth del conductor.
     * @return Flow que emite el [Conductor] actualizado, o null si no existe.
     */
    override fun findById(id: String): Flow<Conductor?> = callbackFlow {
        val listener = col.document(id).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            // toObject deserializa el documento Firestore al DTO automáticamente
            trySend(snap?.toObject(ConductorDto::class.java)?.toDomain(snap.id))
        }
        // awaitClose se ejecuta cuando el Flow se cancela — elimina el listener de Firestore
        awaitClose { listener.remove() }
    }


    /**
     * Escucha en tiempo real la lista completa de conductores.
     * - Cada cambio en cualquier documento de `/conductores` dispara una nueva emisión con la lista actualizada.
     *
     * @return Flow que emite la lista de conductores actualizada.
     */
    override fun getAll(): Flow<List<Conductor>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            // mapNotNull descarta documentos que no se puedan deserializar (evita crashes)
            trySend(snap?.documents?.mapNotNull { doc ->
                doc.toObject(ConductorDto::class.java)?.toDomain(doc.id)
            } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    /**
     * Crea un documento nuevo en `/conductores` con ID generado por Firestore.
     *
     * Se usa `col.add()` (no `col.document(id).set()`) para dejar que Firestore
     * asigne el ID — el UID de Firebase Auth aún no existe en este punto.
     *
     * @return ID del documento recién creado.
     */
    override suspend fun add(nombre: String, telefono: String): String =
        col.add(mapOf("nombre" to nombre, "telefono" to telefono, "rol" to "conductor"))
            .await()
            .id

    /**
     * Actualiza nombre y teléfono de un conductor sin tocar el campo `rol`.
     *
     * `update()` solo modifica los campos indicados; el resto del documento queda intacto.
     */
    override suspend fun update(id: String, nombre: String, telefono: String) {
        col.document(id).update("nombre", nombre, "telefono", telefono).await()
    }
}
