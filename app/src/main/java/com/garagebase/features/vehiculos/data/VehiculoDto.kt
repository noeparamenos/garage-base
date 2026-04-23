package com.garagebase.features.vehiculos.data

import com.garagebase.core.model.Vehiculo
import com.google.firebase.Timestamp
import java.time.Instant

/**
 * DTO (Data Transfer Object) de Vehiculo para Firestore.
 *
 * Representa el documento `/vehiculos/{id}` de Firestore.
 * Firestore tiene sus propios tipos:
 * - Números enteros → `Long`  (el dominio usa `Int`)
 * - Números decimales → `Double`  (el dominio usa `Float`)
 * - Fechas → [Timestamp] (el dominio usa [java.time.Instant])
 *
 * Los valores por defecto permiten a Firestore instanciar el DTO sin constructor explícito durante la deserialización automática.
 *
 * @property matricula Matrícula del vehículo.
 * @property km Kilómetros acumulados. Firestore lo almacena como Long.
 * @property horas Horas de uso acumuladas. Firestore lo almacena como Double.
 * @property conductorId ID del conductor asignado. Null si no tiene ninguno.
 * @property conductorNombre Nombre del conductor, denormalizado para evitar lecturas adicionales.
 * @property ultimaActualizacion Fecha de la última actualización de km/horas. Null si no ha habido ninguna.
 */
internal data class VehiculoDto(
    val matricula: String = "",
    val km: Long = 0,
    val horas: Double = 0.0,
    val conductorId: String? = null,
    val conductorNombre: String? = null,
    val ultimaActualizacion: Timestamp? = null
) {
    /**
     * Mapea este DTO al modelo de dominio [Vehiculo].
     *
     * Convierte los tipos de Firestore a los tipos del dominio:
     * - `Long` → `Int` para km
     * - `Double` → `Float` para horas
     * - [Timestamp] → [Instant] para la fecha (usando segundos y nanosegundos de época Unix)
     *
     * @param id El ID del documento de Firestore.
     * @return El [Vehiculo] equivalente en el modelo de dominio.
     */
    fun toDomain(id: String) = Vehiculo(
        id = id,
        matricula = matricula,
        km = km.toInt(),
        horas = horas.toFloat(),
        conductorId = conductorId,
        conductorNombre = conductorNombre,
        // La expresión `?.let { }` solo ejecuta el bloque si ultimaActualizacion no es null
        ultimaActualizacion = ultimaActualizacion?.let {
            Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong())
        }
    )
}

/**
 * Extension function: Mapea un [Vehiculo] del dominio a [VehiculoDto] para Firestore.
 *
 * @receiver El vehículo del dominio a convertir.
 * @return El [VehiculoDto] listo para escribir en Firestore.
 */
internal fun Vehiculo.toDto() = VehiculoDto(
    matricula = matricula,
    km = km.toLong(),
    horas = horas.toDouble(),
    conductorId = conductorId,
    conductorNombre = conductorNombre,
    ultimaActualizacion = ultimaActualizacion?.let { Timestamp(it.epochSecond, it.nano) }
)
