package com.garagebase

import android.app.Application

// El plugin google-services inicializa Firebase automáticamente al arrancar.
// Esta clase existe para tener un punto de entrada propio donde:
//   - conectar los emuladores locales (siguiente paso, 3.2)
//   - inicializar inyección de dependencias cuando llegue el momento
class GarageBaseApplication : Application()