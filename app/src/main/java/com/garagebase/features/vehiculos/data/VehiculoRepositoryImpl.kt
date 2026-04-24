package com.garagebase.features.vehiculos.data

import com.garagebase.core.model.Vehiculo
import com.garagebase.features.vehiculos.domain.VehiculoRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [VehiculoRepository] usando Cloud Firestore.
 *
 * Accede a la colección `/vehiculos` y convierte cada documento a [Vehiculo] mediante [VehiculoDto]
 *
 * - Las lecturas usan `callbackFlow` para envolver los listeners en tiempo real de Firestore.
 * - Las escrituras usan `.await()` para convertir la `Task<T>` de Firestore en una coroutine suspendible sin callbacks.
 */
class VehiculoRepositoryImpl : VehiculoRepository {

    /** Referencia a la colección raíz de vehículos en Firestore. */
    private val col = Firebase.firestore.collection("vehiculos")

    /**
     * Escucha en tiempo real el vehículo asignado a un conductor.
     *
     * - Filtra la colección por `conductorId` y devuelve el primero que encuentre, (un conductor = 0/1 vehiculos)
     * - Emite `null` si no hay ningún vehículo con ese `conductorId`.
     *
     * @param conductorId UID del conductor cuyo vehículo se quiere observar.
     * @return Flow que emite el [Vehiculo] asignado, o null si no tiene ninguno.
     */
    override fun getVehiculoAsignado(conductorId: String): Flow<Vehiculo?> = callbackFlow {
        val listener = col.whereEqualTo("conductorId", conductorId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                // firstOrNull: un conductor tiene como máximo un vehículo asignado
                val vehiculo = snap?.documents?.firstOrNull()?.let { doc ->
                    doc.toObject(VehiculoDto::class.java)?.toDomain(doc.id)
                }
                trySend(vehiculo)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Escucha en tiempo real la lista completa de vehículos de la flota.
     *
     * - Cualquier cambio en un documento de `/vehiculos` emite la lista completa actualizada.
     *
     * @return Flow que emite la lista de vehículos actualizada.
     */
    override fun getAll(): Flow<List<Vehiculo>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.documents?.mapNotNull { doc ->
                doc.toObject(VehiculoDto::class.java)?.toDomain(doc.id)
            } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    /**
     * Actualiza kilómetros y horas de un vehículo en Firestore.
     *
     * - Usa `FieldValue.serverTimestamp()` para `ultimaActualizacion`
     *  - Usa la hora del servidor de Firestore, evitando inconsistencias si el reloj del dispositivo del conductor está desajustado.
     * - `.await()` suspende la coroutine hasta que Firestore confirma la escritura.
     *
     * @param vehiculoId ID del vehículo a actualizar.
     * @param km Nuevo valor de kilómetros.
     * @param horas Nuevo valor de horas.
     */
    override suspend fun actualizarKmYHoras(vehiculoId: String, km: Int, horas: Float) {
        col.document(vehiculoId)
            .update("km", km, "horas", horas, "ultimaActualizacion", FieldValue.serverTimestamp())
            .await()
    }

    /**
     * Asigna un conductor a un vehículo con un `WriteBatch` atómico.
     *
     * El conductor solo puede estar asignado a un vehículo a la vez. Si ya tenía uno
     * asignado, esta operación lo libera antes de asignarle el nuevo:
     * 1. Busca el vehículo anterior de ese conductor (query a Firestore).
     * 2. Si existe y es distinto al destino, lo limpia en el batch.
     * 3. Asigna el conductor al vehículo destino en el mismo batch.
     * 4. Hace `commit()` → ambas escrituras llegan a Firestore a la vez o ninguna lo hace.
     *
     * **WriteBatch vs transacción**: el batch garantiza atomicidad de escritura pero no
     * lee datos en medio del proceso — es suficiente aquí porque la invariante "un conductor,
     * un vehículo" se mantiene con la búsqueda previa.
     *
     * @param vehiculoId ID del vehículo destino.
     * @param conductorId ID del conductor que se asigna.
     * @param conductorNombre Nombre del conductor (denormalizado en el documento del vehículo).
     */
    override suspend fun asignarConductor(vehiculoId: String, conductorId: String, conductorNombre: String) {
        // Buscamos si el conductor ya tiene un vehículo asignado distinto al destino.
        val anterior = col.whereEqualTo("conductorId", conductorId).get().await()
            .documents.firstOrNull { it.id != vehiculoId }

        val batch = Firebase.firestore.batch()

        if (anterior != null) {
            // Limpiamos los campos denormalizados del vehículo anterior.
            batch.update(anterior.reference, "conductorId", null, "conductorNombre", null)
        }

        batch.update(col.document(vehiculoId), "conductorId", conductorId, "conductorNombre", conductorNombre)
        batch.commit().await()
    }

    /**
     * Crea un vehículo nuevo con km y horas a cero y sin conductor asignado.
     *
     * Los campos `conductorId` y `conductorNombre` se omiten intencionalmente
     * para que Firestore los trate como `null` — el DTO tiene valores por defecto nullable.
     */
    override suspend fun add(matricula: String) {
        col.add(mapOf("matricula" to matricula, "km" to 0L, "horas" to 0.0)).await()
    }

    /**
     * Quita el conductor de un vehículo poniendo `conductorId` y `conductorNombre` a null.
     *
     * Un solo documento → no hace falta WriteBatch. La relación conductor↔vehículo vive
     * exclusivamente en el documento del vehículo, así que basta con limpiar sus campos.
     */
    override suspend fun quitarConductor(vehiculoId: String) {
        col.document(vehiculoId)
            .update("conductorId", null, "conductorNombre", null)
            .await()
    }
}
