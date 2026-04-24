package com.garagebase.features.conductores.domain

import com.garagebase.core.model.Conductor
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a los datos de los conductores.
 *
 * Define QUÉ operaciones necesita el dominio, sin saber CÓMO se implementan.
 * - El dominio y la UI solo conocen esta interfaz.
 * - Si en el futuro se cambia de tecnología de almacenamiento, solo cambia la implementación.
 *
 * **Flow vs suspend:** las lecturas devuelven [Flow]
 * - Firestore emite actualizaciones en tiempo real cada vez que cambia un documento
 * - Flow propaga esos cambios a la UI automáticamente.
 * - Las escrituras usan `suspend` porque son operaciones puntuales que esperan confirmación del servidor antes de continuar.
 */
interface ConductorRepository {

    /**
     * Escucha en tiempo real el perfil de un conductor
     * - Emite un nuevo valor cada vez que el documento cambia en Firestore.
     * - Emite `null` si el documento no existe (ej: en el primer login antes de que se haya creado el conductor).
     *
     * El Flow se cancela automáticamente cuando el observador deja de escuchar
     * (ej: al salir de la pantalla), cerrando también el listener de Firestore.
     *
     * @param id UID de Firebase Auth del conductor.
     * @return Flow que emite el [Conductor] actualizado, o null si no existe.
     */
    fun findById(id: String): Flow<Conductor?>

    /**
     * Escucha en tiempo real la lista completa de conductores.
     * - Solo el gestor usará esta función. (Las Security Rules de Firestore
     * denegarán la lectura a un conductor normal)
     *
     * @return Flow que emite la lista actualizada cada vez que cambia algún conductor.
     */
    fun getAll(): Flow<List<Conductor>>

    /**
     * Crea un nuevo conductor con ID generado por Firestore y rol `conductor`.
     *
     * El ID resultante NO coincide con el UID de Firebase Auth porque el conductor aún
     * no ha autenticado. La vinculación Auth↔Firestore se resolverá al primer login
     * buscando el documento por [telefono] — fuera del alcance de esta sección.
     *
     * @param nombre Nombre completo del conductor.
     * @param telefono Teléfono en formato E.164 (+34...).
     * @return ID del documento creado.
     */
    suspend fun add(nombre: String, telefono: String): String

    /**
     * Actualiza el nombre y teléfono de un conductor existente.
     *
     * @param id ID del documento del conductor en Firestore.
     * @param nombre Nuevo nombre.
     * @param telefono Nuevo teléfono.
     */
    suspend fun update(id: String, nombre: String, telefono: String)
}
