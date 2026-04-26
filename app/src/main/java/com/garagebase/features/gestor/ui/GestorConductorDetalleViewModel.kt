package com.garagebase.features.gestor.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garagebase.core.model.Conductor
import com.garagebase.core.model.Vehiculo
import com.garagebase.features.conductores.data.ConductorRepositoryImpl
import com.garagebase.features.vehiculos.data.VehiculoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado observable del detalle de un conductor.
 *
 * Combina dos Flows en uno solo:
 * - El conductor concreto (filtrado por ID de la colección completa).
 * - La lista de vehículos — necesaria para mostrar el asignado y el diálogo de selección.
 *
 * Usamos [getAll] + filtro local (igual que en [GestorVehiculoDetalleViewModel]) en lugar
 * de un hipotético `getById`: así reutilizamos la suscripción en tiempo real que ya existe
 * y evitamos abrir una nueva conexión Firestore solo para un documento.
 */
sealed class ConductorDetalleUiState {
    object Cargando : ConductorDetalleUiState()
    data class Ok(
        val conductor: Conductor,
        /** Vehículo actualmente asignado a este conductor, o null si no tiene ninguno. */
        val vehiculoAsignado: Vehiculo?,
        /** Lista completa de vehículos — necesaria para el diálogo de selección. */
        val todosVehiculos: List<Vehiculo>
    ) : ConductorDetalleUiState()
    data class Error(val mensaje: String) : ConductorDetalleUiState()
}

/**
 * Estado del diálogo de edición de datos del conductor.
 *
 * @property confirmando true cuando se muestra el paso de verificación antes de guardar.
 * @property guardando true mientras la escritura en Firestore está en vuelo.
 */
data class DialogoEditarConductorState(
    val nombre: String = "",
    val telefono: String = "",
    val confirmando: Boolean = false,
    val guardando: Boolean = false
)

/**
 * Estado del diálogo de confirmación de borrado.
 *
 * @property borrando true mientras la operación de borrado en Firestore está en vuelo.
 *   Bloquea el botón de confirmar para evitar dobles envíos.
 * @property error Mensaje de error si el borrado falló (ej: permisos de Firestore denegados).
 */
data class DialogoBorrarConductorState(
    val borrando: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel del detalle de un conductor para el gestor.
 *
 * Gestiona dos acciones independientes:
 * - Editar nombre y teléfono (con paso de confirmación antes de persistir).
 * - Seleccionar o quitar el vehículo asignado (opción "Ninguno" en el diálogo).
 *
 * Recibe el [SavedStateHandle] que Navigation rellena automáticamente con "conductorId"
 * de la ruta "gestor_conductor/{conductorId}" — sin ViewModelProvider.Factory adicional.
 */
class GestorConductorDetalleViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conductorId: String = checkNotNull(savedStateHandle["conductorId"])

    private val conductorRepo = ConductorRepositoryImpl()
    private val vehiculoRepo = VehiculoRepositoryImpl()

    private val _uiState = MutableStateFlow<ConductorDetalleUiState>(ConductorDetalleUiState.Cargando)
    val uiState: StateFlow<ConductorDetalleUiState> = _uiState.asStateFlow()

    /** Estado del diálogo de edición; null = diálogo cerrado. */
    private val _dialogoEditar = MutableStateFlow<DialogoEditarConductorState?>(null)
    val dialogoEditar: StateFlow<DialogoEditarConductorState?> = _dialogoEditar.asStateFlow()

    /** Controla la visibilidad del diálogo de selección de vehículo. */
    private val _dialogoAsignar = MutableStateFlow(false)
    val dialogoAsignar: StateFlow<Boolean> = _dialogoAsignar.asStateFlow()

    /** Estado del diálogo de borrado; null = diálogo cerrado. */
    private val _dialogoBorrar = MutableStateFlow<DialogoBorrarConductorState?>(null)
    val dialogoBorrar: StateFlow<DialogoBorrarConductorState?> = _dialogoBorrar.asStateFlow()

    /**
     * true después de que el borrado se completa con éxito.
     * La pantalla observa este valor para llamar a popBackStack() una sola vez.
     */
    private val _navegarAtras = MutableStateFlow(false)
    val navegarAtras: StateFlow<Boolean> = _navegarAtras.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                conductorRepo.getAll(),
                vehiculoRepo.getAll()
            ) { conductores, vehiculos ->
                val conductor = conductores.firstOrNull { it.id == conductorId }
                if (conductor == null) ConductorDetalleUiState.Error("Conductor no encontrado")
                else {
                    val vehiculoAsignado = vehiculos.firstOrNull { it.conductorId == conductorId }
                    ConductorDetalleUiState.Ok(conductor, vehiculoAsignado, vehiculos)
                }
            }.catch { e ->
                _uiState.value = ConductorDetalleUiState.Error(e.message ?: "Error al cargar el conductor")
            }.collect { _uiState.value = it }
        }
    }

    /** Abre el diálogo de edición con los datos actuales del conductor. */
    fun abrirDialogoEditar() {
        val ok = _uiState.value as? ConductorDetalleUiState.Ok ?: return
        _dialogoEditar.value = DialogoEditarConductorState(
            nombre = ok.conductor.nombre,
            telefono = ok.conductor.telefono
        )
    }

    fun cerrarDialogoEditar() { _dialogoEditar.value = null }
    fun actualizarNombre(v: String) { _dialogoEditar.update { it?.copy(nombre = v) } }
    fun actualizarTelefono(v: String) { _dialogoEditar.update { it?.copy(telefono = v) } }
    fun pedirConfirmacion() { _dialogoEditar.update { it?.copy(confirmando = true) } }
    fun cancelarConfirmacion() { _dialogoEditar.update { it?.copy(confirmando = false) } }

    /**
     * Guarda los cambios de nombre y teléfono en Firestore.
     *
     * El spinner ([DialogoEditarConductorState.guardando]) bloquea el botón de confirmar
     * para evitar dobles envíos mientras la coroutine está en vuelo.
     */
    fun guardar() {
        val d = _dialogoEditar.value ?: return
        _dialogoEditar.update { it?.copy(guardando = true) }
        viewModelScope.launch {
            runCatching {
                conductorRepo.update(conductorId, d.nombre.trim(), d.telefono.trim())
            }.onSuccess {
                cerrarDialogoEditar()
            }.onFailure {
                _dialogoEditar.update { it?.copy(guardando = false) }
            }
        }
    }

    fun abrirDialogoAsignar() { _dialogoAsignar.value = true }
    fun cerrarDialogoAsignar() { _dialogoAsignar.value = false }

    /**
     * Abre el diálogo de confirmación de borrado.
     * Cierra primero el diálogo de edición para que no se solapen.
     */
    fun abrirDialogoBorrar() {
        _dialogoEditar.value = null
        _dialogoBorrar.value = DialogoBorrarConductorState()
    }

    fun cerrarDialogoBorrar() { _dialogoBorrar.value = null }

    /**
     * Borra el conductor de Firestore.
     *
     * El orden importa: primero se intenta borrar el conductor (operación que puede fallar
     * por permisos). Solo si eso tiene éxito se libera el vehículo asignado, evitando
     * que el vehículo quede desasignado cuando el conductor sigue existiendo.
     */
    fun borrar() {
        val ok = _uiState.value as? ConductorDetalleUiState.Ok ?: return
        _dialogoBorrar.value = DialogoBorrarConductorState(borrando = true)
        viewModelScope.launch {
            runCatching {
                conductorRepo.delete(conductorId)
                // Solo llega aquí si delete tuvo éxito.
                ok.vehiculoAsignado?.let { vehiculoRepo.quitarConductor(it.id) }
            }.onSuccess {
                _navegarAtras.value = true
            }.onFailure { e ->
                _dialogoBorrar.value = DialogoBorrarConductorState(
                    borrando = false,
                    error = e.message ?: "Error al eliminar el conductor"
                )
            }
        }
    }

    /**
     * Asigna o quita el vehículo del conductor.
     *
     * @param vehiculoId ID del vehículo a asignar, o null para quitar la asignación actual.
     *
     * La lógica de liberar el vehículo anterior vive en [VehiculoRepositoryImpl.asignarConductor]
     * (WriteBatch atómico). Para la opción "Ninguno" usamos [VehiculoRepositoryImpl.quitarConductor],
     * que limpia los campos denormalizados del vehículo que el conductor tenía hasta ahora.
     */
    fun seleccionarVehiculo(vehiculoId: String?) {
        val ok = _uiState.value as? ConductorDetalleUiState.Ok ?: return
        viewModelScope.launch {
            runCatching {
                if (vehiculoId == null) {
                    val actual = ok.vehiculoAsignado ?: return@runCatching
                    vehiculoRepo.quitarConductor(actual.id)
                } else {
                    vehiculoRepo.asignarConductor(vehiculoId, conductorId, ok.conductor.nombre)
                }
            }.onSuccess { cerrarDialogoAsignar() }
        }
    }
}
