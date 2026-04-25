package com.garagebase.features.conductores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import com.garagebase.core.model.Incidencia
import com.garagebase.core.model.Vehiculo
import com.garagebase.ui.theme.GarageBaseTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pantalla principal del conductor (stateful).
 *
 * Posee el [viewModel] y recoge los StateFlows con `collectAsState()`.
 * Delega toda la UI a [ConductorHomeContent], que solo recibe datos y lambdas
 * — esto permite crear Previews sin emulador ni Firebase.
 *
 * @param navController Controlador de navegación. Si hay una entrada anterior en el backstack
 *   (por ejemplo, el gestor llegó aquí desde GestorHome), se muestra un botón de volver.
 *   Para un conductor que aterrizó aquí tras el login no habrá entrada anterior y no aparece.
 */
@Composable
fun ConductorHomeScreen(
    navController: NavHostController? = null,
    viewModel: ConductorHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogoActualizar by viewModel.dialogoActualizar.collectAsState()
    val dialogoIncidencia by viewModel.dialogoIncidencia.collectAsState()
    val errorDialogo by viewModel.errorDialogo.collectAsState()

    // previousBackStackEntry != null solo cuando hay una pantalla a la que volver
    // (gestor navegando desde GestorHome). Los conductores llegan aquí como destino raíz.
    val onBack: (() -> Unit)? = navController
        ?.takeIf { it.previousBackStackEntry != null }
        ?.let { nc -> { nc.popBackStack() } }

    ConductorHomeContent(
        uiState = uiState,
        dialogoActualizar = dialogoActualizar,
        dialogoIncidencia = dialogoIncidencia,
        errorDialogo = errorDialogo,
        onBack = onBack,
        onAbrirActualizar = viewModel::abrirDialogoActualizar,
        onCerrarActualizar = viewModel::cerrarDialogoActualizar,
        onConfirmarActualizar = viewModel::actualizarKmYHoras,
        onAbrirIncidencia = viewModel::abrirDialogoIncidencia,
        onCerrarIncidencia = viewModel::cerrarDialogoIncidencia,
        onConfirmarIncidencia = viewModel::añadirIncidencia
    )
}

/**
 * Contenido de la pantalla del conductor (stateless).
 *
 * @param uiState Estado de carga, error o datos del vehículo asignado.
 * @param dialogoActualizar true si el diálogo de km/horas debe mostrarse.
 * @param dialogoIncidencia true si el diálogo de nueva incidencia debe mostrarse.
 * @param errorDialogo Mensaje de error de validación del diálogo activo, o null.
 * @param onAbrirActualizar Abre el diálogo de actualización de km/horas.
 * @param onCerrarActualizar Cierra el diálogo de actualización de km/horas.
 * @param onConfirmarActualizar Llama al ViewModel con los valores de km y horas introducidos.
 * @param onAbrirIncidencia Abre el diálogo para añadir incidencia.
 * @param onCerrarIncidencia Cierra el diálogo de incidencia.
 * @param onConfirmarIncidencia Llama al ViewModel con la descripción de la incidencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConductorHomeContent(
    uiState: ConductorHomeUiState,
    dialogoActualizar: Boolean,
    dialogoIncidencia: Boolean,
    errorDialogo: String?,
    onBack: (() -> Unit)? = null,
    onAbrirActualizar: () -> Unit,
    onCerrarActualizar: () -> Unit,
    onConfirmarActualizar: (km: String, horas: String) -> Unit,
    onAbrirIncidencia: () -> Unit,
    onCerrarIncidencia: () -> Unit,
    onConfirmarIncidencia: (descripcion: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi vehículo") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                }
            )
        },
        // FAB para añadir incidencia — solo visible cuando hay vehículo asignado
        floatingActionButton = {
            if (uiState is ConductorHomeUiState.Ok) {
                ExtendedFloatingActionButton(
                    onClick = onAbrirIncidencia,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva incidencia") }
                )
            }
        }
    ) { padding ->
        when (uiState) {
            is ConductorHomeUiState.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ConductorHomeUiState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.mensaje, color = MaterialTheme.colorScheme.error)
                }
            }

            is ConductorHomeUiState.SinVehiculo -> {
                Box(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Hola, ${uiState.conductorNombre}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Aún no tienes ningún vehículo asignado.\nContacta con el gestor.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is ConductorHomeUiState.Ok -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    item { VehiculoCard(uiState.vehiculo, onActualizarClick = onAbrirActualizar) }
                    item {
                        Text(
                            "Incidencias",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (uiState.incidencias.isEmpty()) {
                        item {
                            Text(
                                "No hay incidencias registradas.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.incidencias, key = { it.id }) { incidencia ->
                            IncidenciaItem(incidencia)
                            HorizontalDivider()
                        }
                    }
                    // Espacio para que el FAB no tape la última incidencia
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (dialogoActualizar && uiState is ConductorHomeUiState.Ok) {
        ActualizarKmDialog(
            kmActual = uiState.vehiculo.km,
            horasActual = uiState.vehiculo.horas,
            error = errorDialogo,
            onConfirmar = onConfirmarActualizar,
            onCancelar = onCerrarActualizar
        )
    }

    if (dialogoIncidencia) {
        AñadirIncidenciaDialog(
            error = errorDialogo,
            onConfirmar = onConfirmarIncidencia,
            onCancelar = onCerrarIncidencia
        )
    }
}

/**
 * Tarjeta con el resumen del vehículo asignado.
 *
 * [ultimaActualizacion] es null cuando el conductor aún no ha enviado ningún reporte;
 * en ese caso la fila no aparece para evitar mostrar "null" o una fecha de época.
 *
 * @param vehiculo Datos actualizados del vehículo (proviene de un Flow en tiempo real).
 * @param onActualizarClick Abre el diálogo de actualización de km/horas.
 */
@Composable
private fun VehiculoCard(vehiculo: Vehiculo, onActualizarClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(vehiculo.matricula, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Kilómetros: %,d km".format(vehiculo.km))
            Text("Horas: %.1f h".format(vehiculo.horas))
            if (vehiculo.ultimaActualizacion != null) {
                val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                Text(
                    "Última actualización: ${fmt.format(vehiculo.ultimaActualizacion)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onActualizarClick, modifier = Modifier.fillMaxWidth()) {
                Text("Actualizar km / horas")
            }
        }
    }
}

/**
 * Fila de una incidencia con su estado (revisada / pendiente).
 *
 * El estado "Revisada" se muestra en verde (color primary) para que el conductor identifique
 * de un vistazo que el gestor ya la atendió. Las pendientes no tienen badge de acción
 * porque el conductor no puede modificarlas una vez enviadas.
 *
 * @param incidencia Incidencia a mostrar.
 */
@Composable
private fun IncidenciaItem(incidencia: Incidencia) {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
    val colorFondo = if (incidencia.revisada)
        Color(0xFFA5D6A7)   // verde pastel suave
    else
        Color(0xFFEF9A9A)   // rojo pastel suave

    ListItem(
        headlineContent = { Text(incidencia.descripcion) },
        supportingContent = {
            Text("${fmt.format(incidencia.fecha)} · ${"%,d km".format(incidencia.kmAlReportar)}")
        },
        trailingContent = {
            if (incidencia.revisada) {
                Text("Revisada", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            } else {
                Text("Pendiente", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = colorFondo,
            headlineColor = Color.Black,
            supportingColor = Color.Black,
            trailingIconColor = Color.Black
        )
    )
}

/**
 * Diálogo para actualizar los km y horas del vehículo.
 *
 * Muestra los valores actuales como hint para orientar al conductor.
 * La validación (km/horas no pueden bajar) ocurre en el ViewModel;
 * [error] transporta el mensaje de error si la validación falla.
 *
 * @param kmActual Km actuales del vehículo (se muestran como placeholder en el campo).
 * @param horasActual Horas actuales del vehículo.
 * @param error Mensaje de error de validación, o null si no hay error.
 * @param onConfirmar Envía los valores al ViewModel.
 * @param onCancelar Cierra el diálogo sin guardar.
 */
@Composable
private fun ActualizarKmDialog(
    kmActual: Int,
    horasActual: Float,
    error: String?,
    onConfirmar: (km: String, horas: String) -> Unit,
    onCancelar: () -> Unit
) {
    var km by remember { mutableStateOf("") }
    var horas by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Actualizar km / horas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it },
                    label = { Text("Km actuales (actual: %,d)".format(kmActual)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = horas,
                    onValueChange = { horas = it },
                    label = { Text("Horas actuales (actual: %.1f)".format(horasActual)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(km, horas) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

/**
 * Diálogo para añadir una nueva incidencia al vehículo.
 *
 * @param error Mensaje de validación (campo vacío o error de escritura), o null.
 * @param onConfirmar Envía la descripción al ViewModel para crear la incidencia.
 * @param onCancelar Cierra el diálogo sin crear la incidencia.
 */
@Composable
private fun AñadirIncidenciaDialog(
    error: String?,
    onConfirmar: (descripcion: String) -> Unit,
    onCancelar: () -> Unit
) {
    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva incidencia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción del problema") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(descripcion) }) { Text("Enviar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val vehiculoPreview = Vehiculo(
    id = "v1", matricula = "4321 XYZ", km = 78430, horas = 2105.5f,
    conductorId = "u1", conductorNombre = "Carlos López",
    ultimaActualizacion = Instant.parse("2025-03-14T08:30:00Z")
)
private val incidenciasPendientesPreview = listOf(
    Incidencia(
        id = "i1", vehiculoId = "v1", descripcion = "Luz de freno trasera fundida",
        fecha = Instant.parse("2025-03-07T16:00:00Z"), conductorId = "u1",
        conductorNombre = "Carlos López", kmAlReportar = 78100,
        revisada = false, fechaRevisada = null, kmAlRevisar = null
    ),
    Incidencia(
        id = "i2", vehiculoId = "v1", descripcion = "Ruido al frenar en bajada",
        fecha = Instant.parse("2025-02-28T09:15:00Z"), conductorId = "u1",
        conductorNombre = "Carlos López", kmAlReportar = 76500,
        revisada = true, fechaRevisada = Instant.parse("2025-03-01T11:00:00Z"), kmAlRevisar = 76800
    )
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Conductor — vehículo asignado")
@Composable
private fun ConductorHomeOkPreview() {
    GarageBaseTheme {
        ConductorHomeContent(
            uiState = ConductorHomeUiState.Ok(
                conductor = com.garagebase.core.model.Conductor(
                    "u1", "Carlos López", "+34666111222",
                    com.garagebase.core.model.Conductor.Rol.CONDUCTOR
                ),
                vehiculo = vehiculoPreview,
                incidencias = incidenciasPendientesPreview
            ),
            dialogoActualizar = false, dialogoIncidencia = false, errorDialogo = null,
            onAbrirActualizar = {}, onCerrarActualizar = {}, onConfirmarActualizar = { _, _ -> },
            onAbrirIncidencia = {}, onCerrarIncidencia = {}, onConfirmarIncidencia = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Conductor — sin vehículo")
@Composable
private fun ConductorHomeSinVehiculoPreview() {
    GarageBaseTheme {
        ConductorHomeContent(
            uiState = ConductorHomeUiState.SinVehiculo("Carlos López"),
            dialogoActualizar = false, dialogoIncidencia = false, errorDialogo = null,
            onAbrirActualizar = {}, onCerrarActualizar = {}, onConfirmarActualizar = { _, _ -> },
            onAbrirIncidencia = {}, onCerrarIncidencia = {}, onConfirmarIncidencia = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Conductor — diálogo actualizar")
@Composable
private fun ConductorHomeDialogoActualizarPreview() {
    GarageBaseTheme {
        ConductorHomeContent(
            uiState = ConductorHomeUiState.Ok(
                conductor = com.garagebase.core.model.Conductor(
                    "u1", "Carlos López", "+34666111222",
                    com.garagebase.core.model.Conductor.Rol.CONDUCTOR
                ),
                vehiculo = vehiculoPreview,
                incidencias = emptyList()
            ),
            dialogoActualizar = true, dialogoIncidencia = false,
            errorDialogo = "Los km no pueden ser menores que los actuales (78.430 km).",
            onAbrirActualizar = {}, onCerrarActualizar = {}, onConfirmarActualizar = { _, _ -> },
            onAbrirIncidencia = {}, onCerrarIncidencia = {}, onConfirmarIncidencia = {}
        )
    }
}
