package com.garagebase.features.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.garagebase.core.model.Conductor
import com.garagebase.core.model.Vehiculo
import com.garagebase.ui.theme.GarageBaseTheme
import java.time.Instant

/**
 * Pantalla de detalle de un conductor (para el gestor).
 *
 * Permite dos acciones independientes:
 * - Editar nombre y teléfono: botón "Editar" → diálogo con formulario → paso de confirmación.
 * - Gestionar la asignación de vehículo: botón "Asignar" / "Cambiar · Quitar" → diálogo con
 *   lista de vehículos y opción "Ninguno" para dejar al conductor libre.
 *
 * **Patrón stateful/stateless**: [ConductorDetalleScreen] (stateful) crea el ViewModel y
 * observa los Flows; [ConductorDetalleContent] (stateless) solo recibe datos y lambdas,
 * lo que lo hace previsualizable sin emulador.
 *
 * @param navController Para el botón de retroceso.
 * @param viewModel ViewModel con [SavedStateHandle] inyectado por Navigation.
 */
@Composable
fun ConductorDetalleScreen(
    navController: NavHostController,
    viewModel: GestorConductorDetalleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogoEditar by viewModel.dialogoEditar.collectAsState()
    val dialogoAsignar by viewModel.dialogoAsignar.collectAsState()
    val dialogoBorrar by viewModel.dialogoBorrar.collectAsState()
    val navegarAtras by viewModel.navegarAtras.collectAsState()

    // Navega atrás una sola vez cuando el borrado se completa.
    LaunchedEffect(navegarAtras) {
        if (navegarAtras) navController.popBackStack()
    }

    ConductorDetalleContent(
        uiState = uiState,
        dialogoEditar = dialogoEditar,
        dialogoAsignar = dialogoAsignar,
        dialogoBorrar = dialogoBorrar,
        onBack = { navController.popBackStack() },
        onAbrirEditar = viewModel::abrirDialogoEditar,
        onCerrarEditar = viewModel::cerrarDialogoEditar,
        onNombreChange = viewModel::actualizarNombre,
        onTelefonoChange = viewModel::actualizarTelefono,
        onPedirConfirmacion = viewModel::pedirConfirmacion,
        onCancelarConfirmacion = viewModel::cancelarConfirmacion,
        onGuardar = viewModel::guardar,
        onAbrirAsignar = viewModel::abrirDialogoAsignar,
        onCerrarAsignar = viewModel::cerrarDialogoAsignar,
        onSeleccionarVehiculo = viewModel::seleccionarVehiculo,
        onAbrirBorrar = viewModel::abrirDialogoBorrar,
        onCerrarBorrar = viewModel::cerrarDialogoBorrar,
        onBorrar = viewModel::borrar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConductorDetalleContent(
    uiState: ConductorDetalleUiState,
    dialogoEditar: DialogoEditarConductorState?,
    dialogoAsignar: Boolean,
    dialogoBorrar: DialogoBorrarConductorState?,
    onBack: () -> Unit,
    onAbrirEditar: () -> Unit,
    onCerrarEditar: () -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onPedirConfirmacion: () -> Unit,
    onCancelarConfirmacion: () -> Unit,
    onGuardar: () -> Unit,
    onAbrirAsignar: () -> Unit,
    onCerrarAsignar: () -> Unit,
    onSeleccionarVehiculo: (String?) -> Unit,
    onAbrirBorrar: () -> Unit,
    onCerrarBorrar: () -> Unit,
    onBorrar: () -> Unit
) {
    val titulo = (uiState as? ConductorDetalleUiState.Ok)?.conductor?.nombre ?: "Conductor"

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
            is ConductorDetalleUiState.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ConductorDetalleUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.mensaje)
                }
            }

            is ConductorDetalleUiState.Ok -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DatosConductorCard(
                        conductor = uiState.conductor,
                        onEditarClick = onAbrirEditar
                    )
                    VehiculoAsignadoCard(
                        vehiculoAsignado = uiState.vehiculoAsignado,
                        onAsignarClick = onAbrirAsignar
                    )
                }
            }
        }
    }

    // Diálogo de edición: formulario → confirmación (dos pasos)
    if (dialogoEditar != null) {
        if (dialogoEditar.confirmando) {
            ConfirmacionEdicionDialog(
                nombre = dialogoEditar.nombre,
                telefono = dialogoEditar.telefono,
                guardando = dialogoEditar.guardando,
                onConfirmar = onGuardar,
                onCancelar = onCancelarConfirmacion
            )
        } else {
            EditarConductorDialog(
                dialogo = dialogoEditar,
                onNombreChange = onNombreChange,
                onTelefonoChange = onTelefonoChange,
                onCancelar = onCerrarEditar,
                onSiguiente = onPedirConfirmacion,
                onBorrar = onAbrirBorrar
            )
        }
    }

    // Diálogo de confirmación de borrado
    if (dialogoBorrar != null && uiState is ConductorDetalleUiState.Ok) {
        ConfirmacionBorrarDialog(
            nombre = uiState.conductor.nombre,
            borrando = dialogoBorrar.borrando,
            error = dialogoBorrar.error,
            onConfirmar = onBorrar,
            onCancelar = onCerrarBorrar
        )
    }

    // Diálogo de selección de vehículo
    if (dialogoAsignar && uiState is ConductorDetalleUiState.Ok) {
        AsignarVehiculoDialog(
            vehiculos = uiState.todosVehiculos,
            vehiculoActualId = uiState.vehiculoAsignado?.id,
            onSeleccionar = onSeleccionarVehiculo,
            onCancelar = onCerrarAsignar
        )
    }
}

/**
 * Tarjeta con los datos identificativos del conductor.
 *
 * El rol "Gestor" se muestra como etiqueta en color primario para que destaque
 * frente a los conductores normales.
 *
 * @param conductor Datos actuales del conductor (live desde Firestore via Flow).
 * @param onEditarClick Abre el diálogo de edición.
 */
@Composable
private fun DatosConductorCard(conductor: Conductor, onEditarClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Datos del conductor", style = MaterialTheme.typography.titleMedium)
                Text(conductor.nombre)
                Text(conductor.telefono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (conductor.esGestor) {
                    Text(
                        "Gestor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            OutlinedButton(onClick = onEditarClick) { Text("Editar") }
        }
    }
}

/**
 * Tarjeta con el vehículo asignado al conductor.
 *
 * Muestra la matrícula si tiene vehículo, o "Sin vehículo asignado" si está libre.
 * El texto del botón cambia según el estado para orientar al gestor.
 *
 * @param vehiculoAsignado Vehículo actual del conductor, o null si no tiene.
 * @param onAsignarClick Abre el diálogo de selección de vehículo.
 */
@Composable
private fun VehiculoAsignadoCard(vehiculoAsignado: Vehiculo?, onAsignarClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Vehículo asignado", style = MaterialTheme.typography.titleMedium)
                Text(vehiculoAsignado?.matricula ?: "Sin vehículo asignado")
            }
            OutlinedButton(onClick = onAsignarClick) {
                // El texto cambia según haya vehículo o no para guiar la acción esperada.
                Text(if (vehiculoAsignado != null) "Cambiar / Quitar" else "Asignar")
            }
        }
    }
}

/**
 * Formulario de edición de nombre y teléfono.
 *
 * El botón "Siguiente" no guarda directamente — pasa al paso de confirmación
 * para que el gestor pueda detectar errores tipográficos antes de persistirlos.
 *
 * El botón "Eliminar conductor" cierra este diálogo y abre el de confirmación de borrado.
 */
@Composable
private fun EditarConductorDialog(
    dialogo: DialogoEditarConductorState,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCancelar: () -> Unit,
    onSiguiente: () -> Unit,
    onBorrar: () -> Unit
) {
    val camposValidos = dialogo.nombre.isNotBlank() && dialogo.telefono.isNotBlank()
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar conductor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dialogo.nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dialogo.telefono,
                    onValueChange = onTelefonoChange,
                    label = { Text("Teléfono") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                TextButton(
                    onClick = onBorrar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar conductor") }
            }
        },
        confirmButton = {
            TextButton(onClick = onSiguiente, enabled = camposValidos) { Text("Siguiente") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

/**
 * Paso de verificación antes de persistir los cambios en Firestore.
 *
 * Muestra nombre y teléfono tal como quedarán guardados. Mientras [guardando] es true
 * se bloquean ambos botones y aparece un spinner para evitar doble pulsación.
 */
@Composable
private fun ConfirmacionEdicionDialog(
    nombre: String,
    telefono: String,
    guardando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Confirmar cambios") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verifica los datos antes de guardar:")
                Text("Nombre: $nombre", style = MaterialTheme.typography.bodyMedium)
                Text("Teléfono: $telefono", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (guardando) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                TextButton(onClick = onConfirmar, enabled = !guardando) { Text("Guardar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) { Text("Corregir") }
        }
    )
}

/**
 * Diálogo de confirmación antes de eliminar al conductor.
 *
 * Muestra el nombre para que el gestor pueda verificar que no se ha equivocado.
 * Mientras [borrando] es true se bloquean ambos botones y aparece un spinner.
 * Si el conductor tiene vehículo asignado, el ViewModel lo libera antes de borrar.
 */
@Composable
private fun ConfirmacionBorrarDialog(
    nombre: String,
    borrando: Boolean,
    error: String?,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!borrando) onCancelar() },
        title = { Text("Eliminar conductor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Seguro que quieres eliminar a $nombre? Esta acción no se puede deshacer.")
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (borrando) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                TextButton(
                    onClick = onConfirmar,
                    enabled = !borrando,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !borrando) { Text("Cancelar") }
        }
    )
}

/**
 * Diálogo para seleccionar el vehículo del conductor con opción "Ninguno".
 *
 * Para cada vehículo ya ocupado se muestra el nombre de su conductor actual
 * (denormalizado en el documento del vehículo) en color terciario, advirtiendo al
 * gestor de una posible reasignación. El repositorio se encarga de liberar el vehículo
 * anterior con un WriteBatch atómico.
 *
 * El botón "Confirmar" solo se habilita cuando la selección difiere del estado actual,
 * evitando escrituras innecesarias a Firestore.
 *
 * @param vehiculoActualId ID del vehículo actualmente asignado (preseleccionado al abrir).
 * @param onSeleccionar Callback con el ID del vehículo elegido, o null si se eligió "Ninguno".
 */
@Composable
private fun AsignarVehiculoDialog(
    vehiculos: List<Vehiculo>,
    vehiculoActualId: String?,
    onSeleccionar: (String?) -> Unit,
    onCancelar: () -> Unit
) {
    var seleccionadoId by remember { mutableStateOf(vehiculoActualId) }
    val haCambiado = seleccionadoId != vehiculoActualId

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Asignar vehículo") },
        text = {
            // Column scrollable en lugar de LazyColumn: AlertDialog acota la altura
            // y LazyColumn dentro generaría un contenedor de altura infinita.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = seleccionadoId == null, onClick = { seleccionadoId = null })
                    Text("Ninguno", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                vehiculos.forEach { vehiculo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = vehiculo.id == seleccionadoId,
                            onClick = { seleccionadoId = vehiculo.id }
                        )
                        Column {
                            Text(vehiculo.matricula)
                            Text(
                                text = vehiculo.conductorNombre ?: "Libre",
                                style = MaterialTheme.typography.bodySmall,
                                // Tertiary = aviso visual: este vehículo ya tiene conductor asignado.
                                color = if (vehiculo.conductorNombre != null)
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
                onClick = { onSeleccionar(seleccionadoId) },
                enabled = haCambiado
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val conductorPreview = Conductor(
    id = "c1", nombre = "Ana García", telefono = "+34612345678", rol = Conductor.Rol.CONDUCTOR
)
private val vehiculosPreview = listOf(
    Vehiculo("v1", "1234 ABC", 45231, 1230.5f, "c1", "Ana García", Instant.parse("2025-01-10T09:00:00Z")),
    Vehiculo("v2", "5678 DEF", 12100, 340.0f, null, null, null),
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Conductor con vehículo asignado")
@Composable
private fun ConductorDetalleConVehiculoPreview() {
    GarageBaseTheme {
        ConductorDetalleContent(
            uiState = ConductorDetalleUiState.Ok(
                conductor = conductorPreview,
                vehiculoAsignado = vehiculosPreview[0],
                todosVehiculos = vehiculosPreview
            ),
            dialogoEditar = null, dialogoAsignar = false, dialogoBorrar = null,
            onBack = {}, onAbrirEditar = {}, onCerrarEditar = {},
            onNombreChange = {}, onTelefonoChange = {}, onPedirConfirmacion = {},
            onCancelarConfirmacion = {}, onGuardar = {},
            onAbrirAsignar = {}, onCerrarAsignar = {}, onSeleccionarVehiculo = {},
            onAbrirBorrar = {}, onCerrarBorrar = {}, onBorrar = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Conductor sin vehículo")
@Composable
private fun ConductorDetalleSinVehiculoPreview() {
    GarageBaseTheme {
        ConductorDetalleContent(
            uiState = ConductorDetalleUiState.Ok(
                conductor = Conductor("c2", "Pedro Martínez", "+34698765432", Conductor.Rol.CONDUCTOR),
                vehiculoAsignado = null,
                todosVehiculos = vehiculosPreview
            ),
            dialogoEditar = null, dialogoAsignar = false, dialogoBorrar = null,
            onBack = {}, onAbrirEditar = {}, onCerrarEditar = {},
            onNombreChange = {}, onTelefonoChange = {}, onPedirConfirmacion = {},
            onCancelarConfirmacion = {}, onGuardar = {},
            onAbrirAsignar = {}, onCerrarAsignar = {}, onSeleccionarVehiculo = {},
            onAbrirBorrar = {}, onCerrarBorrar = {}, onBorrar = {}
        )
    }
}
