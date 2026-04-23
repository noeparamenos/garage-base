package com.garagebase.core.model

import java.time.Instant

/**
 * Representa un problema reportado sobre un vehículo.
 * - La crea el conductor asignado al vehículo; el gestor la revisa y la marca como [revisada].
 *
 * **Snapshots inmutables:** se fijan en el momento del reporte y nunca cambian
 * - [conductorId], [conductorNombre] y [kmAlReportar] en el momento del reporte.
 * - [fechaRevisada] y [kmAlRevisar] cuando el gestor marca [revisada]
 *
 * @property id Identificador único generado por Firestore.
 * @property vehiculoId ID del vehículo al que pertenece esta incidencia.
 * @property descripcion Texto libre que describe el problema.
 * @property fecha Fecha y hora en que el conductor reportó la incidencia.
 * @property conductorId Snapshot: ID del conductor asignado al reportar (inmutable).
 * @property conductorNombre Snapshot: nombre del conductor al reportar (inmutable).
 * @property kmAlReportar Snapshot: kilómetros del vehículo al reportar (inmutable).
 * @property revisada Indica si el gestor ya revisó la incidencia.
 * @property fechaRevisada Fecha en que el gestor marcó la incidencia como revisada. Null si está pendiente.
 * @property kmAlRevisar Kilómetros del vehículo cuando el gestor la revisó. Null si está pendiente.
 */
data class Incidencia(
    val id: String,
    val vehiculoId: String,
    val descripcion: String,
    val fecha: Instant,
    val conductorId: String,
    val conductorNombre: String,
    val kmAlReportar: Int,
    val revisada: Boolean,
    val fechaRevisada: Instant?,
    val kmAlRevisar: Int?
)
