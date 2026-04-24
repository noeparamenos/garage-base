package com.garagebase.features.gestor.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garagebase.core.model.Conductor
import com.garagebase.core.model.Incidencia
import com.garagebase.core.model.Vehiculo
import com.garagebase.features.conductores.data.ConductorRepositoryImpl
import com.garagebase.features.incidencias.data.IncidenciaRepositoryImpl
import com.garagebase.features.vehiculos.data.VehiculoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado observable del detalle de un vehículo.
 *
 * Combina tres Flows en uno solo:
 * - El vehículo concreto (filtrado de la colección completa por ID).
 * - Todas sus incidencias (la UI filtra pendientes / históricas según [mostrarHistorico]).
 * - Todos los conductores (necesarios para el diálogo de asignación).
 */
sealed class VehiculoDetalleUiState {
    object Cargando : VehiculoDetalleUiState()
    data class Ok(
        val vehiculo: Vehiculo,
        val incidencias: List<Incidencia>,
        val conductores: List<Conductor>,
        /** Lista completa de vehículos — necesaria para mostrar qué vehículo lleva ya
         *  cada conductor en el diálogo de asignación. */
        val todosVehiculos: List<Vehiculo>
    ) : VehiculoDetalleUiState()
    data class Error(val mensaje: String) : VehiculoDetalleUiState()
}

/**
 * ViewModel del detalle de un vehículo para el gestor.
 *
 * Recibe el [SavedStateHandle] que Navigation rellena automáticamente con los argumentos
 * de la ruta ("gestor_vehiculo/{vehiculoId}") — no hace falta ningún ViewModelProvider.Factory
 * adicional cuando se instancia con `viewModel()` dentro de un `NavHost`.
 */
class GestorVehiculoDetalleViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // checkNotNull lanza IllegalStateException con mensaje descriptivo en lugar del
    // NullPointerException opaco del operador `!!`. Solo puede fallar si el NavGraph
    // olvida declarar el argumento "vehiculoId" en la ruta — un error de programación,
    // no de usuario, por eso la excepción es lo correcto.
    private val vehiculoId: String = checkNotNull(savedStateHandle["vehiculoId"])

    private val vehiculoRepo = VehiculoRepositoryImpl()
    private val incidenciaRepo = IncidenciaRepositoryImpl()
    private val conductorRepo = ConductorRepositoryImpl()

    private val _uiState = MutableStateFlow<VehiculoDetalleUiState>(VehiculoDetalleUiState.Cargando)
    val uiState: StateFlow<VehiculoDetalleUiState> = _uiState.asStateFlow()

    /**
     * Cuando es true la UI muestra todas las incidencias (revisadas + pendientes).
     * Cuando es false muestra solo las pendientes.
     */
    private val _mostrarHistorico = MutableStateFlow(false)
    val mostrarHistorico: StateFlow<Boolean> = _mostrarHistorico.asStateFlow()

    /** Incidencia seleccionada para mostrar el diálogo de confirmación de revisión. */
    private val _incidenciaARevisar = MutableStateFlow<Incidencia?>(null)
    val incidenciaARevisar: StateFlow<Incidencia?> = _incidenciaARevisar.asStateFlow()

    /** Controla si el diálogo de asignación de conductor está visible. */
    private val _dialogoAsignar = MutableStateFlow(false)
    val dialogoAsignar: StateFlow<Boolean> = _dialogoAsignar.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                vehiculoRepo.getAll(),      // lista completa: filtramos aquí y pasamos ambas cosas al estado
                incidenciaRepo.getDeVehiculo(vehiculoId),
                conductorRepo.getAll()
            ) { todosVehiculos, incidencias, conductores ->
                val vehiculo = todosVehiculos.firstOrNull { it.id == vehiculoId }
                if (vehiculo == null) VehiculoDetalleUiState.Error("Vehículo no encontrado")
                else VehiculoDetalleUiState.Ok(vehiculo, incidencias, conductores, todosVehiculos)
            }.catch { e ->
                _uiState.value = VehiculoDetalleUiState.Error(e.message ?: "Error al cargar el vehículo")
            }.collect { _uiState.value = it }
        }
    }

    /** Alterna entre vista "solo pendientes" y vista histórica completa. */
    fun toggleHistorico() { _mostrarHistorico.update { !it } }

    fun abrirDialogoAsignar() { _dialogoAsignar.value = true }
    fun cerrarDialogoAsignar() { _dialogoAsignar.value = false }

    /**
     * Asigna el [conductor] al vehículo actual actualizando los campos denormalizados.
     *
     * @param conductor El nuevo conductor que se asigna.
     */
    fun asignarConductor(conductor: Conductor) {
        viewModelScope.launch {
            runCatching {
                vehiculoRepo.asignarConductor(vehiculoId, conductor.id, conductor.nombre)
            }.onSuccess { cerrarDialogoAsignar() }
        }
    }

    fun pedirConfirmacionRevisar(incidencia: Incidencia) { _incidenciaARevisar.value = incidencia }
    fun cancelarRevisar() { _incidenciaARevisar.value = null }

    /**
     * Quita el conductor actualmente asignado al vehículo.
     *
     * El repositorio limpia `conductorId` y `conductorNombre` en Firestore. El Flow
     * de [uiState] se actualiza automáticamente cuando Firestore confirma el cambio.
     */
    fun quitarConductor() {
        viewModelScope.launch {
            runCatching {
                vehiculoRepo.quitarConductor(vehiculoId)
            }
        }
    }

    /**
     * Marca la incidencia seleccionada como revisada usando los km actuales del vehículo
     * como snapshot — el ViewModel ya tiene el vehículo en memoria, evitando una lectura extra.
     */
    fun marcarRevisada() {
        val incidencia = _incidenciaARevisar.value ?: return
        val kmActuales = (_uiState.value as? VehiculoDetalleUiState.Ok)?.vehiculo?.km ?: 0
        viewModelScope.launch {
            runCatching {
                incidenciaRepo.marcarRevisada(vehiculoId, incidencia.id, kmActuales)
            }.onSuccess { cancelarRevisar() }
        }
    }
}
