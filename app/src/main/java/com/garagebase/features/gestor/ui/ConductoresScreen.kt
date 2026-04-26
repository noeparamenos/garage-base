package com.garagebase.features.gestor.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.garagebase.core.model.Conductor
import com.garagebase.core.model.Vehiculo
import com.garagebase.navigation.Screen
import com.garagebase.ui.theme.GarageBaseTheme
import java.time.Instant

/**
 * Pantalla de gestión de conductores: listado + añadir.
 *
 * **Flujo de acciones**:
 * - FAB "+" → diálogo de alta (nombre + teléfono + confirmación).
 * - Pulsar en un conductor → navega a [ConductorDetalleScreen] donde se puede editar
 *   y gestionar la asignación de vehículo.
 *
 * **Patrón stateful/stateless** — separación en dos composables:
 * - [ConductoresScreen] (stateful): crea el ViewModel con `viewModel()`, observa los Flows
 *   con `collectAsState()` y convierte los métodos del VM en lambdas que pasa hacia abajo.
 * - [ConductoresContent] (stateless): solo recibe datos y callbacks; no sabe nada de
 *   ViewModels ni Flows. Eso lo hace previsualizeable sin emulador y fácilmente testeable.
 *
 * @param navController Para navegar hacia atrás y hacia el detalle del conductor.
 * @param viewModel ViewModel inyectado automáticamente por Compose; su ciclo de vida queda
 *                  ligado al [NavBackStackEntry] de esta pantalla, no a la Activity.
 */
@Composable
fun ConductoresScreen(
    navController: NavHostController,
    viewModel: GestorConductoresViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogo by viewModel.dialogo.collectAsState()

    ConductoresContent(
        uiState = uiState,
        dialogo = dialogo,
        onBack = { navController.popBackStack() },
        onAnadirClick = viewModel::abrirDialogoNuevo,
        onConductorClick = { conductor ->
            navController.navigate(Screen.GestorConductorDetalle.createRoute(conductor.id))
        },
        onCerrarDialogo = viewModel::cerrarDialogo,
        onNombreChange = viewModel::actualizarNombre,
        onTelefonoChange = viewModel::actualizarTelefono,
        onPedirConfirmacion = viewModel::pedirConfirmacion,
        onCancelarConfirmacion = viewModel::cancelarConfirmacion,
        onGuardar = viewModel::guardar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConductoresContent(
    uiState: ConductoresUiState,
    dialogo: DialogoConductorState?,
    onBack: () -> Unit,
    onAnadirClick: () -> Unit,
    onConductorClick: (Conductor) -> Unit,
    onCerrarDialogo: () -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onPedirConfirmacion: () -> Unit,
    onCancelarConfirmacion: () -> Unit,
    onGuardar: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conductores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAnadirClick) {
                Icon(Icons.Default.Add, contentDescription = "Añadir conductor")
            }
        }
    ) { padding ->
        when (uiState) {
            is ConductoresUiState.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ConductoresUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.mensaje, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }

            is ConductoresUiState.Ok -> {
                if (uiState.conductores.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No hay conductores. Pulsa + para añadir uno.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        items(uiState.conductores, key = { it.id }) { conductor ->
                            val matricula = uiState.vehiculos
                                .firstOrNull { it.conductorId == conductor.id }
                                ?.matricula ?: "—"

                            ListItem(
                                headlineContent = { Text(conductor.nombre) },
                                supportingContent = { Text("Vehículo: $matricula") },
                                trailingContent = {
                                    if (conductor.esGestor) {
                                        Text(
                                            "Gestor",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { onConductorClick(conductor) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Diálogo de alta (formulario → confirmación)
    if (dialogo != null) {
        if (dialogo.confirmando) {
            ConfirmacionDialog(
                nombre = dialogo.nombre,
                telefono = dialogo.telefono,
                onConfirmar = onGuardar,
                onCancelar = onCancelarConfirmacion,
                guardando = dialogo.guardando
            )
        } else {
            FormularioConductorDialog(
                dialogo = dialogo,
                onNombreChange = onNombreChange,
                onTelefonoChange = onTelefonoChange,
                onCancelar = onCerrarDialogo,
                onSiguiente = onPedirConfirmacion
            )
        }
    }
}

/**
 * Formulario de alta de conductor: nombre y teléfono.
 *
 * Solo se usa para crear conductores nuevos — la edición se hace desde [ConductorDetalleScreen].
 * El botón "Buscar en agenda" lanza el selector de contactos del sistema con ACTION_PICK
 * sobre Phone.CONTENT_URI, lo que otorga acceso temporal sin pedir READ_CONTACTS.
 * El número se limpia de espacios y guiones para ajustarse al formato +34XXXXXXXXX.
 */
@Composable
private fun FormularioConductorDialog(
    dialogo: DialogoConductorState,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCancelar: () -> Unit,
    onSiguiente: () -> Unit
) {
    val context = LocalContext.current

    val seleccionarContacto = rememberLauncherForActivityResult(
        contract = object : ActivityResultContract<Unit, Uri?>() {
            override fun createIntent(context: Context, input: Unit) =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
                if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    ) { uri ->
        uri?.let { phoneUri ->
            context.contentResolver.query(
                phoneUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    onNombreChange(cursor.getString(0))
                    onTelefonoChange(normalizarTelefono(cursor.getString(1)))
                }
            }
        }
    }

    val camposValidos = dialogo.nombre.isNotBlank() && dialogo.telefono.isNotBlank()
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo conductor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { seleccionarContacto.launch(Unit) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar en agenda")
                }
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
                    label = { Text("Teléfono (ej. +34612345678)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
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
 * Paso de verificación de datos antes de persistir el nuevo conductor.
 *
 * Muestra [nombre] y [telefono] tal como quedarán en Firestore para que el gestor
 * detecte errores tipográficos. El teléfono es especialmente crítico porque es la
 * clave de acceso del conductor a la app.
 *
 * Mientras [guardando] es true se bloquean los botones y aparece un spinner para evitar
 * doble envío mientras la escritura en Firestore está en vuelo.
 */
@Composable
private fun ConfirmacionDialog(
    nombre: String,
    telefono: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Confirmar creación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verifica los datos antes de crear el conductor:")
                Text("Nombre: $nombre", style = MaterialTheme.typography.bodyMedium)
                Text("Teléfono: $telefono", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (guardando) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                TextButton(onClick = onConfirmar, enabled = !guardando) { Text("Crear") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) { Text("Corregir") }
        }
    )
}

// ── Utilidades ───────────────────────────────────────────────────────────────

/**
 * Normaliza un número de teléfono al formato E.164.
 *
 * Casos que maneja:
 *   "+34 612 345 678"  → "+34612345678"  (cualquier + ya es E.164, solo limpiar)
 *   "+44 7700 900123"  → "+447700900123" (ídem, extranjero)
 *   "0034612345678"    → "+34612345678"  (00 internacional español)
 *   "0044 7700 900123" → "+447700900123" (00 internacional extranjero)
 *   "34612345678"      → "+34612345678"  (prefijo 34 sin +)
 *   "612345678"        → "+34612345678"  (número local español de 9 dígitos)
 */
private fun normalizarTelefono(raw: String): String {
    val soloDigitos = raw.filter { it.isDigit() }
    val conSigno = if (raw.contains('+')) "+$soloDigitos" else soloDigitos
    return when {
        conSigno.startsWith("+")                           -> conSigno          // ya E.164
        conSigno.startsWith("00")                          -> "+${conSigno.drop(2)}"  // 00XX → +XX
        conSigno.startsWith("34") && conSigno.length >= 11 -> "+$conSigno"     // 34 sin +
        soloDigitos.length == 9                            -> "+34$soloDigitos" // local español
        else                                               -> conSigno
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────
// Las previews llaman a [ConductoresContent] (privado) en lugar de [ConductoresScreen]
// porque este último necesita un ViewModel real y un NavController — cosas que el sistema
// de preview de Android Studio no puede instanciar. El composable stateless sí puede
// recibir datos de ejemplo directamente, que es precisamente la ventaja del patrón.

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Lista de conductores")
@Composable
private fun ConductoresOkPreview() {
    val conductores = listOf(
        Conductor("c1", "Ana García", "+34612345678", Conductor.Rol.CONDUCTOR),
        Conductor("c2", "Pedro Martínez", "+34698765432", Conductor.Rol.CONDUCTOR),
        Conductor("g1", "Juan López", "+34677889900", Conductor.Rol.GESTOR),
    )
    val vehiculos = listOf(
        Vehiculo("v1", "1234 ABC", 45231, 1230.5f, "c1", "Ana García", Instant.parse("2025-01-10T09:00:00Z")),
        Vehiculo("v2", "5678 DEF", 12100, 340.0f, null, null, null),
    )
    GarageBaseTheme {
        ConductoresContent(
            uiState = ConductoresUiState.Ok(conductores, vehiculos),
            dialogo = null,
            onBack = {}, onAnadirClick = {}, onConductorClick = {},
            onCerrarDialogo = {}, onNombreChange = {}, onTelefonoChange = {},
            onPedirConfirmacion = {}, onCancelarConfirmacion = {}, onGuardar = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Diálogo nuevo conductor")
@Composable
private fun ConductoresDialogoPreview() {
    GarageBaseTheme {
        ConductoresContent(
            uiState = ConductoresUiState.Ok(emptyList(), emptyList()),
            dialogo = DialogoConductorState(nombre = "María López", telefono = "+34612"),
            onBack = {}, onAnadirClick = {}, onConductorClick = {},
            onCerrarDialogo = {}, onNombreChange = {}, onTelefonoChange = {},
            onPedirConfirmacion = {}, onCancelarConfirmacion = {}, onGuardar = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Diálogo confirmación creación")
@Composable
private fun ConductoresConfirmacionPreview() {
    GarageBaseTheme {
        ConductoresContent(
            uiState = ConductoresUiState.Cargando,
            dialogo = DialogoConductorState(
                nombre = "María López", telefono = "+34612345678", confirmando = true
            ),
            onBack = {}, onAnadirClick = {}, onConductorClick = {},
            onCerrarDialogo = {}, onNombreChange = {}, onTelefonoChange = {},
            onPedirConfirmacion = {}, onCancelarConfirmacion = {}, onGuardar = {}
        )
    }
}
