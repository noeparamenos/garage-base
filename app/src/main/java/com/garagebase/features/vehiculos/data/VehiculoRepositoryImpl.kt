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
     * Asigna un conductor a un vehículo actualizando los campos denormalizados.
     *
     * Actualiza `conductorId` y `conductorNombre` en el mismo documento.
     * - En Firestore, una llamada a `update()` con múltiples campos es atómica (como transacción).
     * - Garantiza que nunca quede el documento en un estado inconsistente (ID de un conductor pero el nombre del anterior).
     *
     * @param vehiculoId ID del vehículo a reasignar.
     * @param conductorId ID del nuevo conductor.
     * @param conductorNombre Nombre del nuevo conductor (denormalizado).
     */
    override suspend fun asignarConductor(vehiculoId: String, conductorId: String, conductorNombre: String) {
        col.document(vehiculoId)
            .update("conductorId", conductorId, "conductorNombre", conductorNombre)
            .await()
    }
}
