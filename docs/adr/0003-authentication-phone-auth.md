# ADR 0003: Firebase Phone Auth como mecanismo de autenticación

**Fecha:** 2026-04-22
**Estado:** Aceptada

## Contexto

- La app es de uso interno (flota privada). Los conductores reciben la APK directamente (sideloading), no desde Play Store.
- El gestor gestiona la lista de conductores a partir de su agenda de contactos — conoce el teléfono de cada conductor, pero no necesariamente su email.
- El perfil de usuario objetivo (conductores de flota) hace preferible un sistema sin contraseña: menos fricción, sin credenciales que olvidar.
- El gestor debe controlar quién tiene acceso: un conductor no puede auto-registrarse.

## Alternativas consideradas

**Email/contraseña:** Requiere que el gestor conozca el email de cada conductor (no siempre disponible) y que cada conductor recuerde una contraseña. Peor UX para el perfil objetivo.

**ID de empleado + contraseña asignada por el gestor:** Sin coste de SMS, pero mantiene el problema de credenciales que olvidar y obliga a implementar el flujo de cambio de contraseña.

## Decisión

**Firebase Phone Auth (SMS OTP).**

- El gestor añade al conductor introduciendo su teléfono, opcionalmente desde el picker de contactos del sistema (`READ_CONTACTS`), y se crea su documento en `/conductores`.
- Al primer login, el conductor introduce su número → Firebase envía un SMS con código OTP de 6 dígitos (válido ~5 min).
- Tras autenticarse, la sesión persiste en el dispositivo. No se requiere re-autenticación al abrir la app.
- **Control de acceso:** cualquier número puede solicitar un OTP, pero tras autenticarse la app verifica que el `uid` resultante exista en `/conductores`. Si no está, se deniega el acceso y se cierra la sesión. Solo entran los números que el gestor ha añadido previamente.

## Requisitos técnicos derivados

- **SHA-1 registrado en Firebase Console:** obligatorio para Phone Auth en APK sideloaded. Sin él, Firebase recurre a un flujo reCAPTCHA web confuso para el usuario objetivo. Se registran el SHA-1 de debug (`./gradlew signingReport`) y el de release.
- **Permiso `READ_CONTACTS`** en `AndroidManifest.xml`: para el picker de agenda en la pantalla de alta de conductor del gestor.

## Consecuencias

**Positivas:**
- Sin contraseñas que gestionar ni olvidar.
- El gestor puede añadir conductores directamente desde su agenda.
- Sesión persistente: el conductor se autentica una vez por dispositivo.

**Negativas / compromisos:**
- **Coste SMS:** 10.000 verificaciones/mes en capa gratuita; luego ~0,01–0,06 €/SMS según país. Para una flota de decenas de conductores con autenticación poco frecuente, el coste es despreciable — revisar si la escala cambia.
- **SHA-1 requerido:** añade un paso al setup inicial y al proceso de firma de release.
- **Dependencia de cobertura SMS:** si el conductor no tiene señal al instalar, no puede autenticarse la primera vez. Mitigado porque la autenticación solo ocurre una vez por dispositivo.

**Cuándo revisitar:**
- Si los conductores necesitan acceder desde múltiples dispositivos simultáneamente (Phone Auth vincula la sesión al dispositivo).
- Si el volumen de re-autenticaciones (reinstalaciones, cambios de dispositivo) genera un coste SMS significativo.
