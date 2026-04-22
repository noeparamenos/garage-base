# errors — GarageBase

Registro de errores e incidencias encontradas durante el desarrollo. Cada entrada incluye el problema, la causa y la solución aplicada. Las entradas se ordenan de más reciente a más antigua.

---


1. **Problema**: Android Studio no deja crear proyecto en carpeta existente.
Al intentar crear el proyecto con el wizard *New Project* apuntando a una carpeta con el contenido base (ej. README...), 
Android Studio rechaza la ubicación indicando que la dirección ya existe.
   - **Causa**:Es una salvaguarda del wizard: solo permite crear proyectos en directorios inexistentes o vacíos, para evitar sobrescribir archivos del usuario. No es un bug ni un problema de permisos.
   - **Solución**: Crear el proyecto en una carpeta temporal (`xxx-tmp`) y fusionar luego su contenido con la carpeta original, respetando los archivos exitente. 
   - **Solución Alternativa:** mover temporalmente los archivos existentes fuera, crear el proyecto en la ruta original, y restaurarlos después.

2. **Problema**: Android Studio avisa de *CRLF line separators* al commitear.
   - **Causa**: el scaffold de Android Studio genera `gradlew.bat` con CRLF (Windows lo exige en archivos `.bat`), mientras que el resto del proyecto en Linux usa LF. Sin una política declarada, git detecta la mezcla y avisa para evitar diffs falsos entre colaboradores con SO distintos.
   - **Solución**: añadir un archivo `.gitattributes` en la raíz del proyecto que declara la política por tipo de archivo — `* text=auto eol=lf` como norma general, `*.bat / *.cmd / *.ps1 text eol=crlf` para scripts de Windows, y marca explícita `binary` para `.jar`, `.png`, `.apk`, keystores, etc.
3.  **Firebase Local Emulator Suite?**
    - **Contexto**: al integrar Firebase en el proyecto, el flujo profesional exige poder probar las reglas de seguridad de Firestore sin tocar datos reales ni gastar cuota.
    - **Qué hace**: levanta instancias locales de Firestore y Authentication en tu máquina (`localhost:8080` y `localhost:9099`). La app se conecta a ellas en lugar de a Firebase real — puedes crear y borrar datos de prueba sin consecuencias.
    - **Por qué es el estándar**: la librería `@firebase/rules-unit-testing` solo funciona contra el emulador. Permite escribir tests que verifican que las reglas permiten o deniegan exactamente lo que deben. Sin emulador, los errores de reglas solo aparecen en runtime.
    - **Cómo arrancarlo**: `firebase emulators:start` desde la raíz. UI de inspección en `http://localhost:4000`.
    - **Instalación**: Firebase CLI vía `npm install -g firebase-tools` (requiere Node ≥ 20, gestionado con nvm). `firebase init emulators` genera `firebase.json` y `.firebaserc`, ambos versionados en el repo.

---
