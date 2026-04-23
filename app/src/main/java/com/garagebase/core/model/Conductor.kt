package com.garagebase.core.model

/**
 * Persona que conduce un vehículo de la flota. Entidad central del dominio.
 * - Todos los usuarios de la app son conductores;
 * - El gestor es un conductor con el rol [Rol.GESTOR].
 * El [id] coincide con el UID que Firebase Auth asigna al autenticar por teléfono,
 * - Unifica autenticación e identidad de dominio en una sola clave.
 *
 * @property id Identificador único. Coincide con el UID de Firebase Auth.
 * @property nombre Nombre completo del conductor.
 * @property telefono Número de teléfono en formato E.164 (ej. +34612345678).
 * @property rol Determina los permisos del conductor dentro de la app.
 */
data class Conductor(
    val id: String,
    val nombre: String,
    val telefono: String,
    val rol: Rol
) {
    /**
     * Roles posibles de un conductor.
     * - Evita errores tipográficos al comparar roles en el resto del código.
     */
    enum class Rol {
        CONDUCTOR, // ve y actualiza su vehículo
        GESTOR // ve todos los vehículos y conductores, reasigna conductores y revisa incidencias
    }

    /**
     * Indica si el conductor es el gestor
     * - Propiedad calculada para evitar escribir `conductor.rol == Rol.GESTOR`
     */
    val esGestor: Boolean get() = rol == Rol.GESTOR
}