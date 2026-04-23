package com.garagebase

import android.app.Application

/** El plugin google-services inicializa Firebase automáticamente al arrancar.
 * Esta clase existe para tener un punto de entrada propio donde:
 *  - conectar los emuladores locales
 *  - inicializar inyección de dependencias
*/
class GarageBaseApplication : Application()