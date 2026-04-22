# TODO — GarageBase

Lista de tareas paso a paso. Marca cada casilla al completarla. Cuando aparezca un bloqueo, duda relevante o algo aprendido durante el desarrollo, anótalo en `errors.md`.

## 1. Preparación del proyecto

- [x] Inicializar repositorio git local (`git init`) y crear repo remoto en GitHub
- [x] Añadir `.gitignore` apropiado para Android + Android Studio
- [x] Crear `README.md` con descripción, stack y cómo arrancar el proyecto
- [x] Scaffold del proyecto Android en Android Studio (Kotlin, Jetpack Compose, minSDK 27)
- [x] Definir estructura de carpetas dentro de `app/src/main/java/com/garagebase/` — decidida **feature-based + Clean Architecture + MVVM** (ver `docs/ARCHITECTURE.md`)
- [ ] Crear las carpetas vacías de la estructura con `.gitkeep`
- [ ] Primer commit y push inicial

## 2. Diseño de datos

- [ ] Decidir persistencia: Firebase (Firestore + Auth) vs alternativa (Room + backend propio)
- [ ] Diseñar el diagrama ER en Mermaid (entidades: `Conductor`, `Vehiculo`, `Incidencia`)
  - [ ] Relación `Vehiculo` — `Conductor` (un vehículo tiene un conductor asignado)
  - [ ] Relación `Vehiculo` — `Incidencia` (lista, con estado `revisada`)
  - [ ] Marcar qué `Conductor` es `gestor` (flag/rol, no entidad aparte)
- [ ] Guardar el diagrama en el README o en `docs/`

## 3. Capa de datos

- [ ] Definir modelos (`Conductor`, `Vehiculo`, `Incidencia`) en Kotlin
- [ ] Implementar repositorios con la tecnología elegida en el paso 2
- [ ] Sembrar datos de prueba (al menos: un gestor, dos conductores, dos vehículos)

## 4. Autenticación y roles

- [ ] Pantalla de login
- [ ] Distinguir sesión `conductor` vs `gestor` al entrar
- [ ] Enrutar a la vista correspondiente según el rol

## 5. Vista del conductor

- [ ] Mostrar el vehículo asignado al conductor autenticado
- [ ] Formulario semanal (viernes) para actualizar `km` y `horas`
- [ ] Añadir una nueva `incidencia` a la lista del vehículo
- [ ] Ver incidencias propias con su estado (`pendiente` / `revisada`)

## 6. Vista del gestor

- [ ] Listado de todos los vehículos con su conductor asignado
- [ ] Reasignar conductor a un vehículo
- [ ] Ver detalle del vehículo: `km`, `horas` e incidencias
- [ ] Marcar una incidencia como `revisada` (el cambio se refleja en la vista del conductor)

## 7. Calidad

- [ ] Tests unitarios de la lógica de dominio
- [ ] Lint + formato (`./gradlew lint`, ktlint/detekt si se añade)
- [ ] Prueba manual completa del flujo: gestor asigna → conductor actualiza → gestor revisa

## 8. Pendientes / mejoras futuras

- [ ] (Diferido) Aviso/notificación del gestor a conductores seleccionados — **no implementar hasta que se pida explícitamente**

## Artefactos del proyecto

- [x] `README.md` creado
- [x] `docs/errors.md` creado (se rellena según surjan incidencias de desarrollo)
- [x] `docs/ARCHITECTURE.md` creado
- [ ] Diagrama ER en Mermaid versionado (tras decidir persistencia)
