# Glosario

Términos del stack técnico de GarageBase.

| Término | Descripción |
|---|---|
| **Composable** | Función con `@Composable` que describe una parte de la UI en Jetpack Compose. Se *recompone* automáticamente cuando su estado cambia. |
| **ViewModel** | Clase de Android que sobrevive a rotaciones de pantalla y mantiene el estado de la UI. Expone `StateFlow` a los Composables. |
| **StateFlow** | Tipo de Kotlin Flow con un valor actual siempre disponible — ideal para representar el estado de una pantalla en MVVM. |
| **Repositorio** | Clase que abstrae el origen de los datos. El dominio solicita `vehiculos` sin saber si vienen de Firebase, Room o una lista en memoria. |
| **Caso de uso** (*use case / interactor*) | Clase con una única responsabilidad de negocio, p. ej. `MarcarIncidenciaComoRevisada`. Vive en `domain/`. |
| **Clean Architecture** | Organización en capas concéntricas donde las dependencias apuntan siempre hacia el dominio (capa interior). |
| **MVVM** (*Model-View-ViewModel*) | Patrón de UI estándar en Android con Compose. El ViewModel mantiene el estado; el Composable lo pinta y notifica intenciones. |
| **ADR** (*Architecture Decision Record*) | Documento corto que registra una decisión arquitectónica: contexto, alternativas, decisión y consecuencias. Ver `docs/adr/`. |
| **Custom claims** | Metadatos adjuntos al token de Firebase Auth (p. ej. `rol: "gestor"`). Las Security Rules los leen para controlar el acceso por rol. |
| **Security Rules** | DSL de Firebase que define qué operaciones puede realizar cada usuario sobre cada documento de Firestore, evaluado en el servidor. |
| **Emulador local** | Firebase Local Emulator Suite — ejecuta Firestore y Auth en local para desarrollo y tests sin tocar el proyecto de producción. |