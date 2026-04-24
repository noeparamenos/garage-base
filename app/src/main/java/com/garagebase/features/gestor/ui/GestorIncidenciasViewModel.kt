package com.garagebase.features.gestor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garagebase.core.model.Incidencia
import com.garagebase.core.model.Vehiculo
import com.garagebase.features.incidencias.data.IncidenciaRepositoryImpl
import com.garagebase.features.vehiculos.data.VehiculoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Estado observable del listado global de incidencias pendientes.
 *
 * Se incluye la lista de [vehiculos] para poder mostrar la matrícula de cada incidencia
 * sin una query adicional:
 * - la UI busca `vehiculos.find { it.id == incidencia.vehiculoId }`.
 */
sealed class IncidenciasGestorUiState {
    object Cargando : IncidenciasGestorUiState()
    data class Ok(
        val pendientes: List<Incidencia>,
        val vehiculos: List<Vehiculo>
    ) : IncidenciasGestorUiState()
    data class Error(val mensaje: String) : IncidenciasGestorUiState()
}

/**
 * ViewModel del listado global de incidencias pendientes del gestor.
 *
 * Usa `collectionGroup` de Firestore (a través de [IncidenciaRepositoryImpl.getPendientes])
 * para buscar incidencias en todos los vehículos a la vez, sin iterar vehículo a vehículo.
 */
class GestorIncidenciasViewModel : ViewModel() {

    private val incidenciaRepo = IncidenciaRepositoryImpl()
    private val vehiculoRepo = VehiculoRepositoryImpl()

    private val _uiState = MutableStateFlow<IncidenciasGestorUiState>(IncidenciasGestorUiState.Cargando)
    val uiState: StateFlow<IncidenciasGestorUiState> = _uiState.asStateFlow()

    /** Incidencia seleccionada para ver su detalle / confirmar revisión. null = panel cerrado. */
    private val _seleccionada = MutableStateFlow<Incidencia?>(null)
    val seleccionada: StateFlow<Incidencia?> = _seleccionada.asStateFlow()

    init {
        viewModelScope.launch {
            combine(incidenciaRepo.getPendientes(), vehiculoRepo.getAll()) { pendientes, vehiculos ->
                IncidenciasGestorUiState.Ok(pendientes, vehiculos)
            }.catch { e ->
                _uiState.value = IncidenciasGestorUiState.Error(e.message ?: "Error al cargar incidencias")
            }.collect { _uiState.value = it }
        }
    }

    fun seleccionar(incidencia: Incidencia) { _seleccionada.value = incidencia }
    fun cerrarDetalle() { _seleccionada.value = null }

    /**
     * Marca la incidencia seleccionada como revisada.
     *
     * Los km actuales se obtienen del estado en memoria para evitar una lectura extra
     * a Firestore y la posible race condition que conllevaría.
     */
    fun marcarRevisada() {
        val inc = _seleccionada.value ?: return
        val ok = _uiState.value as? IncidenciasGestorUiState.Ok ?: return
        val km = ok.vehiculos.firstOrNull { it.id == inc.vehiculoId }?.km ?: 0
        viewModelScope.launch {
            runCatching { incidenciaRepo.marcarRevisada(inc.vehiculoId, inc.id, km) }
                .onSuccess { cerrarDetalle() }
        }
    }
}
