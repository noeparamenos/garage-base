package com.garagebase.features.gestor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.garagebase.core.model.Vehiculo
import com.garagebase.navigation.Screen
import com.garagebase.ui.theme.GarageBaseTheme
import java.time.Instant

/**
 * Listado de vehículos de la flota con acceso al detalle de cada uno.
 *
 * Sigue el mismo patrón stateful/stateless que [ConductoresScreen]:
 * - Este composable (stateful) posee el ViewModel y convierte su estado en lambdas.
 * - [VehiculosContent] (stateless) solo renderiza lo que recibe — fácilmente previsualizeable.
 *
 * @param navController Para navegar hacia atrás y al detalle del vehículo.
 * @param viewModel ViewModel inyectado automáticamente por Compose.
 */
@Composable
fun VehiculosScreen(
    navController: NavHostController,
    viewModel: GestorVehiculosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mostrarDialogo by viewModel.mostrarDialogoAnadir.collectAsState()

    VehiculosContent(
        uiState = uiState,
        mostrarDialogoAnadir = mostrarDialogo,
        onBack = { navController.popBackStack() },
        onAnadirClick = viewModel::abrirDialogoAnadir,
        onCerrarDialogo = viewModel::cerrarDialogoAnadir,
        onAnadir = viewModel::anadir,
        onVehiculoClick = { vehiculo ->
            navController.navigate(Screen.GestorVehiculoDetalle.createRoute(vehiculo.id))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiculosContent(
    uiState: VehiculosUiState,
    mostrarDialogoAnadir: Boolean,
    onBack: () -> Unit,
    onAnadirClick: () -> Unit,
    onCerrarDialogo: () -> Unit,
    onAnadir: (String) -> Unit,
    onVehiculoClick: (Vehiculo) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehículos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAnadirClick) {
                Icon(Icons.Default.Add, contentDescription = "Añadir vehículo")
            }
        }
    ) { padding ->
        when (uiState) {
            is VehiculosUiState.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is VehiculosUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.mensaje, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }

            is VehiculosUiState.Ok -> {
                if (uiState.vehiculos.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No hay vehículos. Pulsa + para añadir uno.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        items(uiState.vehiculos, key = { it.id }) { vehiculo ->
                            val conductor = vehiculo.conductorNombre ?: "Sin conductor asignado"
                            ListItem(
                                headlineContent = { Text(vehiculo.matricula) },
                                supportingContent = { Text(conductor) },
                                modifier = Modifier.clickable { onVehiculoClick(vehiculo) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoAnadir) {
        AnadirVehiculoDialog(
            onConfirmar = onAnadir,
            onCancelar = onCerrarDialogo
        )
    }
}

/**
 * Diálogo para añadir un vehículo nuevo introduciendo solo la matrícula.
 *
 * Se limita deliberadamente a la matrícula: el conductor se asigna desde [VehiculoDetalleScreen]
 * para no mezclar dos responsabilidades en el mismo flujo. Separar la creación de la asignación
 * también permite añadir vehículos antes de que haya conductores disponibles.
 *
 * `it.uppercase()` normaliza la matrícula en el momento de teclear, evitando duplicados
 * por diferencia de mayúsculas ("1234abc" vs "1234ABC").
 */
@Composable
private fun AnadirVehiculoDialog(onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    var matricula by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo vehículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Introduce la matrícula del vehículo. El conductor se asigna desde el detalle.")
                OutlinedTextField(
                    value = matricula,
                    onValueChange = { matricula = it.uppercase() },
                    label = { Text("Matrícula") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(matricula.trim()) },
                enabled = matricula.isNotBlank()
            ) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Lista de vehículos")
@Composable
private fun VehiculosOkPreview() {
    val vehiculos = listOf(
        Vehiculo("v1", "1234 ABC", 45231, 1230.5f, "c1", "Ana García", Instant.parse("2025-01-10T09:00:00Z")),
        Vehiculo("v2", "5678 DEF", 12100, 340.0f, null, null, null),
        Vehiculo("v3", "9999 XYZ", 3200, 80.0f, "c2", "Pedro Martínez", null),
    )
    GarageBaseTheme {
        VehiculosContent(
            uiState = VehiculosUiState.Ok(vehiculos),
            mostrarDialogoAnadir = false,
            onBack = {}, onAnadirClick = {}, onCerrarDialogo = {},
            onAnadir = {}, onVehiculoClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Diálogo añadir vehículo")
@Composable
private fun VehiculosDialogoPreview() {
    GarageBaseTheme {
        VehiculosContent(
            uiState = VehiculosUiState.Ok(emptyList()),
            mostrarDialogoAnadir = true,
            onBack = {}, onAnadirClick = {}, onCerrarDialogo = {},
            onAnadir = {}, onVehiculoClick = {}
        )
    }
}
