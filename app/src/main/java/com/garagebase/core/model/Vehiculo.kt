package com.garagebase.core.model

import java.time.Instant

/**
 * Vehículo de la flota.
 * - Estado actual del vehículo: kilómetros, horas de uso y conductor asignado.
 *
 * **Denormalización:** [conductorNombre] se almacena aquí además de en el documento
 * del conductor.
 * - Firestore no soporta JOINs, así que guardar el nombre evita tener
 * que leer N documentos de conductor para mostrar la lista de vehículos (problema N+1).
 *
 * @property id Identificador único generado por Firestore.
 * @property matricula Matrícula del vehículo.
 * @property km Kilómetros acumulados. Solo puede aumentar entre actualizaciones.
 * @property horas Horas de uso acumuladas. Solo puede aumentar entre actualizaciones.
 * @property conductorId ID del conductor actualmente asignado. Null si no tiene ninguno.
 * @property conductorNombre Nombre del conductor asignado, denormalizado para evitar lecturas extra.
 *                           Null si no hay conductor asignado.
 * @property ultimaActualizacion Fecha y hora de la última actualización de km/horas.
 *                               Null si el conductor aún no ha hecho ninguna actualización.
 */
data class Vehiculo(
    val id: String,
    val matricula: String,
    val km: Int,
    val horas: Float,
    val conductorId: String?,
    val conductorNombre: String?,
    val ultimaActualizacion: Instant?
)
