package com.garagebase.features.gestor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.garagebase.core.model.Incidencia
import com.garagebase.core.model.Vehiculo
import com.garagebase.ui.theme.GarageBaseTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Listado global de incidencias pendientes de toda la flota.
 *
 * Usa `collectionGroup("incidencias")` de Firestore (via [GestorIncidenciasViewModel]) para
 * buscar incidencias en todos los vehículos a la vez — sin recorrer vehículo a vehículo.
 *
 * El detalle se muestra como [AlertDialog] inline en lugar de una pantalla separada:
 * la información es compacta y no justifica una entrada en el backstack.
 *
 * @param navController Para el botón de retroceso.
 * @param viewModel ViewModel inyectado automáticamente por Compose.
 */
@Composable
fun IncidenciasGestorScreen(
    navController: NavHostController,
    viewModel: GestorIncidenciasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val seleccionada by viewModel.seleccionada.collectAsState()

    IncidenciasGestorContent(
        uiState = uiState,
        seleccionada = seleccionada,
        onBack = { navController.popBackStack() },
        onSeleccionar = viewModel::seleccionar,
        onCerrarDetalle = viewModel::cerrarDetalle,
        onMarcarRevisada = viewModel::marcarRevisada
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidenciasGestorContent(
    uiState: IncidenciasGestorUiState,
    seleccionada: Incidencia?,
    onBack: () -> Unit,
    onSeleccionar: (Incidencia) -> Unit,
    onCerrarDetalle: () -> Unit,
    onMarcarRevisada: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incidencias pendientes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is IncidenciasGestorUiState.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is IncidenciasGestorUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.mensaje, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }

            is IncidenciasGestorUiState.Ok -> {
                if (uiState.pendientes.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay incidencias pendientes.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        items(uiState.pendientes, key = { it.id }) { incidencia ->
                            val matricula = uiState.vehiculos
                                .firstOrNull { it.id == incidencia.vehiculoId }
                                ?.matricula ?: "—"

                            IncidenciaListItem(
                                incidencia = incidencia,
                                matricula = matricula,
                                onClick = { onSeleccionar(incidencia) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Detalle y confirmación de revisión en un dialog inline
    if (seleccionada != null && uiState is IncidenciasGestorUiState.Ok) {
        val matricula = uiState.vehiculos
            .firstOrNull { it.id == seleccionada.vehiculoId }
            ?.matricula ?: "—"

        DetalleIncidenciaDialog(
            incidencia = seleccionada,
            matricula = matricula,
            onMarcarRevisada = onMarcarRevisada,
            onCerrar = onCerrarDetalle
        )
    }
}

/**
 * Fila del listado de incidencias pendientes.
 *
 * El orden de los campos (matrícula → descripción → fecha) sigue la lectura del gestor:
 * primero identifica de qué vehículo se trata, luego el problema, luego cuándo ocurrió.
 *
 * @param matricula Matrícula del vehículo al que pertenece la incidencia.
 */
@Composable
private fun IncidenciaListItem(
    incidencia: Incidencia,
    matricula: String,
    onClick: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
    ListItem(
        headlineContent = { Text(matricula) },
        supportingContent = {
            Column {
                Text(incidencia.descripcion)
                Text(
                    fmt.format(incidencia.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Diálogo de detalle de una incidencia pendiente con opción de marcarla revisada.
 *
 * Se avisa de que la acción es irreversible para evitar pulsaciones accidentales.
 *
 * @param matricula Matrícula del vehículo al que pertenece la incidencia.
 */
@Composable
private fun DetalleIncidenciaDialog(
    incidencia: Incidencia,
    matricula: String,
    onMarcarRevisada: () -> Unit,
    onCerrar: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Incidencia — $matricula") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(incidencia.descripcion)
                Text("Conductor: ${incidencia.conductorNombre}")
                Text("Fecha: ${fmt.format(incidencia.fecha)}")
                Text("Km al reportar: %,d km".format(incidencia.kmAlReportar))
                Text(
                    "Marcar como revisada es irreversible.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onMarcarRevisada) { Text("Marcar revisada") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val incPreview = Incidencia(
    id = "i1", vehiculoId = "v1", descripcion = "Faro delantero derecho fundido",
    fecha = Instant.parse("2025-01-08T14:30:00Z"),
    conductorId = "c1", conductorNombre = "Ana García",
    kmAlReportar = 44890, revisada = false, fechaRevisada = null, kmAlRevisar = null
)
private val vehiculoIncPreview = Vehiculo(
    id = "v1", matricula = "1234 ABC", km = 45231, horas = 1230.5f,
    conductorId = "c1", conductorNombre = "Ana García",
    ultimaActualizacion = Instant.parse("2025-01-10T09:00:00Z")
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Lista de incidencias pendientes")
@Composable
private fun IncidenciasOkPreview() {
    GarageBaseTheme {
        IncidenciasGestorContent(
            uiState = IncidenciasGestorUiState.Ok(
                pendientes = listOf(
                    incPreview,
                    incPreview.copy(id = "i2", vehiculoId = "v2", descripcion = "Ruido en frenos traseros",
                        fecha = Instant.parse("2025-01-06T08:00:00Z"))
                ),
                vehiculos = listOf(
                    vehiculoIncPreview,
                    vehiculoIncPreview.copy(id = "v2", matricula = "5678 DEF", conductorId = null, conductorNombre = null)
                )
            ),
            seleccionada = null,
            onBack = {}, onSeleccionar = {}, onCerrarDetalle = {}, onMarcarRevisada = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Detalle incidencia abierto")
@Composable
private fun IncidenciasDetallePreview() {
    GarageBaseTheme {
        IncidenciasGestorContent(
            uiState = IncidenciasGestorUiState.Ok(
                pendientes = listOf(incPreview),
                vehiculos = listOf(vehiculoIncPreview)
            ),
            seleccionada = incPreview,
            onBack = {}, onSeleccionar = {}, onCerrarDetalle = {}, onMarcarRevisada = {}
        )
    }
}
