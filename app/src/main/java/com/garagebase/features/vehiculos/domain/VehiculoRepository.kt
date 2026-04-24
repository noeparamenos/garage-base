package com.garagebase.features.vehiculos.domain

import com.garagebase.core.model.Vehiculo
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a los datos de los vehículos.
 *
 */
interface VehiculoRepository {

    /**
     * Escucha en tiempo real el vehículo asignado a un conductor.
     *
     * - `null` si el conductor no tiene ningún vehículo asignado. O Si el gestor reasigna el vehículo mientras el conductor tiene la app abierta,
     *
     *
     * @param conductorId UID del conductor cuyo vehículo se quiere observar.
     * @return Flow que emite el [Vehiculo] asignado, o null si no tiene ninguno.
     */
    fun getVehiculoAsignado(conductorId: String): Flow<Vehiculo?>

    /**
     * Escucha en tiempo real la lista completa de vehículos de la flota. (para el gestor)
     *
     * @return Flow que emite la lista actualizada cada vez que cambia algún vehículo.
     */
    fun getAll(): Flow<List<Vehiculo>>

    /**
     * Actualiza los kilómetros y horas de un vehículo. (por el conductor)
     *
     * La validación de que los nuevos valores son >= a los actuales ocurre en el ViewModel antes
     *
     * @param vehiculoId ID del vehículo a actualizar.
     * @param km Nuevo valor de kilómetros (debe ser >= al valor actual).
     * @param horas Nuevo valor de horas (debe ser >= al valor actual).
     */
    suspend fun actualizarKmYHoras(vehiculoId: String, km: Int, horas: Float)

    /**
     * Asigna un conductor a un vehículo.
     *
     * - Recibe [conductorNombre] directamente porque el ViewModel ya tiene el objeto Conductor cargado
     *  - Evitamos una lectura extra a Firestore solo para obtener el nombre.
     *
     * @param vehiculoId ID del vehículo a reasignar.
     * @param conductorId ID del nuevo conductor asignado.
     * @param conductorNombre Nombre del nuevo conductor (denormalizado en el documento del vehículo).
     */
    suspend fun asignarConductor(vehiculoId: String, conductorId: String, conductorNombre: String)

    /**
     * Añade un nuevo vehículo a la flota con km y horas a cero y sin conductor asignado.
     * El conductor se asigna después con [asignarConductor].
     *
     * @param matricula Matrícula del vehículo (p.ej. "1234 ABC").
     */
    suspend fun add(matricula: String)

    /**
     * Quita el conductor asignado a un vehículo, dejando los campos `conductorId`
     * y `conductorNombre` a null en Firestore.
     *
     * No requiere WriteBatch porque solo modifica un documento: el vehículo queda libre
     * y el conductor ya no tiene referencia al vehículo (la relación es unidireccional
     * desde el documento del vehículo).
     *
     * @param vehiculoId ID del vehículo del que se quita el conductor.
     */
    suspend fun quitarConductor(vehiculoId: String)
}
