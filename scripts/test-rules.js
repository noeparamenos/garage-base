/**
 * Tests de las Firestore Security Rules de GarageBase.
 *
 * ¿Qué testea este archivo?
 * ─────────────────────────
 * Las reglas de `firestore.rules`. Cada `it()` verifica que una operación concreta
 * está PERMITIDA (assertSucceeds) o DENEGADA (assertFails) para una identidad dada.
 *
 * Arquitectura del testing:
 * ─────────────────────────
 * - `@firebase/rules-unit-testing` arranca el emulador local de Firestore y expone
 *   una API para crear clientes con identidades simuladas (uid, claims).
 * - Los tests NO usan mocks — hablan con el emulador real. Si la regla pasa, la
 *   operación se ejecuta; si falla, el emulador devuelve PERMISSION_DENIED.
 * - `withSecurityRulesDisabled` actúa como el Admin SDK: escribe sin pasar por reglas.
 *   Lo usamos en beforeEach para sembrar el estado inicial de cada test.
 *
 * API de firebase/firestore (SDK modular):
 * ─────────────────────────────────────────
 * El SDK modular de Firebase (v9+) exporta funciones en lugar de métodos:
 *   Antes (compat):  db.collection('vehiculos').doc('v1').get()
 *   Ahora (modular): getDoc(doc(db, 'vehiculos', 'v1'))
 * Esta es la API estándar de Firebase hoy en día — los docs oficiales ya solo la usan.
 *
 * ─── REQUISITO ────────────────────────────────────────────────────────────────
 * El emulador debe estar corriendo antes de ejecutar los tests:
 *   firebase emulators:start   (desde la raíz del proyecto)
 *
 * ─── USO ──────────────────────────────────────────────────────────────────────
 *   cd scripts && npm test
 */

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// SDK modular de Firebase — funciones para operar con Firestore
const {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  addDoc,
  deleteDoc,
  collection,
} = require('firebase/firestore');

const { readFileSync } = require('fs');
const { resolve }      = require('path');

// ─── Constantes ───────────────────────────────────────────────────────────────
// UIDs fijos que coinciden con los datos sembrados en beforeEach.

const UID_GESTOR      = 'gestor-001';
const UID_CONDUCTOR_1 = 'conductor-001'; // asignado a vehiculo-001
const UID_CONDUCTOR_2 = 'conductor-002'; // asignado a vehiculo-002

const ID_VEHICULO_1    = 'vehiculo-001';
const ID_VEHICULO_2    = 'vehiculo-002';
const ID_INCIDENCIA_1  = 'incidencia-001'; // sub-colección de vehiculo-001

// ─── Entorno de test ──────────────────────────────────────────────────────────

/** @type {import('@firebase/rules-unit-testing').RulesTestEnvironment} */
let testEnv;

before(async function () {
  // El emulador puede tardar en responder la primera vez; ampliamos el timeout.
  this.timeout(15000);

  testEnv = await initializeTestEnvironment({
    projectId: 'garage-base',
    firestore: {
      // Cargamos el archivo de reglas real — los tests prueban EXACTAMENTE lo que
      // se desplegará a producción, no una versión simplificada.
      rules: readFileSync(resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: 'localhost',
      port: 8080,
    },
  });
});

beforeEach(async function () {
  this.timeout(15000);

  // clearFirestore elimina TODOS los documentos del emulador entre tests.
  // Garantiza que cada test parte de un estado limpio y predecible —
  // un test no puede contaminar a otro con datos residuales.
  await testEnv.clearFirestore();

  // Sembramos el estado inicial sin pasar por las reglas de seguridad.
  // withSecurityRulesDisabled → simula el Admin SDK: acceso total.
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();

    // Conductores
    await setDoc(doc(db, 'conductores', UID_GESTOR), {
      nombre: 'Carlos García', telefono: '+34600000001', rol: 'gestor',
    });
    await setDoc(doc(db, 'conductores', UID_CONDUCTOR_1), {
      nombre: 'Ana López', telefono: '+34600000002', rol: 'conductor',
    });
    await setDoc(doc(db, 'conductores', UID_CONDUCTOR_2), {
      nombre: 'Pedro Martínez', telefono: '+34600000003', rol: 'conductor',
    });

    // Vehículos
    await setDoc(doc(db, 'vehiculos', ID_VEHICULO_1), {
      matricula: '1234ABC', km: 15000, horas: 320.5,
      conductorId: UID_CONDUCTOR_1, conductorNombre: 'Ana López',
    });
    await setDoc(doc(db, 'vehiculos', ID_VEHICULO_2), {
      matricula: '5678DEF', km: 28000, horas: 541.0,
      conductorId: UID_CONDUCTOR_2, conductorNombre: 'Pedro Martínez',
    });

    // Incidencia de prueba en vehiculo-001
    await setDoc(
      doc(db, 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1),
      {
        descripcion: 'Luz de freno fundida',
        conductorId: UID_CONDUCTOR_1,
        conductorNombre: 'Ana López',
        kmAlReportar: 14200,
        revisada: false,
        fechaRevisada: null,
        kmAlRevisar: null,
      }
    );
  });
});

after(async function () {
  // Libera los recursos del entorno de test (cierra la conexión con el emulador).
  await testEnv.cleanup();
});

// ─── Helpers de identidad ─────────────────────────────────────────────────────
// Cada función devuelve una instancia de Firestore "disfrazada" de un usuario
// concreto. Las operaciones sobre esa instancia pasan por las reglas con ese uid y claims.

const dbGestor       = () => testEnv.authenticatedContext(UID_GESTOR, { gestor: true }).firestore();
const dbConductor1   = () => testEnv.authenticatedContext(UID_CONDUCTOR_1).firestore();
const dbConductor2   = () => testEnv.authenticatedContext(UID_CONDUCTOR_2).firestore();
const dbNoAuth       = () => testEnv.unauthenticatedContext().firestore();


// ══════════════════════════════════════════════════════════════════════════════
// TESTS: /conductores
// ══════════════════════════════════════════════════════════════════════════════

describe('/conductores', () => {
  it('conductor autenticado puede leer un conductor', async () => {
    // El caso base: cualquier usuario con sesión puede ver datos de conductores.
    await assertSucceeds(
      getDoc(doc(dbConductor1(), 'conductores', UID_CONDUCTOR_1))
    );
  });

  it('gestor autenticado puede leer cualquier conductor', async () => {
    await assertSucceeds(
      getDoc(doc(dbGestor(), 'conductores', UID_CONDUCTOR_2))
    );
  });

  it('usuario no autenticado NO puede leer conductores', async () => {
    // Sin sesión, `request.auth` es null → la función autenticado() falla.
    await assertFails(
      getDoc(doc(dbNoAuth(), 'conductores', UID_CONDUCTOR_1))
    );
  });

  it('conductor autenticado NO puede escribir en la colección de conductores', async () => {
    // La regla es `allow write: if false`. Solo el Admin SDK puede escribir aquí.
    await assertFails(
      setDoc(doc(dbConductor1(), 'conductores', 'nuevo-conductor'), {
        nombre: 'Intruso', telefono: '+34000000000', rol: 'conductor',
      })
    );
  });

  it('gestor tampoco puede escribir conductores desde el cliente', async () => {
    // Aunque sea gestor, las reglas dicen `if false`. El Admin SDK es otra historia.
    await assertFails(
      setDoc(doc(dbGestor(), 'conductores', 'nuevo-conductor'), {
        nombre: 'Gestor intentando', telefono: '+34000000001', rol: 'conductor',
      })
    );
  });
});


// ══════════════════════════════════════════════════════════════════════════════
// TESTS: /vehiculos
// ══════════════════════════════════════════════════════════════════════════════

describe('/vehiculos — lecturas', () => {
  it('conductor autenticado puede leer su vehículo', async () => {
    await assertSucceeds(
      getDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1))
    );
  });

  it('conductor autenticado puede leer el vehículo de otro conductor', async () => {
    // `allow read: if autenticado()` no restringe por conductorId.
    // La restricción está en las ESCRITURAS, no en las lecturas.
    // (El gestor necesita ver todos los vehículos; el conductor filtra en la query.)
    await assertSucceeds(
      getDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_2))
    );
  });

  it('usuario no autenticado NO puede leer vehículos', async () => {
    await assertFails(
      getDoc(doc(dbNoAuth(), 'vehiculos', ID_VEHICULO_1))
    );
  });
});

describe('/vehiculos — conductor actualiza km/horas', () => {
  it('conductor asignado puede actualizar km, horas y ultimaActualizacion', async () => {
    // Este es el flujo principal del conductor: informe semanal de km/horas.
    await assertSucceeds(
      updateDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1), {
        km: 16000,
        horas: 340.0,
        ultimaActualizacion: new Date(),
      })
    );
  });

  it('conductor asignado NO puede modificar conductorId (campo fuera de soloModifica)', async () => {
    // Un conductor no puede "robar" el vehículo de otro cambiando conductorId.
    // soloModifica(['km','horas','ultimaActualizacion']) rechaza cualquier campo extra.
    await assertFails(
      updateDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1), {
        km: 16000,
        conductorId: 'conductor-intento-robo',
      })
    );
  });

  it('conductor NO asignado NO puede actualizar el vehículo de otro', async () => {
    // conductor-002 intenta actualizar vehiculo-001, que está asignado a conductor-001.
    // resource.data.conductorId ('conductor-001') != request.auth.uid ('conductor-002')
    await assertFails(
      updateDoc(doc(dbConductor2(), 'vehiculos', ID_VEHICULO_1), {
        km: 99999,
        horas: 999.9,
        ultimaActualizacion: new Date(),
      })
    );
  });
});

describe('/vehiculos — gestor gestiona la flota', () => {
  it('gestor puede asignar conductor (actualizar conductorId)', async () => {
    // Esta operación la usa el caso de uso asignarConductor() del gestor.
    // El gestor pasa directamente por `esGestor()` sin restricción de campos.
    await assertSucceeds(
      updateDoc(doc(dbGestor(), 'vehiculos', ID_VEHICULO_1), {
        conductorId: UID_CONDUCTOR_2,
        conductorNombre: 'Pedro Martínez',
      })
    );
  });

  it('gestor puede crear un nuevo vehículo', async () => {
    await assertSucceeds(
      setDoc(doc(dbGestor(), 'vehiculos', 'vehiculo-nuevo'), {
        matricula: '9999XYZ', km: 0, horas: 0.0,
        conductorId: null, conductorNombre: null,
      })
    );
  });

  it('gestor puede eliminar un vehículo', async () => {
    await assertSucceeds(
      deleteDoc(doc(dbGestor(), 'vehiculos', ID_VEHICULO_1))
    );
  });

  it('conductor NO puede crear un vehículo', async () => {
    await assertFails(
      setDoc(doc(dbConductor1(), 'vehiculos', 'vehiculo-pirata'), {
        matricula: '0000AAA', km: 0, horas: 0.0,
      })
    );
  });

  it('conductor NO puede eliminar un vehículo', async () => {
    await assertFails(
      deleteDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1))
    );
  });
});


// ══════════════════════════════════════════════════════════════════════════════
// TESTS: /vehiculos/{id}/incidencias
// ══════════════════════════════════════════════════════════════════════════════

describe('/incidencias — lecturas', () => {
  it('conductor asignado puede leer sus propias incidencias', async () => {
    // esConductorAsignado() lee vehiculo-001 y comprueba conductorId == uid.
    await assertSucceeds(
      getDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1))
    );
  });

  it('conductor NO asignado NO puede leer incidencias de otro vehículo', async () => {
    // conductor-002 está asignado a vehiculo-002, no a vehiculo-001.
    // esConductorAsignado('vehiculo-001') → conductorId='conductor-001' != 'conductor-002'
    await assertFails(
      getDoc(doc(dbConductor2(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1))
    );
  });

  it('gestor puede leer incidencias de cualquier vehículo', async () => {
    // Cubre también el caso collectionGroup: la regla aplica igual a cada doc.
    await assertSucceeds(
      getDoc(doc(dbGestor(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1))
    );
  });

  it('usuario no autenticado NO puede leer incidencias', async () => {
    await assertFails(
      getDoc(doc(dbNoAuth(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1))
    );
  });
});

describe('/incidencias — el conductor crea incidencias', () => {
  it('conductor asignado puede crear una incidencia con su propio conductorId', async () => {
    // Flujo normal: el conductor reporta un problema en su vehículo.
    await assertSucceeds(
      addDoc(
        collection(dbConductor1(), 'vehiculos', ID_VEHICULO_1, 'incidencias'),
        {
          descripcion: 'Neumático desgastado',
          conductorId: UID_CONDUCTOR_1,   // debe coincidir con request.auth.uid
          conductorNombre: 'Ana López',
          kmAlReportar: 15500,
          revisada: false,
          fechaRevisada: null,
          kmAlRevisar: null,
        }
      )
    );
  });

  it('conductor NO puede crear una incidencia con el conductorId de otro', async () => {
    // Intenta firmar la incidencia como si fuera conductor-002.
    // `request.resource.data.conductorId == request.auth.uid` lo rechaza.
    await assertFails(
      addDoc(
        collection(dbConductor1(), 'vehiculos', ID_VEHICULO_1, 'incidencias'),
        {
          descripcion: 'Incidencia falsificada',
          conductorId: UID_CONDUCTOR_2,   // ← distinto de request.auth.uid (conductor-001)
          conductorNombre: 'Pedro Martínez',
          kmAlReportar: 15500,
          revisada: false,
          fechaRevisada: null,
          kmAlRevisar: null,
        }
      )
    );
  });

  it('conductor NO asignado NO puede crear incidencias en el vehículo de otro', async () => {
    // conductor-002 intenta añadir una incidencia a vehiculo-001.
    await assertFails(
      addDoc(
        collection(dbConductor2(), 'vehiculos', ID_VEHICULO_1, 'incidencias'),
        {
          descripcion: 'Incidencia intrusa',
          conductorId: UID_CONDUCTOR_2,
          conductorNombre: 'Pedro Martínez',
          kmAlReportar: 28500,
          revisada: false,
          fechaRevisada: null,
          kmAlRevisar: null,
        }
      )
    );
  });
});

describe('/incidencias — el gestor revisa incidencias', () => {
  it('gestor puede marcar una incidencia como revisada', async () => {
    // Flujo del gestor: revisa la incidencia y la marca.
    // soloModifica permite exactamente estos tres campos.
    await assertSucceeds(
      updateDoc(
        doc(dbGestor(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1),
        { revisada: true, fechaRevisada: new Date(), kmAlRevisar: 15200 }
      )
    );
  });

  it('gestor NO puede editar la descripción de una incidencia', async () => {
    // La descripción es un snapshot inmutable del momento del reporte.
    // soloModifica(['revisada','fechaRevisada','kmAlRevisar']) rechaza 'descripcion'.
    await assertFails(
      updateDoc(
        doc(dbGestor(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1),
        { revisada: true, descripcion: 'Descripción alterada por el gestor' }
      )
    );
  });

  it('conductor NO puede marcar una incidencia como revisada', async () => {
    // Marcar revisada es una operación exclusiva del gestor.
    await assertFails(
      updateDoc(
        doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1),
        { revisada: true, fechaRevisada: new Date(), kmAlRevisar: 15200 }
      )
    );
  });
});

describe('/incidencias — nadie puede borrar', () => {
  it('gestor NO puede borrar una incidencia', async () => {
    // Las incidencias son registro de auditoría. `allow delete: if false`
    // aplica para todos, incluido el gestor.
    await assertFails(
      deleteDoc(doc(dbGestor(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1))
    );
  });

  it('conductor NO puede borrar una incidencia', async () => {
    await assertFails(
      deleteDoc(doc(dbConductor1(), 'vehiculos', ID_VEHICULO_1, 'incidencias', ID_INCIDENCIA_1))
    );
  });
});
