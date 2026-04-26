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
     *
     * Cada documento en `/conductores` tiene como ID el UID de Firebase Auth.
     * El doc temporal con ID = teléfono (creado por el gestor) se borra en el
     * primer login del conductor dentro de [vincularPorTelefono], por lo que
     * nunca hay duplicados visibles aquí.
     *
     * @return Flow que emite la lista actualizada cada vez que cambia algún conductor.
     */
    override fun getAll(): Flow<List<Conductor>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.documents?.mapNotNull { doc ->
                doc.toObject(ConductorDto::class.java)?.toDomain(doc.id)
            } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    /**
     * Crea el documento del conductor usando el **teléfono como ID**.
     *
     * Usar el teléfono como ID (en lugar de `col.add()` que genera un ID aleatorio)
     * permite que las Security Rules verifiquen la existencia de este documento
     * con `exists()` al primer login del conductor — así solo los conductores
     * pre-registrados por el gestor pueden crear su perfil de UID.
     *
     * El documento con ID = teléfono es temporal: `vincularPorTelefono` crea
     * el definitivo con ID = UID en el primer login.
     *
     * @return El teléfono, que actúa como ID del documento.
     */
    override suspend fun add(nombre: String, telefono: String): String {
        col.document(telefono).set(
            mapOf("nombre" to nombre, "telefono" to telefono, "rol" to "conductor")
        ).await()
        return telefono
    }

    /**
     * Actualiza nombre y teléfono de un conductor sin tocar el campo `rol`.
     *
     * `update()` solo modifica los campos indicados; el resto del documento queda intacto.
     */
    override suspend fun update(id: String, nombre: String, telefono: String) {
        col.document(id).update("nombre", nombre, "telefono", telefono).await()
    }

    /** Borra el documento del conductor. Las Security Rules deben permitir `delete` al gestor. */
    override suspend fun delete(id: String) {
        col.document(id).delete().await()
    }

    /**
     * Vincula el documento pre-creado por el gestor con el UID real de Firebase Auth.
     *
     * **Por qué NO usamos WriteBatch aquí**: la regla de actualización del vehículo
     * hace un `get()` al documento del conductor para verificar el teléfono. Firestore
     * evalúa las reglas contra el estado *ya comprometido*, nunca contra cambios
     * pendientes del mismo batch. Si el create del conductor y el update del vehículo
     * van en el mismo batch, la regla del vehículo no encuentra el conductor nuevo →
     * PERMISSION_DENIED aunque ambas operaciones sean legítimas.
     * Solución: dos escrituras secuenciales con `.await()` — el conductor se crea
     * primero, se compromete, y solo entonces se actualiza el vehículo.
     *
     * **El documento antiguo no se borra**: las reglas prohíben `delete` a todos los
     * clientes (`allow delete: if false`). El doc antiguo queda huérfano en Firestore
     * (el vehículo ya no lo referencia y findById usa el UID) — su impacto es mínimo.
     */
    override suspend fun vincularPorTelefono(uid: String, telefono: String) {
        val vehiculosCol = Firebase.firestore.collection("vehiculos")

        // Guardia: si el doc con UID ya existe, la vinculación está completa o casi.
        // Cubre dos fallos parciales:
        //   A) batch falló antes de commit → vehículo sigue apuntando al teléfono Y phone doc existe.
        //   B) batch falló a mitad (imposible con batch atómico, pero defensive) → solo phone doc queda.
        val uidDoc = col.document(uid).get().await()
        if (uidDoc.exists()) {
            val vehiculoConIdAntiguo = vehiculosCol
                .whereEqualTo("conductorId", telefono).get().await()
                .documents.firstOrNull()
            val phoneDoc = col.document(telefono).get().await()
            if (vehiculoConIdAntiguo != null || phoneDoc.exists()) {
                val cleanupBatch = Firebase.firestore.batch()
                if (vehiculoConIdAntiguo != null) {
                    cleanupBatch.update(vehiculoConIdAntiguo.reference, "conductorId", uid)
                }
                if (phoneDoc.exists()) {
                    cleanupBatch.delete(phoneDoc.reference)
                }
                cleanupBatch.commit().await()
            }
            return
        }

        // Doc UID no existe → buscar el doc pre-creado por el gestor.
        val preDoc = col.whereEqualTo("telefono", telefono).get().await()
            .documents.firstOrNull { it.id != uid } ?: return

        val data = preDoc.data ?: return

        // Paso 1 — crear /conductores/{uid} y esperar a que se comprometa.
        // La regla del vehículo (paso 2) hace get() sobre este doc para verificar
        // el teléfono; si aún no está comprometido, Firestore no lo encuentra y
        // devuelve PERMISSION_DENIED aunque los datos sean correctos.
        col.document(uid).set(data).await()

        // Paso 2 — batch atómico: actualizar el vehículo + borrar el doc huérfano.
        // Ambas operaciones en el mismo batch para evitar el estado intermedio donde
        // el vehículo apunta al UID pero el doc del teléfono sigue existiendo.
        val batch = Firebase.firestore.batch()

        val vehiculoAnterior = vehiculosCol
            .whereEqualTo("conductorId", preDoc.id).get().await()
            .documents.firstOrNull()
        if (vehiculoAnterior != null) {
            batch.update(vehiculoAnterior.reference, "conductorId", uid)
        }

        // La regla permite borrar el doc cuyo ID == teléfono del usuario autenticado.
        batch.delete(preDoc.reference)

        batch.commit().await()
    }
}
