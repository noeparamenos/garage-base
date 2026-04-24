package com.garagebase.features.gestor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garagebase.core.model.Vehiculo
import com.garagebase.features.vehiculos.data.VehiculoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Estado observable del listado de vehículos.
 */
sealed class VehiculosUiState {
    object Cargando : VehiculosUiState()
    data class Ok(val vehiculos: List<Vehiculo>) : VehiculosUiState()
    data class Error(val mensaje: String) : VehiculosUiState()
}

/**
 * ViewModel del listado de vehículos del gestor.
 *
 * Gestiona el Flow de tiempo real y el estado del diálogo para añadir un vehículo nuevo.
 */
class GestorVehiculosViewModel : ViewModel() {

    private val vehiculoRepo = VehiculoRepositoryImpl()

    private val _uiState = MutableStateFlow<VehiculosUiState>(VehiculosUiState.Cargando)
    val uiState: StateFlow<VehiculosUiState> = _uiState.asStateFlow()

    /** Controla la visibilidad del diálogo para añadir vehículo. */
    private val _mostrarDialogoAnadir = MutableStateFlow(false)
    val mostrarDialogoAnadir: StateFlow<Boolean> = _mostrarDialogoAnadir.asStateFlow()

    init {
        viewModelScope.launch {
            vehiculoRepo.getAll()
                .catch { e -> _uiState.value = VehiculosUiState.Error(e.message ?: "Error al cargar vehículos") }
                .collect { _uiState.value = VehiculosUiState.Ok(it) }
        }
    }

    fun abrirDialogoAnadir() { _mostrarDialogoAnadir.value = true }
    fun cerrarDialogoAnadir() { _mostrarDialogoAnadir.value = false }

    /**
     * Crea el vehículo en Firestore y cierra el diálogo al terminar.
     *
     * @param matricula Matrícula del nuevo vehículo.
     */
    fun anadir(matricula: String) {
        viewModelScope.launch {
            runCatching { vehiculoRepo.add(matricula) }
                .onSuccess { cerrarDialogoAnadir() }
        }
    }
}
