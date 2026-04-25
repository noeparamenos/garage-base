package com.garagebase.features.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.garagebase.core.model.Conductor
import com.garagebase.core.model.Incidencia
import com.garagebase.core.model.Vehiculo
import com.garagebase.ui.theme.GarageBaseTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Detalle de un vehículo: km, horas, incidencias (pendientes o todas) y asignación de conductor.
 *
 * El [vehiculoId] lo lee el ViewModel desde el SavedStateHandle que Navigation rellena
 * automáticamente — esta pantalla no necesita recibirlo como parámetro.
 *
 * @param navController Para el botón de retroceso.
 * @param viewModel ViewModel con SavedStateHandle inyectado por Navigation.
 */
@Composable
fun VehiculoDetalleScreen(
    navController: NavHostController,
    viewModel: GestorVehiculoDetalleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mostrarHistorico by viewModel.mostrarHistorico.collectAsState()
    val incidenciaARevisar by viewModel.incidenciaARevisar.collectAsState()
    val dialogoAsignar by viewModel.dialogoAsignar.collectAsState()

    VehiculoDetalleContent(
        uiState = uiState,
        mostrarHistorico = mostrarHistorico,
        incidenciaARevisar = incidenciaARevisar,
        dialogoAsignar = dialogoAsignar,
        onBack = { navController.popBackStack() },
        onToggleHistorico = viewModel::toggleHistorico,
        onAbrirAsignar = viewModel::abrirDialogoAsignar,
        onCerrarAsignar = viewModel::cerrarDialogoAsignar,
        onAsignar = viewModel::asignarConductor,
        onQuitarConductor = viewModel::quitarConductor,
        onPedirConfirmacionRevisar = viewModel::pedirConfirmacionRevisar,
        onCancelarRevisar = viewModel::cancelarRevisar,
        onConfirmarRevisar = viewModel::marcarRevisada
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiculoDetalleContent(
    uiState: VehiculoDetalleUiState,
    mostrarHistorico: Boolean,
    incidenciaARevisar: Incidencia?,
    dialogoAsignar: Boolean,
    onBack: () -> Unit,
    onToggleHistorico: () -> Unit,
    onAbrirAsignar: () -> Unit,
    onCerrarAsignar: () -> Unit,
    onAsignar: (Conductor) -> Unit,
    onQuitarConductor: () -> Unit,
    onPedirConfirmacionRevisar: (Incidencia) -> Unit,
    onCancelarRevisar: () -> Unit,
    onConfirmarRevisar: () -> Unit
) {
    val titulo = when (uiState) {
        is VehiculoDetalleUiState.Ok -> uiState.vehiculo.matricula
        else -> "Detalle vehículo"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is VehiculoDetalleUiState.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is VehiculoDetalleUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.mensaje)
                }
            }

            is VehiculoDetalleUiState.Ok -> {
                val incidencias = if (mostrarHistorico) {
                    uiState.incidencias
                } else {
                    uiState.incidencias.filter { !it.revisada }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { ResumenCard(uiState.vehiculo) }
                    item {
                        ConductorAsignadoCard(
                            conductorNombre = uiState.vehiculo.conductorNombre,
                            onAsignarClick = onAbrirAsignar,
                            onQuitarClick = onQuitarConductor
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Incidencias", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onToggleHistorico) {
                                Text(if (mostrarHistorico) "Solo pendientes" else "Ver histórico")
                            }
                        }
                    }

                    if (incidencias.isEmpty()) {
                        item {
                            Text(
                                if (mostrarHistorico) "Sin incidencias registradas."
                                else "Sin incidencias pendientes.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(incidencias, key = { it.id }) { inc ->
                            IncidenciaItem(
                                incidencia = inc,
                                onMarcarRevisada = { onPedirConfirmacionRevisar(inc) }
                            )
                            HorizontalDivider()
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Diálogo de confirmación para marcar revisada
    if (incidenciaARevisar != null) {
        ConfirmacionRevisadaDialog(
            incidencia = incidenciaARevisar,
            onConfirmar = onConfirmarRevisar,
            onCancelar = onCancelarRevisar
        )
    }

    // Diálogo para asignar conductor
    if (dialogoAsignar && uiState is VehiculoDetalleUiState.Ok) {
        AsignarConductorDialog(
            conductores = uiState.conductores,
            vehiculos = uiState.todosVehiculos,
            conductorActualId = uiState.vehiculo.conductorId,
            onAsignar = onAsignar,
            onCancelar = onCerrarAsignar
        )
    }
}

/**
 * Tarjeta con el resumen numérico del vehículo.
 *
 * Muestra km con separador de miles (`%,d`) para facilitar la lectura de números grandes.
 * La [ultimaActualizacion] solo aparece si el conductor ya hizo algún reporte; al crear
 * el vehículo este campo es null.
 *
 * @param vehiculo Datos actualizados del vehículo (viene de un Flow en tiempo real).
 */
@Composable
private fun ResumenCard(vehiculo: Vehiculo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium)
            Text("Kilómetros: %,d km".format(vehiculo.km))
            Text("Horas: %.1f h".format(vehiculo.horas))
            if (vehiculo.ultimaActualizacion != null) {
                val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                Text("Última actualización: ${fmt.format(vehiculo.ultimaActualizacion)}")
            }
        }
    }
}

/**
 * Tarjeta con el conductor asignado al vehículo y acciones de gestión.
 *
 * [conductorNombre] es nullable porque un vehículo recién creado puede no tener conductor.
 * El botón "Quitar" solo aparece cuando hay un conductor asignado; "Modificar" siempre
 * está disponible para abrir el diálogo de selección de conductor.
 *
 * @param conductorNombre Nombre denormalizado del conductor asignado, o null si no hay ninguno.
 * @param onAsignarClick Abre el diálogo de selección de conductor.
 * @param onQuitarClick Quita directamente el conductor asignado (solo visible cuando hay uno).
 */
@Composable
private fun ConductorAsignadoCard(
    conductorNombre: String?,
    onAsignarClick: () -> Unit,
    onQuitarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conductor asignado", style = MaterialTheme.typography.titleMedium)
            Text(conductorNombre ?: "Sin asignar")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAsignarClick) { Text("Modificar") }
                if (conductorNombre != null) {
                    OutlinedButton(onClick = onQuitarClick) { Text("Quitar") }
                }
            }
        }
    }
}

/**
 * Fila de una incidencia con su estado y (si está pendiente) botón de revisión.
 *
 * Muestra [Incidencia.kmAlReportar] como snapshot del momento del reporte — es el valor
 * que el gestor necesita para entender el contexto (cuántos km llevaba el vehículo cuando
 * ocurrió el problema). No se muestra el km actual del vehículo porque podría llevar
 * a confusión.
 *
 * @param incidencia Incidencia a mostrar.
 * @param onMarcarRevisada Acción al pulsar "Revisar" — abre el diálogo de confirmación.
 */
@Composable
private fun IncidenciaItem(incidencia: Incidencia, onMarcarRevisada: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
    ListItem(
        headlineContent = { Text(incidencia.descripcion) },
        supportingContent = {
            Column {
                Text("${incidencia.conductorNombre} · ${fmt.format(incidencia.fecha)}")
                Text("Al reportar: %,d km".format(incidencia.kmAlReportar))
            }
        },
        trailingContent = {
            if (!incidencia.revisada) {
                Button(onClick = onMarcarRevisada) { Text("Reparar") }
            } else {
                Text(
                    "Revisada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

/**
 * Diálogo de confirmación antes de marcar una incidencia como revisada.
 *
 * Se destaca que la acción es irreversible para que el gestor no lo haga por error.
 */
@Composable
private fun ConfirmacionRevisadaDialog(
    incidencia: Incidencia,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Marcar como reparada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"${incidencia.descripcion}\"")
                Text(
                    "Esta acción es irreversible. Una vez marcada, la incidencia no puede volver a estado pendiente.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

/**
 * Diálogo para seleccionar un conductor de la lista y asignarlo al vehículo.
 *
 * Muestra para cada conductor su vehículo actual (si tiene) para que el gestor sepa
 * si está reasignando a alguien que ya tiene otro vehículo. El repositorio se encarga
 * de liberar el vehículo anterior con un WriteBatch atómico.
 *
 * Usa RadioButtons para que la selección sea visualmente clara.
 *
 * @param vehiculos Lista completa de vehículos — se usa para mostrar la matrícula actual
 *                  de cada conductor y advertir de reasignaciones.
 * @param conductorActualId ID del conductor actualmente asignado (preseleccionado al abrir).
 */
@Composable
private fun AsignarConductorDialog(
    conductores: List<Conductor>,
    vehiculos: List<Vehiculo>,
    conductorActualId: String?,
    onAsignar: (Conductor) -> Unit,
    onCancelar: () -> Unit
) {
    var seleccionadoId by remember { mutableStateOf(conductorActualId) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Asignar conductor") },
        text = {
            // Column con scroll en lugar de LazyColumn: los AlertDialog tienen altura
            // acotada y LazyColumn dentro generaría un contenedor de altura infinita.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                conductores.forEach { conductor ->
                    val vehiculoActual = vehiculos.firstOrNull { it.conductorId == conductor.id }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = conductor.id == seleccionadoId,
                            onClick = { seleccionadoId = conductor.id }
                        )
                        Column {
                            Text(conductor.nombre)
                            Text(
                                text = vehiculoActual?.matricula ?: "Sin vehículo",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (vehiculoActual != null)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    conductores.firstOrNull { it.id == seleccionadoId }?.let { onAsignar(it) }
                },
                enabled = seleccionadoId != null
            ) { Text("Asignar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────
// Datos ficticios con fechas fijas (no `Instant.now()`) para que el preview sea reproducible.

private val vehiculoPreview = Vehiculo(
    id = "v1", matricula = "1234 ABC", km = 45231, horas = 1230.5f,
    conductorId = "c1", conductorNombre = "Ana García",
    ultimaActualizacion = Instant.parse("2025-01-10T09:00:00Z")
)
private val incidenciaPendientePreview = Incidencia(
    id = "i1", vehiculoId = "v1", descripcion = "Faro delantero derecho fundido",
    fecha = Instant.parse("2025-01-08T14:30:00Z"), conductorId = "c1",
    conductorNombre = "Ana García", kmAlReportar = 44890,
    revisada = false, fechaRevisada = null, kmAlRevisar = null
)
private val incidenciaRevisadaPreview = Incidencia(
    id = "i2", vehiculoId = "v1", descripcion = "Ruido extraño en la transmisión",
    fecha = Instant.parse("2024-12-20T08:00:00Z"), conductorId = "c1",
    conductorNombre = "Ana García", kmAlReportar = 43200,
    revisada = true, fechaRevisada = Instant.parse("2024-12-22T10:00:00Z"), kmAlRevisar = 43500
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Detalle vehículo — pendientes")
@Composable
private fun VehiculoDetalleOkPreview() {
    GarageBaseTheme {
        VehiculoDetalleContent(
            uiState = VehiculoDetalleUiState.Ok(
                vehiculo = vehiculoPreview,
                incidencias = listOf(incidenciaPendientePreview, incidenciaRevisadaPreview),
                conductores = listOf(Conductor("c1", "Ana García", "+34612345678", Conductor.Rol.CONDUCTOR)),
                todosVehiculos = listOf(vehiculoPreview)
            ),
            mostrarHistorico = false, incidenciaARevisar = null, dialogoAsignar = false,
            onBack = {}, onToggleHistorico = {}, onAbrirAsignar = {}, onCerrarAsignar = {},
            onAsignar = {}, onQuitarConductor = {}, onPedirConfirmacionRevisar = {}, onCancelarRevisar = {}, onConfirmarRevisar = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Detalle vehículo — confirmar revisión")
@Composable
private fun VehiculoDetalleRevisarPreview() {
    GarageBaseTheme {
        VehiculoDetalleContent(
            uiState = VehiculoDetalleUiState.Ok(
                vehiculo = vehiculoPreview,
                incidencias = listOf(incidenciaPendientePreview),
                conductores = emptyList(),
                todosVehiculos = listOf(vehiculoPreview)
            ),
            mostrarHistorico = false, incidenciaARevisar = incidenciaPendientePreview, dialogoAsignar = false,
            onBack = {}, onToggleHistorico = {}, onAbrirAsignar = {}, onCerrarAsignar = {},
            onAsignar = {}, onQuitarConductor = {}, onPedirConfirmacionRevisar = {}, onCancelarRevisar = {}, onConfirmarRevisar = {}
        )
    }
}
