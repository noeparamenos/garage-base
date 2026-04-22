# ADR 0001: Feature-based + Clean Architecture + MVVM

**Fecha:** 2026-04-22
**Estado:** Aceptada

## Contexto

Proyecto Android en solitario con Kotlin + Jetpack Compose. La app modela tres áreas de negocio diferenciadas (`auth`, `vehiculos`, `incidencias`), dos roles de usuario (`conductor`, `gestor`) y prevé incorporar nuevas features sin un calendario fijo.

Restricciones relevantes al tomar la decisión:
- Un solo desarrollador.
- Lógica de negocio que debe ser testeable sin emulador (más rápido, sin dependencias de Android).
- Capa de persistencia todavía no decidida en ese momento — la arquitectura no debía asumir Firebase, Room ni ninguna tecnología concreta.

## Alternativas consideradas

**Flat por tipo** (`ui/`, `data/`, `domain/` globales en la raíz del paquete): sencillo al principio, inmanejable cuando hay más de 2–3 pantallas por capa. Archivos sin relación conviven en la misma carpeta.

**Feature-based sin capas internas**: cada feature agrupa sus archivos directamente, sin separar UI/domain/data. Más rápido de arrancar, pero mezcla responsabilidades — dificulta testear la lógica de negocio de forma aislada.

**Multi-módulo Gradle desde el inicio**: máxima separación y compilación en paralelo. Overhead de configuración desproporcionado para un proyecto con un solo módulo de producción.

## Decisión

**Feature-based por fuera + Clean Architecture por dentro + MVVM en la capa UI.**

```
com.garagebase/
├── core/model/          ← entidades de dominio compartidas entre features
├── features/<feature>/
│   ├── ui/              ← Composables + ViewModels (MVVM)
│   ├── domain/          ← casos de uso y reglas de negocio (Kotlin puro)
│   └── data/            ← repositorios y fuentes de datos
└── MainActivity.kt
```

Regla de dependencias: `ui/ → domain/ ← data/`. La capa `domain/` no importa ningún framework (`androidx`, Firebase, Compose).

Las carpetas se crean orgánicamente — no se pre-crean vacías.

## Consecuencias

**Positivas:**
- La lógica de negocio se testea con JUnit puro, sin emulador.
- Cambiar la persistencia solo toca `data/` — `domain/` y `ui/` no se enteran.
- Añadir una feature = añadir una carpeta con la misma estructura conocida.

**Negativas / compromisos:**
- Más carpetas al principio para features pequeñas; la estructura puede sentirse sobredimensionada para 1 pantalla + 1 repositorio.

**Cuándo revisitar:**
- Si aparece un segundo cliente (web, API pública): evaluar convertir `core/` en módulo Gradle compartible.
- Si una feature supera ~15 pantallas: extraerla a módulo Gradle independiente para compilación paralela.
- Si `domain/` acumula imports de frameworks: señal de contaminación que hay que limpiar.