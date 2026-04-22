# Arquitectura

Este documento explica cómo está estructurado el código de la aplicación.
- Permite entender la estructura y situación de cada archivo.

## Decisión resumida

> **Feature-based por fuera + Clean Architecture por dentro, con MVVM en la capa UI.**

```
com.garagebase/
├── core/
│   └── model/                   ← Entidades de dominio compartidas
│       ├── Conductor.kt
│       ├── Vehiculo.kt
│       └── Incidencia.kt
├── features/
│   ├── auth/
│   │   ├── ui/                  ← Composables + ViewModels   (MVVM)
│   │   ├── domain/              ← Casos de uso, reglas de negocio
│   │   └── data/                ← Repositorios, fuentes de datos
│   ├── vehiculos/
│   │   ├── ui/
│   │   ├── domain/
│   │   └── data/
│   └── incidencias/
│       ├── ui/
│       ├── domain/
│       └── data/
└── MainActivity.kt
```

## Por qué estas tres capas

Las capas separan el código por **responsabilidad técnica**. Cada una responde a una pregunta distinta:

| Capa | Responsabilidad | Contenido                                                             |
|---|---|-----------------------------------------------------------------------|
| `ui/` | Pintar en pantalla y reaccionar al usuario | Composables, ViewModels, estados de UI                                |
| `domain/` | Reglas de negocio de la app | Casos de uso (p. ej. `MarcarIncidenciaComoRevisada`), entidades puras |
| `data/` | De dónde salen y a dónde van los datos | Repositorios, clientes de Firebase/Room, mappers                      |

### Regla de dependencias

**La UI conoce el dominio. El dominio no conoce la UI. El dominio no conoce los datos. Los datos conocen el dominio.** (las flechas de dependencia apuntan siempre hacia adentro, hacia el dominio.)

```
ui/  ──► domain/  ◄──  data/
```

- En `domain/` **no puede haber** imports de `androidx`, ni de Firebase, ni de Compose. Es Kotlin puro (permite testearlo rápido y sin emulador).

### Beneficios

- **Sustituibilidad**: cambiar Firebase por Room solo toca `data/`. La UI y el dominio no se enteran.
- **Testabilidad**: las reglas de negocio se testean con JUnit puro.
- **Una sola razón para cambiar cada archivo**

## feature-based por fuera

Las features agrupan el código por **área de negocio**.

Cada carpeta dentro de `features/` contiene todo lo necesario para que esa funcionalidad exista:

- **Cohesión**: todo lo de vehículos vive en `features/vehiculos/`...
- **Navegación cognitiva**: en Android Studio no te pierdes en un `ui/` gigante con 40 archivos de cosas distintas.
- **Crecimiento**: añadir una feature = añadir una carpeta.
- **Modularización futura**: si algún día convertimos `vehiculos` en un módulo Gradle separado para compilar más rápido, la estructura ya está preparada.

## `core/model/` aparte 

Las entidades del modelo (ej. `Conductor`) se usan desde varias features. Por eso viven en `core/` como "lenguaje común" del proyecto y no dentro de una feature concreta

- algo entra en `core/` solo si lo usan **varias** features. Si solo lo usa una, vive dentro de esa feature.

## Patrón MVVM dentro de `ui/`

MVVM = **M**odel - **V**iew - **V**iew**M**odel (patrón estándar de Android con Compose).

- **Model**: los datos (vienen de `domain/` vía los casos de uso).
- **View**: el Composable. Pinta y notifica intenciones ("el usuario ha pulsado este botón").
- **ViewModel**: intermediaro. Contiene el **estado** de la pantalla (como un `StateFlow`) y las funciones que la View puede invocar. Sobrevive a los cambios de configuración (p. ej. rotación de pantalla).

Flujo típico:

```
Usuario pulsa botón
       ▼
   Composable llama a viewModel.accionX()
       ▼
   ViewModel ejecuta un caso de uso de domain/
       ▼
   Caso de uso llama a un repositorio de data/
       ▼
   El resultado actualiza el StateFlow del ViewModel
       ▼
   El Composable se recompone automáticamente con el nuevo estado
```

## Glosario mínimo

| Término | Descripción                                                                                      |
|---|--------------------------------------------------------------------------------------------------|
| **Composable** | Función con `@Composable` que describe una parte de la UI en Jetpack Compose. Se *recompone* cuando su estado cambia. |
| **ViewModel** | Clase de Android que sobrevive a rotaciones y mantiene el estado de la pantalla. Expone `StateFlow` a los Composables. |
| **StateFlow** | Tipo de Kotlin Flow que siempre tiene un valor actual — ideal para representar el estado de una pantalla. |
| **Repositorio** | Clase que abstrae "de dónde vienen los datos". El dominio le pide `vehiculos` sin saber si salen de Firebase, Room, o memoria. |
| **Caso de uso** (*use case* / *interactor*) | Clase con una sola acción de negocio, p. ej. `MarcarIncidenciaComoRevisada`. Vive en `domain/`.  |
| **Clean Architecture** | Organización en capas concéntricas donde las dependencias apuntan hacia adentro, al dominio.     |
| **ADR** (*Architecture Decision Record*) | Documento corto que registra una decisión arquitectónica y su porqué. Este archivo funciona como ADR inicial. |

## Cuándo revisitar esta decisión

- Si aparece un segundo cliente (p. ej. app del gestor como versión web): replantear si `core/` debería ser un módulo Gradle compartible.
- Si una feature crece hasta tener 15+ pantallas: plantear convertirla en módulo independiente.
- Si cambiamos de stack de persistencia: solo debería afectar a `data/`. Si afecta a más capas, es una señal de que alguna capa se ha contaminado y hay que limpiar.
