# GarageBase

Aplicación Android para la gestión de una flota de vehículos. Cada conductor actualiza semanalmente los datos de su vehículo (kilometraje, horas e incidencias) y un gestor supervisa todo el parque y revisa las incidencias reportadas.

> Estado: en desarrollo. Ver [`docs/TODO.md`](docs/TODO.md) para el progreso por fases.

## Funcionamiento

Hay dos roles en la aplicación:

- **Conductor** — tiene un vehículo asignado. Cada viernes entra a la app y actualiza `km` y `horas`, y opcionalmente añade una nueva incidencia a la lista de su vehículo.
- **Gestor** — es también un conductor, pero además puede ver todos los vehículos, reasignar conductores, consultar todas las incidencias y marcarlas como *revisadas*. El estado *revisada* es visible para el conductor que reportó la incidencia.

## Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Build:** Gradle con Kotlin DSL
- **Package:** `com.garagebase`
- **minSdk:** 27 (Android 8.1) · **targetSdk / compileSdk:** 36
- **Persistencia:** por decidir (Firebase Firestore + Auth es el candidato principal)

## Requisitos

- Android Studio (recomendado: versión estable reciente).
- JDK 17 o superior (el proyecto usa el JetBrains Runtime incluido con Android Studio).
- Para ejecutar en dispositivo físico: modo desarrollador y depuración USB activados.

## Arrancar el proyecto

```bash
git clone git@github.com:noeparamenos/garage-base.git
cd garage-base
```

Después, desde Android Studio:

1. *File → Open* y selecciona la carpeta del repo.
2. Espera a que Gradle sincronice.
3. Selecciona un emulador o dispositivo físico y pulsa *Run*.

Alternativamente, por línea de comandos:

```bash
./gradlew assembleDebug          # compila el APK de debug
./gradlew installDebug           # instala en el dispositivo conectado
./gradlew test                   # tests unitarios
./gradlew connectedAndroidTest   # tests instrumentados (requiere emulador/dispositivo)
./gradlew lint                   # análisis estático
```

## Estructura del proyecto

```
GarageBase/
├── app/                   ← módulo de la aplicación (código, recursos, tests)
│   └── src/main/java/...
├── build.gradle.kts       ← configuración raíz del proyecto
├── settings.gradle.kts
├── gradle/                ← wrapper y catálogo de versiones
├── docs/                  ← documentación del proyecto
│   ├── ARCHITECTURE.md    ← decisión arquitectónica y su porqué
│   ├── TODO.md            ← plan de trabajo por fases
│   └── errors.md          ← registro de incidencias y sus soluciones
└── README.md
```

## Modelo de datos

El diagrama entidad-relación en Mermaid se añadirá en esta sección cuando se cierre la decisión sobre la capa de persistencia. Entidades previstas:

- `Conductor` — con un flag/rol para identificar al gestor.
- `Vehiculo` — asignado a un conductor, con `km`, `horas` y una lista de incidencias.
- `Incidencia` — con texto y estado (`pendiente` / `revisada`).

## Documentación adicional

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — decisión de arquitectura (Clean Architecture + MVVM, feature-based) con el porqué de cada elección.
- [`docs/TODO.md`](docs/TODO.md) — lista de tareas paso a paso con estado.
- [`docs/errors.md`](docs/errors.md) — incidencias técnicas encontradas y cómo se resolvieron.

## Licencia

Pendiente de definir.
