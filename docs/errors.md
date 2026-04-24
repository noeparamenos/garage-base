# errors — GarageBase

Registro de errores e incidencias encontradas durante el desarrollo. Cada entrada incluye el problema, la causa y la solución aplicada. Las entradas se ordenan de más antigua a más reciente.

---

1. **Problema**: Android Studio no deja crear proyecto en carpeta existente.
   Al intentar crear el proyecto con el wizard *New Project* apuntando a una carpeta con el contenido base (ej. README...), Android Studio rechaza la ubicación indicando que la dirección ya existe.
   - **Causa**: es una salvaguarda del wizard: solo permite crear proyectos en directorios inexistentes o vacíos, para evitar sobrescribir archivos del usuario. No es un bug ni un problema de permisos.
   - **Solución**: crear el proyecto en una carpeta temporal (`xxx-tmp`) y fusionar luego su contenido con la carpeta original, respetando los archivos existentes.
   - **Solución alternativa**: mover temporalmente los archivos existentes fuera, crear el proyecto en la ruta original, y restaurarlos después.

2. **Problema**: Android Studio avisa de *CRLF line separators* al commitear.
   - **Causa**: el scaffold de Android Studio genera `gradlew.bat` con CRLF (Windows lo exige en archivos `.bat`), mientras que el resto del proyecto en Linux usa LF. Sin una política declarada, git detecta la mezcla y avisa para evitar diffs falsos entre colaboradores con SO distintos.
   - **Solución**: añadir un archivo `.gitattributes` en la raíz del proyecto que declara la política por tipo de archivo — `* text=auto eol=lf` como norma general, `*.bat / *.cmd / *.ps1 text eol=crlf` para scripts de Windows, y marca explícita `binary` para `.jar`, `.png`, `.apk`, keystores, etc.

3. **Firebase Local Emulator Suite**
   - **Contexto**: al integrar Firebase en el proyecto, el flujo profesional exige poder probar las reglas de seguridad de Firestore sin tocar datos reales ni gastar cuota.
   - **Qué hace**: levanta instancias locales de Firestore y Authentication en tu máquina (`localhost:8080` y `localhost:9099`). La app se conecta a ellas en lugar de a Firebase real — puedes crear y borrar datos de prueba sin consecuencias.
   - **Por qué es el estándar**: la librería `@firebase/rules-unit-testing` solo funciona contra el emulador. Permite escribir tests que verifican que las reglas permiten o deniegan exactamente lo que deben. Sin emulador, los errores de reglas solo aparecen en runtime.
   - **Cómo arrancarlo**: `firebase emulators:start` desde la raíz. UI de inspección en `http://localhost:4000`.
   - **Instalación**: Firebase CLI vía `npm install -g firebase-tools` (requiere Node ≥ 20, gestionado con nvm). `firebase init emulators` genera `firebase.json` y `.firebaserc`, ambos versionados en el repo.

4. **Problema**: `Could not find com.google.firebase:firebase-auth-ktx`
 y errores de `Unresolved reference 'ktx'` en los repositorios tras añadir las dependencias de Firebase.
   - **Causa**: en Firebase Android SDK BOM 33+, los artefactos `-ktx` y sus paquetes  fueron fusionados en los artefactos principales y posteriormente eliminados. (En BOM 34.x ya no existen). 
   - **Solución**:
     1. En `libs.versions.toml`: renombrar `firebase-auth-ktx` → `firebase-auth` y `firebase-firestore-ktx` → `firebase-firestore` (cambiando también el campo `name` del artefacto).
     2. En `app/build.gradle.kts`: usar los alias nuevos y eliminar entradas duplicadas.
     3. En todos los repositorios de la capa `data/`: actualizar imports a `com.google.firebase.Firebase`  `com.google.firebase.firestore.firestore`.
   - **Lección**: Ante un error de resolución de dependencias, leer el changelog del SDK afectado para entender si hubo una migración de paquetes.

5. **Problema**: `Unresolved reference: BuildConfig` al compilar `GarageBaseApplication`.
   - **Causa**: a partir de AGP 8.0, la generación de `BuildConfig` está desactivada por defecto para reducir tiempos de compilación incremental. El archivo no se genera salvo que se active explícitamente.
   - **Solución**: añadir `buildConfig = true` en el bloque `buildFeatures` de `app/build.gradle.kts`.
   - **Lección**: en proyectos migrados desde AGP 7.x, `BuildConfig` se generaba siempre. En proyectos nuevos con AGP 8+ hay que activarlo si se necesita (típicamente para `BuildConfig.DEBUG` o variables de entorno de build).

6. **Problema**: Android Studio marca *"LocalContext should not be cast to Activity, use LocalActivity instead"* en un Composable.
   - **Causa**: `LocalContext.current as Activity` es un cast inseguro. En teoría el contexto podría ser un `ContextWrapper` que no es una `Activity` directamente (aunque en práctica con Single Activity siempre lo es). Compose introdujo `LocalActivity` como `CompositionLocal` específico y tipado para este propósito desde `activity-compose 1.8`.
   - **Solución**: sustituir `val activity = LocalContext.current as Activity` por `val activity = LocalActivity.current` (devuelve `Activity?`) y manejar el nullable con `activity?.let { ... }`.

7. **Problema**: `Cleartext HTTP traffic to 127.0.0.1 not permitted` al intentar conectar con los emuladores de Firebase desde un dispositivo físico.
   - **Causa**: Android 9+ bloquea todo tráfico HTTP sin cifrar por defecto. Los emuladores locales de Firebase usan HTTP (puerto 9099 y 8080), no HTTPS. La restricción aplica tanto a dispositivos físicos como al AVD.
   - **Solución**: crear `app/src/debug/res/xml/network_security_config.xml` con una excepción de cleartext para `127.0.0.1` y `10.0.2.2`, y referenciarla desde `app/src/debug/AndroidManifest.xml`. Al usar el source set `debug`, la excepción nunca llega a la build de release.
   - **Lección**: nunca añadir `android:usesCleartextTrafficPermitted="true"` en el manifest principal — afectaría a producción. La solución correcta es siempre un `network_security_config` acotado al entorno de desarrollo.

8. **Problema**: la app crashea con `Failed to connect to /127.0.0.1:9099` al probar en un dispositivo físico con la configuración del emulador activa.
   - **Causa**: `GarageBaseApplication` llama a `Firebase.auth.useEmulator("127.0.0.1", 9099)` en builds debug. En un móvil físico `127.0.0.1` apunta al propio teléfono, no al ordenador donde corren los emuladores. Sin el túnel `adb reverse`, la conexión falla y la excepción no está capturada, matando el proceso.
   - **Solución para dispositivo físico**: comentar la llamada al emulador y apuntar al Firebase real. Para volver a los emuladores: `firebase emulators:start` + `adb reverse tcp:9099 tcp:9099 && adb reverse tcp:8080 tcp:8080`.
   - **Diferencia con el error 7**: el 7 era Android bloqueando HTTP; este es la red no alcanzando el puerto. Pueden coincidir en el mismo escenario pero son errores distintos.

9. **Problema**: la app crashea con `INVALID_REFRESH_TOKEN` al cambiar del emulador a Firebase real.
   - **Causa**: el dispositivo tenía guardado un token de sesión del emulador local. Al apuntar la app a Firebase real, `getIdToken()` intenta refrescarlo contra los servidores de Google, que lo rechazan por ser un token de emulador. La excepción no estaba capturada en `checkSession()` del `AuthViewModel`, lo que mataba el proceso.
   - **Solución**: envolver `authRepository.isGestor()` en `runCatching` dentro de `checkSession()`. En el bloque `onFailure` se llama a `authRepository.signOut()` para borrar las credenciales inválidas y se emite `AuthUiState.Unauthenticated`, enviando al usuario al login sin crash.
   - **Lección**: cualquier operación de red en el arranque de la app (especialmente `getIdToken`) puede fallar. Siempre capturar la excepción y degradar con gracia en lugar de dejar que el proceso muera.

10. **Problema**: `PERMISSION_DENIED` al abrir la pantalla de incidencias del gestor aunque las reglas de Firestore sí permiten la lectura.
    - **Causa**: la regla `allow read` para `/incidencias/{id}` estaba anidada dentro de `match /vehiculos/{vehiculoId}`. Las reglas anidadas cubren lecturas directas por ruta, pero las queries `collectionGroup("incidencias")` atraviesan toda la base de datos. Firestore exige para ello una regla raíz explícita con el patrón `/{path=**}/incidencias/{id}` — sin ella, la query es rechazada incluso si el usuario tendría acceso a cada documento individual.
    - **Solución**: añadir en el nivel raíz de las reglas (dentro de `match /databases/{database}/documents`): `match /{path=**}/incidencias/{incidenciaId} { allow read: if esGestor(); }`. La regla anidada original se mantiene para cubrir el acceso directo por ruta del conductor.
    - **Lección**: las reglas anidadas y las reglas `collectionGroup` son independientes en Firestore. Necesitar ambas cuando un mismo recurso se consulta de las dos formas (ruta directa para el conductor, `collectionGroup` para el gestor).

11. **Problema**: `FAILED_PRECONDITION: The query requires an index` al abrir la pantalla de incidencias tras corregir el error de permisos.
    - **Causa**: la query `collectionGroup("incidencias").whereEqualTo("revisada", false).orderBy("fecha", ASCENDING)` combina un filtro de igualdad con una ordenación en una colección agrupada. Firestore no puede resolver este tipo de query sin un índice compuesto de scope `COLLECTION_GROUP` sobre los campos `revisada` y `fecha`. Sin el índice declarado y desplegado, la query falla aunque las reglas lo permitan.
    - **Solución**: crear `firestore.indexes.json` en la raíz del proyecto con el índice `{ collectionGroup: "incidencias", queryScope: "COLLECTION_GROUP", fields: [revisada ASC, fecha ASC] }`, referenciar el archivo en `firebase.json`, y desplegar con `firebase deploy --only firestore` (no `--only firestore:rules`, que omite los índices).
    - **Lección**: `--only firestore` despliega reglas e índices juntos; `--only firestore:rules` solo despliega las reglas. Los índices de `collectionGroup` tardan 1-2 minutos en construirse tras el despliegue.

