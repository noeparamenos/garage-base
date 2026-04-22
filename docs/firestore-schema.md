# Firestore Schema

Mapa de la estructura de datos en Cloud Firestore. Describe cómo se persisten las entidades del [modelo de dominio](data-model.md): colecciones, sub-colecciones, campos denormalizados e índices necesarios.

## Estructura de colecciones

```
/conductores/{conductorId}
/vehiculos/{vehiculoId}
    /incidencias/{incidenciaId}
```

`conductorId` = UID de Firebase Auth del conductor. Los demás IDs son generados automáticamente por Firestore.

---

### /conductores/{conductorId}

```
{
  nombre:  string
  email:   string
  rol:     string     // "conductor" | "gestor"
}
```

---

### /vehiculos/{vehiculoId}

```
{
  matricula:           string
  km:                  number
  horas:               number
  conductorId:         string | null    // conductor actualmente asignado
  conductorNombre:     string | null    // denormalizado — ver nota
  ultimaActualizacion: timestamp | null
}
```

---

### /vehiculos/{vehiculoId}/incidencias/{incidenciaId}

```
{
  descripcion:     string
  fecha:           timestamp      // fecha de reporte
  conductorId:     string         // snapshot: conductor asignado al reportar
  conductorNombre: string         // snapshot: nombre en el momento del reporte
  kmAlReportar:    number         // snapshot: km del vehículo al reportar
  revisada:        boolean
  fechaRevisada:   timestamp | null   // null si pendiente
  kmAlRevisar:     number | null      // null si pendiente
}
```

Los campos `conductorId`, `conductorNombre` y `kmAlReportar` se escriben una sola vez al crear la incidencia y son inmutables. `fechaRevisada` y `kmAlRevisar` se escriben una sola vez al marcar `revisada = true`.

---

## Denormalizaciones

### `conductorNombre` en `/vehiculos`

El gestor ve el listado de todos los vehículos con su conductor asignado. Sin denormalización eso requeriría N lecturas adicionales a `/conductores` (una por vehículo). Guardando `conductorNombre` en el documento del vehículo basta con una sola query a `/vehiculos`.

**Mantenimiento:** cuando se reasigna un conductor a un vehículo, se actualiza `conductorId` y `conductorNombre` en el documento del vehículo en un batch write atómico.

### `conductorNombre` en `/incidencias`

Mismo motivo: el gestor ve el listado de incidencias de un vehículo con el nombre del conductor que las reportó. Como es un snapshot histórico, este valor nunca se actualiza aunque el conductor cambie de nombre o se reasigne el vehículo.

---

## Índices

| Query | Tipo | Índice |
|---|---|---|
| Vehículo del conductor autenticado | Campo simple (auto) | `vehiculos` → `conductorId` ASC |
| Incidencias de un vehículo ordenadas por fecha | Campo simple (auto) | `vehiculos/{id}/incidencias` → `fecha` DESC |
| **Todas las incidencias no revisadas de la flota** | **Collection group — declarar** | `incidencias` → `revisada` ASC + `fecha` ASC |

Los índices de campo simple los crea Firestore automáticamente. 
El índice de collection group hay que declararlo en `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "incidencias",
      "queryScope": "COLLECTION_GROUP",
      "fields": [
        { "fieldPath": "revisada", "order": "ASCENDING" },
        { "fieldPath": "fecha",    "order": "ASCENDING" }
      ]
    }
  ]
}
```

La query correspondiente en código:
```kotlin
Firebase.firestore
    .collectionGroup("incidencias")
    .whereEqualTo("revisada", false)
    .orderBy("fecha", Query.Direction.ASCENDING)
```

---

## Validaciones en Security Rules

Además del control de acceso, las Security Rules aplican invariantes del dominio en el servidor — segunda línea de defensa si la app tiene un bug o alguien llama a la API directamente.

```
// km y horas nunca disminuyen
allow update: if request.resource.data.km    >= resource.data.km
              && request.resource.data.horas >= resource.data.horas;

// fechaRevisada y kmAlRevisar son inmutables una vez escritos
allow update: if !("fechaRevisada" in resource.data)
              || request.resource.data.fechaRevisada == resource.data.fechaRevisada;
```

La validación también se aplica en la UI antes de enviar (primera línea de defensa: feedback inmediato al usuario sin consumir una escritura de Firestore).

## Implicaciones para Security Rules

La jerarquía de sub-colección permite reglas limpias y transitivas:

- **`/conductores/{id}`**: cualquier usuario autenticado puede leer su propio documento. Solo el gestor puede leer todos y escribir cualquiera.
- **`/vehiculos/{id}`**: cualquier usuario autenticado puede leer. Solo el conductor asignado (`conductorId == request.auth.uid`) o el gestor puede escribir.
- **`/vehiculos/{id}/incidencias/{iid}`**: hereda el acceso del vehículo padre. Solo el gestor puede hacer `update` del campo `revisada`.

Las reglas completas se implementan en `firestore.rules`.