# Arquitectura

Estado actual de la organización del código. Para el razonamiento detrás de cada decisión, ver [`docs/adr/`](adr/).

## Estructura de paquetes

```
com.garagebase/
├── core/
│   └── model/                   ← entidades de dominio compartidas
├── features/
│   ├── auth/
│   │   ├── ui/                  ← Composables + ViewModels
│   │   ├── domain/              ← casos de uso, reglas de negocio
│   │   └── data/                ← repositorios, fuentes de datos
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

> Las carpetas se crean orgánicamente — no se pre-crean vacías.

## Regla de dependencias

```
ui/  ──►  domain/  ◄──  data/
```

`domain/` es Kotlin puro: sin imports de `androidx`, Firebase ni Compose. Testeable con JUnit sin emulador.

## Decisiones de arquitectura

| Decisión | ADR |
|---|---|
| Feature-based + Clean Architecture + MVVM | [ADR 0001](adr/0001-architecture-feature-based-clean-mvvm.md) |
| Firebase (Firestore + Auth) como persistencia | [ADR 0002](adr/0002-persistence-firebase.md) |