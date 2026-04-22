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

### 3.2 Wiring en el proyecto Android

- [x] Añadir plugin `com.google.gms.google-services` en el `build.gradle.kts` raíz y aplicarlo en `:app`
- [x] Añadir Firebase BoM + `firebase-auth-ktx` + `firebase-firestore-ktx` en `libs.versions.toml` y `app/build.gradle.kts`
- [ ] Crear `GarageBaseApplication : Application`, inicializar Firebase y declararla en `AndroidManifest.xml`
- [ ] Instalar Firebase CLI y Firebase Local Emulator Suite (Firestore + Auth)

### 3.3 Modelos y repositorios

- [ ] Definir modelos de dominio (`Conductor`, `Vehiculo`, `Incidencia`) en `core/model/` — Kotlin puro, sin imports de Firebase
- [ ] Definir interfaces de repositorio por feature en `domain/`
- [ ] Implementar repositorios Firestore en `data/` con *mappers* DTO↔dominio
- [ ] Sembrar datos de prueba (1 gestor, 2 conductores, 2 vehículos, varias incidencias) con script reproducible

### 3.4 Seguridad y reglas

- [ ] Escribir `firestore.rules` iniciales (autenticación requerida, conductor solo edita su propio vehículo, solo gestor marca `revisada`)
- [ ] Configurar emulador local y escribir tests de reglas con `@firebase/rules-unit-testing`
- [ ] Asignar rol `gestor` vía *custom claims* (script inicial con Firebase Admin SDK; valorar Cloud Function cuando haga falta)

## 4. Autenticación y roles

- [ ] Pantalla de login
- [ ] Distinguir sesión `conductor` vs `gestor` al entrar
- [ ] Enrutar a la vista correspondiente según el rol

## 5. Vista del conductor

- [ ] Mostrar el vehículo asignado al conductor autenticado
- [ ] Formulario semanal (viernes) para actualizar `km` y `horas`
  - [ ] Validar que los nuevos valores sean ≥ a los actuales antes de enviar
  - [ ] Diálogo de confirmación antes de guardar (evitar modificaciones accidentales)
- [ ] Añadir una nueva `incidencia` a la lista del vehículo
- [ ] Ver incidencias propias con su estado (`pendiente` / `revisada`)

## 6. Vista del gestor

- [ ] Listado de todos los vehículos con su conductor asignado
- [ ] Reasignar conductor a un vehículo
- [ ] Ver detalle del vehículo: `km`, `horas` e incidencias ordenadas por fecha
- [ ] Listado global de incidencias pendientes de toda la flota, ordenadas por fecha
- [ ] Marcar una incidencia como `revisada` (el cambio se refleja en la vista del conductor)
  - [ ] Diálogo de confirmación antes de marcar (acción irreversible)

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

- [ ] Crear keystore de release y guardarla fuera del repositorio
- [ ] Registrar el SHA-1 de release en Firebase Console (Authentication → Configuración del proyecto → tu app Android → Añadir huella digital)
- [ ] Configurar firma de release en `app/build.gradle.kts` (bloque `signingConfigs`)
- [ ] Generar APK firmado y distribuirlo a conductores

## 9. Pendientes / mejoras futuras

- [ ] (Diferido) Aviso/notificación del gestor a conductores seleccionados — **no implementar hasta que se pida explícitamente**

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
- [ ] `firestore.rules` versionado en la raíz del repo
