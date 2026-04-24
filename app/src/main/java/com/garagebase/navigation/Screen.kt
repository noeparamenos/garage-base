package com.garagebase.navigation

/**
 * Rutas de navegación de la app.
 *
 * Sealed class en lugar de strings sueltos: el compilador garantiza que cubrimos
 * todos los casos y evitamos typos en las rutas.
 *
 * Patrón para rutas con argumentos:
 *   - [route] define el placeholder:  "otp/{verificationId}"
 *   - [createRoute] construye la URL: "otp/abc123"
 * Navigation extrae el argumento del backstack entry con `backStackEntry.arguments`.
 */
sealed class Screen(val route: String) {
    object Splash        : Screen("splash")
    object Login         : Screen("login")
    object GestorHome    : Screen("gestor_home")
    object ConductorHome : Screen("conductor_home")

    // Sección gestor: sub-pantallas accesibles desde GestorHome
    object GestorConductores : Screen("gestor_conductores")
    object GestorVehiculos   : Screen("gestor_vehiculos")
    object GestorIncidencias : Screen("gestor_incidencias")

    object GestorConductorDetalle : Screen("gestor_conductor/{conductorId}") {
        /** Genera la ruta concreta para el detalle de un conductor. */
        fun createRoute(conductorId: String) = "gestor_conductor/$conductorId"
    }

    object Otp : Screen("otp/{verificationId}") {
        /** Genera la ruta concreta sustituyendo el placeholder por el valor real. */
        fun createRoute(verificationId: String) = "otp/$verificationId"
    }

    object GestorVehiculoDetalle : Screen("gestor_vehiculo/{vehiculoId}") {
        /** Genera la ruta concreta para el detalle de un vehículo. */
        fun createRoute(vehiculoId: String) = "gestor_vehiculo/$vehiculoId"
    }
}
