#!/bin/bash
# Compila la APK de debug y la sube a Firebase App Distribution.
# Uso: ./scripts/distribute.sh "Descripción de los cambios"

set -e

NOTAS="${1:-Versión de desarrollo}"
APP_ID="1:927843193170:android:dccddb715a4071f8082a8b"
APK="app/build/outputs/apk/debug/app-debug.apk"

echo "▶ Compilando APK de debug..."
./gradlew assembleDebug

echo "▶ Subiendo a Firebase App Distribution..."
firebase appdistribution:distribute "$APK" \
  --app "$APP_ID" \
  --release-notes "$NOTAS" \

echo "✓ Listo. Siguiente paso:
      1. Entra en la consola de firebase > App Distribution
      2. Haz clic sobre la versión que quieres compartir
      3. Agrega los verificadores a los que quieres que se distribuya"
