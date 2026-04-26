# TODO — GarageBase

Lista de tareas paso a paso. Marca cada casilla al completarla. Cuando aparezca un bloqueo, duda relevante o algo aprendido durante el desarrollo, anótalo en `errors.md`.

## 1. Preparación del proyecto

- [x] Inicializar repositorio git local (`git init`) y crear repo remoto en GitHub
- [x] Añadir `.gitignore` apropiado para Android + Android Studio
- [x] Crear `README.md` con descripción, stack y cómo arrancar el proyecto
- [x] Scaffold del proyecto Android en Android Studio (Kotlin, Jetpack Compose, minSDK 27)
- [x] Definir estructura de carpetas: **feature-based + Clean Architecture + MVVM** (ver `docs/ARCHITECTURE.md`). Las carpetas cuando cada feature tenga su primer archivo.
- [x] Primer commit y push inicial

## 2. Diseño de datos

- [x] Decidir persistencia: **Firebase (Firestore + Auth)** 
- [x] Diseñar el diagrama ER en Mermaid (modelo de dominio — entidades: `Conductor`, `Vehiculo`, `Incidencia`)
  - [x] Relación `Vehiculo` — `Conductor` (un vehículo tiene un conductor asignado)
  - [x] Relación `Vehiculo` — `Incidencia` (lista, con estado `revisada`)
  - [x] Marcar qué `Conductor` es `gestor` (flag/rol, no entidad aparte)
- [x] Diseñar el mapa de estructura Firestore a partir del ER (colecciones, sub-colecciones, qué campos denormalizar, índices previsibles).
- [x] Publicar el ER en `docs/data-model.md` y el mapa Firestore en `docs/firestore-schema.md`. El README solo enlaza al primero desde su sección "Modelo de datos"

## 3. Capa de datos

### 3.1 Configuración externa (Firebase Console)

- [x] Crear proyecto en Firebase Console
- [x] Registrar la app Android (`com.garagebase`, SHA-1 de depuración con `./gradlew signingReport`)
- [x] Descargar `google-services.json` a `app/`
- [x] Habilitar Authentication → proveedor Teléfono/SMS
- [x] Habilitar Cloud Firestore (modo producción, reglas cerradas por defecto)
- [ ] (Diferido — requiere plan Blaze) Configurar alerta de presupuesto en Google Cloud
- [ ] (Diferido) Activar Firebase App Check — protección extra contra uso fraudulento de la API; configurar cuando se prepare la release

### 3.5 SMS reales (de números de prueba a OTP real)

- [x] Registrar SHA-256 de debug en Firebase Console (Project settings → tu app → Añadir huella digital) — necesario para Play Integrity y verificación OTP silenciosa sin reCAPTCHA
- [x] Probar el flujo completo con un número de teléfono real (OTP recibido por SMS, sin números de prueba)
- [x] Verificar que `vincularPorTelefono` funciona con el número real: único doc `/conductores/{uid}` en Firestore tras el primer login
- [x] Confirmar que el gestor puede añadir conductores con su número real y que reciben el SMS al hacer login

### 3.2 Wiring en el proyecto Android

- [x] Añadir plugin `com.google.gms.google-services` en el `build.gradle.kts` raíz y aplicarlo en `:app`
- [x] Añadir Firebase BoM + `firebase-auth-ktx` + `firebase-firestore-ktx` en `libs.versions.toml` y `app/build.gradle.kts`
- [x] Crear `GarageBaseApplication : Application`, inicializar Firebase y declararla en `AndroidManifest.xml`
- [x] Instalar Firebase CLI y Firebase Local Emulator Suite (Firestore + Auth)

### 3.3 Modelos y repositorios

- [x] Definir modelos de dominio (`Conductor`, `Vehiculo`, `Incidencia`) en `core/model/` — Kotlin puro, sin imports de Firebase
- [x] Definir interfaces de repositorio por feature en `domain/`
- [x] Implementar repositorios Firestore en `data/` con *mappers* DTO↔dominio
- [x] Sembrar datos de prueba (1 gestor, 2 conductores, 2 vehículos, varias incidencias) con script reproducible

### 3.4 Seguridad y reglas

- [x] Escribir `firestore.rules` iniciales (autenticación requerida, conductor solo edita su propio vehículo, solo gestor marca `revisada`)
- [x] Configurar emulador local y escribir tests de reglas con `@firebase/rules-unit-testing`
- [x] Asignar rol `gestor` vía *custom claims* (script inicial con Firebase Admin SDK; valorar Cloud Function cuando haga falta)

## 4. Autenticación y roles

- [x] Pantalla de login
- [x] Distinguir sesión `conductor` vs `gestor` al entrar
- [x] Enrutar a la vista correspondiente según el rol

## 5. Vista del gestor

El gestor es el primer usuario que entra en la app: crea vehículos y conductores
antes de que haya nada que ver para un conductor.

- [x] GestorHome: menú con accesos a Vehículos, Conductores e Incidencias
- [x] Listado de conductores (nombre | matrícula asignada)
  - [x] Añadir conductor (nombre, teléfono)
  - [x] Pantalla de detalle del conductor: editar datos con diálogo de confirmación
  - [x] Asignar / cambiar / quitar vehículo desde el detalle del conductor (opción "Ninguno")
- [x] Listado de vehículos (matrícula | conductor asignado)
  - [x] Añadir vehículo (matrícula)
  - [x] Detalle del vehículo: km, horas, incidencias pendientes + botón histórico
  - [x] Asignar / quitar conductor desde el detalle del vehículo
  - [x] Marcar incidencia como `revisada` con diálogo de confirmación (irreversible)
- [x] Listado global de incidencias pendientes (matrícula | fecha)
  - [x] Detalle de incidencia con opción de marcar revisada
- [x] Botón "Mi vehículo" en GestorHome: el gestor accede a la vista del conductor para su propio vehículo asignado

## 6. Vista del conductor

- [x] Mostrar el vehículo asignado al conductor autenticado
- [x] Formulario para actualizar `km` y `horas` (diálogo con validación)
  - [x] Validar que los nuevos valores sean ≥ a los actuales antes de enviar
- [x] Añadir una nueva `incidencia` a la lista del vehículo
- [x] Ver incidencias propias con su estado (`pendiente` / `revisada`)

## 7. Calidad

- [ ] Tests unitarios de la lógica de dominio
- [ ] Lint + formato (`./gradlew lint`, ktlint/detekt si se añade)
- [ ] Prueba manual completa del flujo: gestor asigna → conductor actualiza → gestor revisa

### 7.1 Mantenimiento del scaffold

Warnings heredados del template de Android Studio — no bloquean pero conviene limpiarlos.

- [ ] Limpiar warnings cosméticos: import no usado en `Theme.kt`, namespace sin usar y label redundante en `AndroidManifest.xml`
- [ ] Actualizar versiones en `gradle/libs.versions.toml`:
  - Plugin Kotlin Compose `2.0.21` → `2.3.20`
  - `compose-bom` `2024.09.00` → versión actual (revisar breaking changes por el salto grande)
- [ ] Actualizar Gradle wrapper: `9.1.0` → `9.4.1`

## 8. Distribución

### 8.1 Firebase App Distribution (beta con testers — requisito previo a Google Play)

- [ ] Añadir plugin `com.google.firebase.appdistribution` en `build.gradle.kts`
- [ ] Configurar bloque `firebaseAppDistribution` en el `buildType debug` de `app/build.gradle.kts`
- [ ] Subir primera APK de debug a App Distribution (`./gradlew appDistributionUploadDebug`)
- [ ] Invitar a los 20 testers por email desde la consola de Firebase
- [ ] Que los 20 testers instalen la app (enlace directo, sin App Tester) y la usen 14 días consecutivos → requisito de Google Play cumplido

### 8.2 Google Play (producción)

- [ ] Crear keystore de release y guardarla fuera del repositorio
- [ ] Registrar el SHA-1 de release en Firebase Console (Authentication → Configuración del proyecto → tu app Android → Añadir huella digital)
- [ ] Configurar firma de release en `app/build.gradle.kts` (bloque `signingConfigs`)
- [ ] Generar APK/AAB firmado
- [ ] Publicar en Google Play (requiere 8.1 completado)

## 9. Pendientes / mejoras futuras

- [ ] (Diferido) Aviso/notificación del gestor a conductores seleccionados — **no implementar hasta que se pida explícitamente**

## 10. Multi-tenancy — cualquier persona puede crear su propia flota

Permite que cualquier usuario descargue la app, cree su flota y gestione sus conductores sin intervención manual en Firebase Console. Prerequisito para Google Play.

### 10.1 Migración de datos — nueva estructura Firestore

- [ ] Crear colección raíz `/flotas/{flotaId}` con campos: `nombre`, `gestorId`, `codigoInvitacion`, `creadaEn`
- [ ] Mover `/conductores` → `/flotas/{flotaId}/conductores`
- [ ] Mover `/vehiculos` → `/flotas/{flotaId}/vehiculos` (con su subcolección `/incidencias`)
- [ ] Escribir script de migración para mover los datos actuales a la nueva estructura
- [ ] Actualizar `firestore.indexes.json` si hay índices afectados

### 10.2 Capa de datos — repositorios

- [ ] Crear `FlotaRepository` + `FlotaRepositoryImpl`: `crear(nombre)`, `findById()`, `generarCodigo()`
- [ ] Actualizar `ConductorRepositoryImpl`: todas las rutas pasan a `/flotas/{flotaId}/conductores/...`
- [ ] Actualizar `VehiculoRepositoryImpl`: todas las rutas pasan a `/flotas/{flotaId}/vehiculos/...`
- [ ] Actualizar `IncidenciaRepositoryImpl` (subcolección sigue igual pero bajo la nueva ruta de vehiculos)
- [ ] Propagar `flotaId` desde el login al resto de repositorios (inyección en el ViewModel o singleton de sesión)

### 10.3 Seguridad — Security Rules

- [ ] Regla de aislamiento por flota: un usuario solo puede leer/escribir datos de su propia flota
- [ ] Regla de creación de flota: cualquier usuario autenticado puede crear una flota (se convierte en gestor)
- [ ] Regla de unión por código: un conductor puede unirse si el código que presenta existe en el documento de la flota
- [ ] Eliminar acceso global a `/conductores` y `/vehiculos` (rutas antiguas)

### 10.4 Autenticación — nuevo flujo de onboarding

- [ ] Detectar si es el primer login del usuario (no pertenece a ninguna flota)
- [ ] Pantalla de onboarding con dos opciones: "Crear mi flota" y "Unirme con código"
- [ ] Flujo "Crear flota": formulario con nombre de flota → crea `/flotas/{flotaId}` → usuario queda como gestor
- [ ] Flujo "Unirme con código": campo de 6 dígitos → valida contra Firestore → crea conductor en la flota correspondiente
- [ ] Eliminar el rol gestor manual vía Admin SDK (`set-gestor-claim.js`) — ya no es necesario
- [ ] Actualizar `vincularPorTelefono` para que opere dentro de `/flotas/{flotaId}/conductores`

### 10.5 UI — pantallas nuevas y ajustes

- [ ] Pantalla de onboarding (primera vez): "Crear flota" / "Tengo un código"
- [ ] Pantalla "Crear flota": campo nombre + botón crear
- [ ] Pantalla "Unirme a una flota": campo código de 6 dígitos
- [ ] GestorHome: botón para ver y compartir el código de invitación de su flota
- [ ] Ajustar `SplashScreen` para detectar si el usuario ya pertenece a una flota y enrutar correctamente

## Artefactos del proyecto

- [x] `README.md`
- [x] `docs/errors.md`
- [x] `docs/ARCHITECTURE.md` — estado actual de la arquitectura (enlaces a ADRs)
- [x] `docs/data-model.md` — diagrama ER, relaciones, invariantes
- [x] `docs/glossary.md` — glosario de términos técnicos
- [x] `docs/adr/0001-architecture-feature-based-clean-mvvm.md`
- [x] `docs/adr/0002-persistence-firebase.md`
- [x] `docs/adr/0003-authentication-phone-auth.md`
- [x] `docs/firestore-schema.md` — mapa de colecciones y denormalizaciones
- [x] `docs/screens.md` — mapa de pantallas: flujo de navegación, campos y acciones por pantalla
- [x] `firestore.rules` versionado en la raíz del repo
