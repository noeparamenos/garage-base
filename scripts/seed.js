/**
 * Script de siembra de datos para el emulador local de Firestore.
 *
 * Crea un conjunto de datos de prueba reproducible:
 *   - 1 gestor + 2 conductores en /conductores
 *   - 2 vehículos en /vehiculos (uno por conductor)
 *   - Varias incidencias como sub-colección de cada vehículo
 *
 * USO:
 *   cd scripts && npm install && npm run seed
 *
 * REQUISITO: el emulador debe estar corriendo antes de ejecutar este script.
 *   firebase emulators:start  (desde la raíz del proyecto)
 *
 * Los IDs son fijos y legibles para facilitar las pruebas manuales en la UI del emulador.
 */

const admin = require('firebase-admin');

// Variable de entorno que redirige firebase-admin al emulador local.
// Sin esto, el Admin SDK intentaría conectar al Firebase real en la nube.
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';

// initializeApp sin credenciales funciona porque el emulador no requiere autenticación.
// En producción esto requeriría una service account.
admin.initializeApp({ projectId: 'garage-base' });

const db = admin.firestore();

// ─── Datos de prueba ──────────────────────────────────────────────────────────

const conductores = [
  { id: 'gestor-001',     nombre: 'Carlos García',    telefono: '+34600000001', rol: 'gestor' },
  { id: 'conductor-001',  nombre: 'Ana López',         telefono: '+34600000002', rol: 'conductor' },
  { id: 'conductor-002',  nombre: 'Pedro Martínez',    telefono: '+34600000003', rol: 'conductor' },
];

const vehiculos = [
  {
    id: 'vehiculo-001',
    matricula: '1234ABC',
    km: 15000,
    horas: 320.5,
    conductorId: 'conductor-001',
    conductorNombre: 'Ana López',
    ultimaActualizacion: admin.firestore.Timestamp.fromDate(new Date('2026-04-18')),
  },
  {
    id: 'vehiculo-002',
    matricula: '5678DEF',
    km: 28000,
    horas: 541.0,
    conductorId: 'conductor-002',
    conductorNombre: 'Pedro Martínez',
    ultimaActualizacion: admin.firestore.Timestamp.fromDate(new Date('2026-04-18')),
  },
];

// Las incidencias son sub-colección de cada vehículo.
// conductorId y conductorNombre son snapshots inmutables del momento del reporte.
const incidencias = [
  {
    vehiculoId: 'vehiculo-001',
    descripcion: 'Luz de freno trasera derecha fundida',
    fecha: admin.firestore.Timestamp.fromDate(new Date('2026-04-10')),
    conductorId: 'conductor-001',
    conductorNombre: 'Ana López',
    kmAlReportar: 14200,
    revisada: true,
    fechaRevisada: admin.firestore.Timestamp.fromDate(new Date('2026-04-11')),
    kmAlRevisar: 14350,
  },
  {
    vehiculoId: 'vehiculo-001',
    descripcion: 'Ruido extraño al frenar en bajada',
    fecha: admin.firestore.Timestamp.fromDate(new Date('2026-04-18')),
    conductorId: 'conductor-001',
    conductorNombre: 'Ana López',
    kmAlReportar: 15000,
    revisada: false,
    fechaRevisada: null,
    kmAlRevisar: null,
  },
  {
    vehiculoId: 'vehiculo-002',
    descripcion: 'Nivel de aceite bajo, posible fuga',
    fecha: admin.firestore.Timestamp.fromDate(new Date('2026-04-15')),
    conductorId: 'conductor-002',
    conductorNombre: 'Pedro Martínez',
    kmAlReportar: 27500,
    revisada: false,
    fechaRevisada: null,
    kmAlRevisar: null,
  },
];

// ─── Siembra ──────────────────────────────────────────────────────────────────

async function seed() {
  console.log('🌱 Sembrando datos en el emulador...\n');

  // Usamos un batch para escribir todos los documentos en una sola operación atómica.
  // Si algo falla, no quedan datos a medias.
  const batch = db.batch();

  for (const conductor of conductores) {
    const { id, ...data } = conductor;
    batch.set(db.collection('conductores').doc(id), data);
    console.log(`  ✓ Conductor: ${data.nombre} (${data.rol})`);
  }

  for (const vehiculo of vehiculos) {
    const { id, ...data } = vehiculo;
    batch.set(db.collection('vehiculos').doc(id), data);
    console.log(`  ✓ Vehículo: ${data.matricula} → ${data.conductorNombre}`);
  }

  await batch.commit();

  // Las incidencias se escriben después del batch porque necesitan el vehiculoId
  // para construir la ruta de la sub-colección, y addDoc no se puede usar en batch.
  for (const incidencia of incidencias) {
    const { vehiculoId, ...data } = incidencia;
    await db.collection('vehiculos').doc(vehiculoId).collection('incidencias').add(data);
    console.log(`  ✓ Incidencia en ${vehiculoId}: "${data.descripcion.substring(0, 40)}..."`);
  }

  console.log('\n✅ Siembra completada. Abre http://localhost:4000 para verificar los datos.');
}

seed().catch(err => {
  console.error('❌ Error durante la siembra:', err);
  process.exit(1);
});
