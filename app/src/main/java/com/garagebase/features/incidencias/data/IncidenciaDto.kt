package com.garagebase.features.incidencias.data

import com.garagebase.core.model.Incidencia
import com.google.firebase.Timestamp
import java.time.Instant

/**
 * DTO (Data Transfer Object) de Incidencia para Firestore.
 *
 * Representa el documento `/vehiculos/{vehiculoId}/incidencias/{id}`
 * -`vehiculoId` NO es un campo del documento:
 *  - se infiere de la ruta de la sub-colección, no se almacena dentro.
 *  - [toDomain] lo recibe como parámetro externo.
 *
 * - [fechaRevisada] y [kmAlRevisar] son null mientras la incidencia está pendiente
 *  - se escriben una sola vez cuando el gestor la marca como revisada.
 *
 * @property descripcion Texto libre que describe el problema reportado.
 * @property fecha Fecha de reporte como [Timestamp] de Firestore.
 * @property conductorId Snapshot del conductor asignado al reportar (inmutable).
 * @property conductorNombre Snapshot del nombre del conductor al reportar (inmutable).
 * @property kmAlReportar Snapshot de los km del vehículo al reportar (inmutable).
 * @property revisada Indica si el gestor ya revisó la incidencia.
 * @property fechaRevisada Fecha en que se revisó. Null si está pendiente.
 * @property kmAlRevisar Km del vehículo cuando se revisó. Null si está pendiente.
 */
internal data class IncidenciaDto(
    val descripcion: String = "",
    val fecha: Timestamp? = null,
    val conductorId: String = "",
    val conductorNombre: String = "",
    val kmAlReportar: Long = 0,
    val revisada: Boolean = false,
    val fechaRevisada: Timestamp? = null,
    val kmAlRevisar: Long? = null
) {

    /**
     * Mapea este DTO al modelo de dominio [Incidencia].
     *
     * - Recibe [vehiculoId] porque Firestore no lo almacena dentro del documento (se deduce de la ruta `/vehiculos/{vehiculoId}/incidencias/{id}`).
     * - Si `fecha` fuera null (documento malformado), usamos [Instant.EPOCH] como fallback para no crashear (aunque no deberia pasar).
     *
     * @param id El ID del documento de Firestore.
     * @param vehiculoId El ID del vehículo padre, extraído de la ruta de la sub-colección.
     * @return La [Incidencia] equivalente en el modelo de dominio.
     */
    fun toDomain(id: String, vehiculoId: String) = Incidencia(
        id = id,
        vehiculoId = vehiculoId,
        descripcion = descripcion,
        fecha = fecha?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) } ?: Instant.EPOCH,
        conductorId = conductorId,
        conductorNombre = conductorNombre,
        kmAlReportar = kmAlReportar.toInt(),
        revisada = revisada,
        fechaRevisada = fechaRevisada?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) },
        kmAlRevisar = kmAlRevisar?.toInt()
    )
}

/**
 * Extension function: convierte una [Incidencia] del dominio a [IncidenciaDto] para Firestore.
 * - No incluye `vehiculoId` en el DTO porque Firestore lo almacena en la ruta de la sub-colección, no dentro del documento.
 *
 * @receiver La incidencia del dominio a convertir.
 * @return El [IncidenciaDto] listo para escribir en Firestore.
 */
internal fun Incidencia.toDto() = IncidenciaDto(
    descripcion = descripcion,
    fecha = Timestamp(fecha.epochSecond, fecha.nano),
    conductorId = conductorId,
    conductorNombre = conductorNombre,
    kmAlReportar = kmAlReportar.toLong(),
    revisada = revisada,
    fechaRevisada = fechaRevisada?.let { Timestamp(it.epochSecond, it.nano) },
    kmAlRevisar = kmAlRevisar?.toLong()
)
