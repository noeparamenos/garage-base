package com.garagebase.features.conductores.data

import com.garagebase.core.model.Conductor

/**
 * DTO (Data Transfer Object) de Conductor para Firestore.
 *
 * Representa el documento `/conductores/{id}` como lo almacena Firestore.
 * Existe separado del modelo de dominio [Conductor] por dos razones:
 * 1. Firestore necesita instanciar la clase automáticamente durante la deserialización,
 *  - Requiere un constructor sin argumentos (En Kotlin equivale a dar valores por defecto a todos los parámetros)
 * 2. Firestore tiene sus propios tipos de datos (Timestamp, Long, etc.)
 *  - Son distintos a los del dominio (Instant, Int, etc.).
 *  - El mapper hace la conversión. Es `internal` porque solo debe ser conocido dentro del módulo de datos (el dominio
 * y la UI no deberían saber que este DTO existe)
 *
 * @property nombre Nombre completo del conductor.
 * @property telefono Número de teléfono en formato E.164.
 * @property rol Rol como string: "conductor" o "gestor". Se convierte al enum [Conductor.Rol] en el mapper.
 */
internal data class ConductorDto(
    val nombre: String = "",
    val telefono: String = "",
    val rol: String = "conductor"
) {
    /**
     * Mapea este DTO al modelo de dominio [Conductor].
     * - Traducela representación de la capa de datos a la representación de la capa de dominio.
     * - El [id] se pasa como parámetro porque Firestore no lo incluye dentro del documento (es la key).
     *
     * @param id El ID del documento de Firestore, que coincide con el UID de Firebase Auth.
     * @return El [Conductor] equivalente en el modelo de dominio.
     */
    fun toDomain(id: String) = Conductor(
        id = id,
        nombre = nombre,
        telefono = telefono,
        rol = if (rol == "gestor") Conductor.Rol.GESTOR else Conductor.Rol.CONDUCTOR
    )
}

/**
 * Mapea un [Conductor] del dominio a su representación DTO para Firestore.
 *
 * **extension functions**: permiten añadir métodos a una clase existente sin modificarla ni heredar de ella.
 * - Añadimos `toDto()` a [Conductor] desde fuera de su definición (el dominio no sabe nada de DTOs ni de Firestore).
 *
 * @receiver El conductor del dominio a convertir.
 * @return El [ConductorDto] listo para escribir en Firestore.
 */
internal fun Conductor.toDto() = ConductorDto(
    nombre = nombre,
    telefono = telefono,
    rol = rol.name.lowercase() // Convierte CONDUCTOR → "conductor", GESTOR → "gestor"
)
