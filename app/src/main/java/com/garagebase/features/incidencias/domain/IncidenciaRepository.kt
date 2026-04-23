package com.garagebase.features.incidencias.domain

import com.garagebase.core.model.Incidencia
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a los datos de las incidencias.
 *
 */
interface IncidenciaRepository {

    /**
     * Escucha en tiempo real las incidencias de un vehículo concreto.
     * Las incidencias se emiten ordenadas de más reciente a más antigua.
     *
     * @param vehiculoId ID del vehículo cuyas incidencias se quieren observar.
     * @return Flow que emite la lista actualizada cada vez que cambia alguna incidencia.
     */
    fun getDeVehiculo(vehiculoId: String): Flow<List<Incidencia>>

    /**
     * Escucha en tiempo real todas las incidencias pendientes de la flota.
     *
     * **`collectionGroup` query de Firestore**: busca en todas las sub-colecciones "incidencias" de cualquier vehículo a la vez.
     * Requiere un índice compuesto declarado en `firestore.indexes.json`.
     *
     * @return Flow que emite la lista de incidencias sin revisar, ordenadas de más antigua a más reciente.
     */
    fun getPendientes(): Flow<List<Incidencia>>

    /**
     * Añade una nueva incidencia al vehículo indicado.
     *
     * - Solo Por el conductor asignado al vehículo (Las Security Rules de Firestore lo garantizan en el servidor).
     * - Firestore genera el ID de la incidencia automáticamente.
     *
     * @param incidencia La incidencia a crear, incluyendo los snapshots del momento del reporte.
     */
    suspend fun add(incidencia: Incidencia)

    /**
     * Marca una incidencia como revisada. (usada por el gestor)
     * Recibe [kmAlRevisar] directamente
     * - el ViewModel ya tiene el vehículo cargado en memoria desde su Flow activo
     *  - Evita una lectura extra y una posible race condition, los km podrían cambiar
     * entre una lectura en el repositorio y la escritura posterior).
     * - Una vez marcada como revisada, la incidencia no puede volver a estado pendiente.
     *
     * @param vehiculoId ID del vehículo al que pertenece la incidencia.
     * @param incidenciaId ID de la incidencia a marcar.
     * @param kmAlRevisar Kilómetros actuales del vehículo en el momento de la revisión (snapshot).
     */
    suspend fun marcarRevisada(vehiculoId: String, incidenciaId: String, kmAlRevisar: Int)
}
