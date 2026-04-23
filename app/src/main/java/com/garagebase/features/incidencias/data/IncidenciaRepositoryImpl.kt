package com.garagebase.features.incidencias.data

import com.garagebase.core.model.Incidencia
import com.garagebase.features.incidencias.domain.IncidenciaRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [IncidenciaRepository] usando Firestore.
 *
 * Las incidencias son una sub-colección de cada vehículo: `/vehiculos/{vehiculoId}/incidencias/{incidenciaId}`.
 * - Permite reglas de seguridad limpias (las incidencias heredan el acceso del vehículo padre)
 * - hace natural la query "dame las incidencias de este vehículo".
 *
 * Exceción: [getPendientes] usa `collectionGroup` para atravesar todas las sub-colecciones de incidencias a la vez (para el gestor).
 */
class IncidenciaRepositoryImpl : IncidenciaRepository {

    /** Referencia a la instancia de Firestore. Punto de entrada para todas las operaciones. */
    private val db = Firebase.firestore

    /**
     * Devuelve la referencia a la sub-colección de incidencias de un vehículo concreto.
     *
     * - Función privada auxiliar para no repetir la ruta en cada método.
     *
     * @param vehiculoId ID del vehículo cuya sub-colección se quiere referenciar.
     */
    private fun incidenciasCol(vehiculoId: String) =
        db.collection("vehiculos").document(vehiculoId).collection("incidencias")

    /**
     * Escucha en tiempo real las incidencias de un vehículo, de más reciente a más antigua.
     *
     * @param vehiculoId ID del vehículo cuyas incidencias se quieren observar.
     * @return Flow que emite la lista actualizada cada vez que cambia alguna incidencia.
     */
    override fun getDeVehiculo(vehiculoId: String): Flow<List<Incidencia>> = callbackFlow {
        val listener = incidenciasCol(vehiculoId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(IncidenciaDto::class.java)?.toDomain(doc.id, vehiculoId)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Escucha en tiempo real todas las incidencias pendientes de toda la flota.
     *
     * **`collectionGroup("incidencias")`**: query especial de Firestore que busca simultáneamente en TODAS las sub-colecciones con ese nombre,
     * independientemente del vehículo al que pertenezcan.
     * - Requiere el índice compuesto declarado en `firestore.indexes.json`.
     * - `vehiculoId` no está en el documento sino en la ruta
     * - se extrae navegando la referencia: `doc.reference.parent.parent?.id`.
     *
     * @return Flow con todas las incidencias no revisadas, de más antigua a más reciente.
     */
    override fun getPendientes(): Flow<List<Incidencia>> = callbackFlow {
        val listener = db.collectionGroup("incidencias")
            .whereEqualTo("revisada", false)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { doc ->
                    // Ruta del documento: /vehiculos/{vehiculoId}/incidencias/{id}
                    // parent → colección "incidencias" | parent.parent → documento del vehículo
                    val vehiculoId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                    doc.toObject(IncidenciaDto::class.java)?.toDomain(doc.id, vehiculoId)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Crea una nueva incidencia en la sub-colección del vehículo.
     *
     * - Usa `add()` para que Firestore genere el ID automáticamente, (como auto-increment en SQL pero distribuido).
     *
     * @param incidencia La incidencia a crear, con todos sus campos de snapshot ya rellenos.
     */
    override suspend fun add(incidencia: Incidencia) {
        incidenciasCol(incidencia.vehiculoId).add(incidencia.toDto()).await()
    }

    /**
     * Marca una incidencia como revisada, fijando la fecha y los km de revisión.
     *
     * - Usa `update()` para modificar solo los campos de revisión sin tocar el resto del documento (descripción...).
     * - `Timestamp.now()` delega la hora al momento de la escritura en el servidor.
     *
     * @param vehiculoId ID del vehículo al que pertenece la incidencia.
     * @param incidenciaId ID de la incidencia a marcar como revisada.
     * @param kmAlRevisar Km actuales del vehículo, tomados del Flow activo en el ViewModel.
     */
    override suspend fun marcarRevisada(vehiculoId: String, incidenciaId: String, kmAlRevisar: Int) {
        incidenciasCol(vehiculoId).document(incidenciaId)
            .update(
                "revisada", true,
                "fechaRevisada", Timestamp.now(),
                "kmAlRevisar", kmAlRevisar
            )
            .await()
    }
}
