# ADR 0002: Firebase (Firestore + Auth) como capa de persistencia

**Fecha:** 2026-04-22
**Estado:** Aceptada

## Contexto

La app necesita:
- Sincronización en tiempo real entre dos roles: el conductor escribe, el gestor lee y modifica, el conductor ve los cambios.
- Autenticación con roles diferenciados (`conductor` / `gestor`).
- Modo offline tolerable: una escritura semanal por conductor; si no hay red, que funcione al recuperarla.
- Volumen reducido: una flota, decenas de conductores, una escritura semanal por conductor.
- Sin infraestructura propia que desplegar ni mantener.

## Alternativas consideradas

**Supabase (Postgres + Auth + Realtime):** Postgres de verdad — FK, JOINs, esquema relacional limpio. Row Level Security para roles. El SDK de Android (`supabase-kt`) es significativamente menos maduro que Firebase en 2026; comunidad Android mucho más pequeña; escasez de recursos específicos para Android.

**Room + backend propio (Ktor/Spring + Retrofit):** control total. Exige diseñar y mantener el backend (despliegue, auth con JWT, renovación de tokens, sincronización offline a mano). Desvío desproporcionado para un proyecto centrado en la app Android con un solo desarrollador.

## Decisión

**Firebase: Cloud Firestore + Firebase Authentication.**

- **Firestore** para persistencia: sincronización en tiempo real con `addSnapshotListener`, caché offline automática del SDK, modelo de sub-colecciones que encaja con la jerarquía `Vehiculo → Incidencias`.
- **Firebase Auth** con Phone Auth (SMS OTP) como proveedor — ver ADR 0003. *Custom claims* para el rol `gestor`, verificables en las Security Rules del servidor sin roundtrip adicional.
- El `id` del documento `Conductor` coincide con el `uid` de Firebase Auth — autenticación e identidad de dominio comparten la misma clave.

## Consecuencias

**Positivas:**
- Sincronización gestor↔conductor resuelta por el SDK, sin código propio.
- Auth + control de acceso por rol disponibles tanto en el cliente como en las Security Rules.
- Plan gratuito cubre ampliamente la escala actual y previsible.
- La decisión es reversible: Firestore vive solo en `data/` (ADR 0001). Migrar = nuevas implementaciones de `Repository`, sin tocar `domain/` ni `ui/`.

**Negativas / compromisos:**
- **NoSQL sin JOINs.** Relaciones resueltas con sub-colecciones, referencias por ID o denormalización deliberada. Ver `docs/firestore-schema.md`.
- **Security Rules:** DSL propietario que hay que aprender y testear con `@firebase/rules-unit-testing` y el emulador local de Firebase.
- **Custom claims para `gestor`** requieren Firebase Admin SDK. Inicialmente un script manual; se valorará una Cloud Function si el número de gestores crece.
- **Vendor lock-in** con Google, mitigado por la separación de capas (ADR 0001).

**Cuándo revisitar:**
- Si aparecen requisitos de queries relacionales complejas (reporting, agregaciones) que Firestore no puede cubrir.
- Si el coste de Firebase escala de forma inesperada. Configurar alertas de presupuesto desde el primer día.