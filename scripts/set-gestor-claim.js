/**
 * Script para asignar el custom claim `gestor: true` a un usuario de Firebase Auth.
 *
 * ¿Por qué custom claims y no un campo en Firestore?
 * —————————————————————————————————————————————————
 * Un campo `rol: "gestor"` en Firestore podría ser leído por las reglas con get(),
 * pero hay dos problemas:
 *   1. Coste: cada evaluación de regla que comprueba el rol hace una lectura extra.
 *   2. Seguridad: si alguien consiguiera escribir en su propio documento /conductores/{uid}
 *      (por un bug en las reglas), podría escalarse a gestor él mismo.
 *
 * Los custom claims viajan dentro del JWT firmado por Firebase Auth. Solo el Admin SDK
 * puede escribirlos. El cliente nunca puede falsificarlos.
 *
 * Flujo completo:
 *   1. Este script llama a admin.auth().setCustomUserClaims(uid, { gestor: true })
 *   2. Firebase Auth guarda el claim en su base de datos y lo incluye en el JWT
 *      la próxima vez que el usuario refresca su token (automático cada ~1 hora)
 *   3. En la app, request.auth.token.gestor == true ya es verdad en las reglas
 *
 * ─── USO ──────────────────────────────────────────────────────────────────────
 *
 * Contra el emulador local:
 *   FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 node set-gestor-claim.js <uid>
 *   Ejemplo: FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 node set-gestor-claim.js gestor-001
 *
 * Contra producción (requiere descargarse la service account desde Firebase Console
 * → Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada):
 *   GOOGLE_APPLICATION_CREDENTIALS=./serviceAccountKey.json node set-gestor-claim.js <uid>
 *
 * ─── NOTA DE SEGURIDAD ────────────────────────────────────────────────────────
 * La serviceAccountKey.json tiene acceso total al proyecto Firebase.
 * Nunca la incluyas en el repositorio. Está en .gitignore por esta razón.
 */

const admin = require('firebase-admin');

// Leemos el UID del primer argumento de línea de comandos (process.argv[0] = node,
// process.argv[1] = nombre del script, process.argv[2] = primer argumento real).
const uid = process.argv[2];

if (!uid) {
  console.error('❌ Falta el UID del usuario.');
  console.error('   Uso: node set-gestor-claim.js <uid>');
  console.error('   Ejemplo (emulador):');
  console.error('     FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 node set-gestor-claim.js gestor-001');
  process.exit(1);
}

// Inicializamos el Admin SDK. En el emulador no hacen falta credenciales reales.
// En producción, GOOGLE_APPLICATION_CREDENTIALS apunta a la service account.
admin.initializeApp({ projectId: 'garage-base' });

async function asignarClaimGestor() {
  // Verificamos que el UID existe antes de intentar modificarlo.
  // Esto da un error claro ("usuario no encontrado") en lugar de un fallo silencioso.
  const user = await admin.auth().getUser(uid);

  console.log(`\n👤 Usuario encontrado: ${user.displayName || user.phoneNumber || uid}`);
  console.log(`   Claims actuales: ${JSON.stringify(user.customClaims ?? {})}`);

  // setCustomUserClaims REEMPLAZA todos los custom claims del usuario, no los fusiona.
  // Si el usuario ya tuviera otros claims (ej: { premium: true }), los perdería.
  // Por eso leemos primero los claims actuales y los fusionamos con spread.
  const claimsActuales = user.customClaims ?? {};
  await admin.auth().setCustomUserClaims(uid, { ...claimsActuales, gestor: true });

  console.log(`\n✅ Custom claim { gestor: true } asignado a UID: ${uid}`);
  console.log('');
  console.log('   Siguiente paso:');
  console.log('   El claim se aplica en el próximo refresco del token de Firebase Auth.');
  console.log('   — Automático: la app lo recibirá en ≤ 1 hora.');
  console.log('   — Forzado (en desarrollo): llama a FirebaseAuth.instance.currentUser?.getIdToken(true)');
  console.log('     o cierra y vuelve a abrir sesión en la app.');
}

asignarClaimGestor().catch(err => {
  console.error('\n❌ Error:', err.message);
  if (err.code === 'auth/user-not-found') {
    console.error(`   El UID "${uid}" no existe en Firebase Auth.`);
    console.error('   Asegúrate de que el emulador está corriendo y el usuario fue creado.');
  }
  process.exit(1);
});
