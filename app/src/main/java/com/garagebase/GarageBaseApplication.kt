package com.garagebase

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

/**
 * El plugin google-services inicializa Firebase automáticamente al arrancar.
 * Esta clase existe para tener un punto de entrada propio donde:
 *  - conectar los emuladores locales en builds de debug
 *  - inicializar inyección de dependencias (cuando se añada)
 */
class GarageBaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        connectToEmulators()
    }

    /**
     * Conecta los SDKs de Firebase al emulador local durante el desarrollo.
     *
     * `BuildConfig.DEBUG` garantiza que este bloque solo se ejecuta en builds de debug.
     * En builds de release los SDKs apuntan directamente a los servicios de Firebase en la nube.
     *
     * Usamos `127.0.0.1` (localhost del dispositivo) en lugar de `10.0.2.2`.
     * Esto requiere ejecutar `adb reverse` antes de instalar la app, pero funciona
     * igual para el AVD de Android Studio y para un móvil físico:
     *
     *   adb reverse tcp:9099 tcp:9099   ← Auth emulator
     *   adb reverse tcp:8080 tcp:8080   ← Firestore emulator
     *
     * `adb reverse` crea un túnel: el puerto del dispositivo apunta al localhost
     * del ordenador donde corren los emuladores de Firebase.
     *
     * Los puertos coinciden con los declarados en `firebase.json`:
     *   auth → 9099 | firestore → 8080
     */
    private fun connectToEmulators() {
        // Desactivado: la app apunta a Firebase real para pruebas en dispositivo físico.
        // Para volver a los emuladores locales:
        //   1. firebase emulators:start
        //   2. adb reverse tcp:9099 tcp:9099 && adb reverse tcp:8080 tcp:8080
        //   3. Descomentar las dos líneas siguientes:
        // if (BuildConfig.DEBUG) {
        //     Firebase.auth.useEmulator("127.0.0.1", 9099)
        //     Firebase.firestore.useEmulator("127.0.0.1", 8080)
        // }
    }
}
