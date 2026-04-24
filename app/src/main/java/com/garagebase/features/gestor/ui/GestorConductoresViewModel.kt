package com.garagebase.features.gestor.ui

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
 * Estado observable de la pantalla de conductores.
 *
 * Combinamos [conductores] y [vehiculos] en un solo estado para que la UI pueda
 * mostrar la matrícula asignada a cada conductor sin una segunda consulta a Firestore:
 * busca en la lista de vehículos el que tenga `conductorId == conductor.id`.
 */
sealed class ConductoresUiState {
    object Cargando : ConductoresUiState()
    data class Ok(
        val conductores: List<Conductor>,
        val vehiculos: List<Vehiculo>
    ) : ConductoresUiState()
    data class Error(val mensaje: String) : ConductoresUiState()
}

/**
 * Estado del diálogo de alta de conductor.
 *
 * Solo se usa para crear conductores nuevos — la edición se hace desde [ConductorDetalleScreen].
 *
 * @property confirmando true cuando se muestra el paso de verificación antes de guardar.
 * @property guardando true mientras la escritura en Firestore está en vuelo.
 */
data class DialogoConductorState(
    val nombre: String = "",
    val telefono: String = "",
    val confirmando: Boolean = false,
    val guardando: Boolean = false
)

/**
 * ViewModel de la pantalla de conductores del gestor.
 *
 * Observa en tiempo real las colecciones de conductores y vehículos para que el listado
 * siempre esté actualizado. Las escrituras (add/update) son operaciones puntuales que
 * usan `suspend` y se lanzan en [viewModelScope].
 */
class GestorConductoresViewModel : ViewModel() {

    private val conductorRepo = ConductorRepositoryImpl()
    private val vehiculoRepo = VehiculoRepositoryImpl()

    private val _uiState = MutableStateFlow<ConductoresUiState>(ConductoresUiState.Cargando)
    val uiState: StateFlow<ConductoresUiState> = _uiState.asStateFlow()

    /** Estado del diálogo de alta/edición; null = diálogo cerrado. */
    private val _dialogo = MutableStateFlow<DialogoConductorState?>(null)
    val dialogo: StateFlow<DialogoConductorState?> = _dialogo.asStateFlow()

    init {
        viewModelScope.launch {
            // combine emite cada vez que cualquiera de los dos flows emite un valor nuevo.
            // Así, si el gestor añade un vehículo en otra pantalla, la lista de conductores
            // también se actualiza (la matrícula asignada aparece al instante).
            combine(conductorRepo.getAll(), vehiculoRepo.getAll()) { conductores, vehiculos ->
                ConductoresUiState.Ok(conductores, vehiculos)
            }.catch { e ->
                _uiState.value = ConductoresUiState.Error(e.message ?: "Error al cargar conductores")
            }.collect { _uiState.value = it }
        }
    }

    /** Abre el diálogo de alta con los campos vacíos. */
    fun abrirDialogoNuevo() { _dialogo.value = DialogoConductorState() }

    fun cerrarDialogo() { _dialogo.value = null }
    fun actualizarNombre(v: String) { _dialogo.update { it?.copy(nombre = v) } }
    fun actualizarTelefono(v: String) { _dialogo.update { it?.copy(telefono = v) } }
    fun pedirConfirmacion() { _dialogo.update { it?.copy(confirmando = true) } }
    fun cancelarConfirmacion() { _dialogo.update { it?.copy(confirmando = false) } }

    /**
     * Persiste el conductor en Firestore (crea o actualiza según [DialogoConductorState.id]).
     *
     * **`runCatching`**: ejecuta el bloque suspend y captura cualquier excepción en un `Result<T>`,
     * evitando que un fallo de red mate la coroutine sin control.
     * - `onSuccess` → cierra el diálogo (flujo normal).
     * - `onFailure` → quita el spinner para permitir reintentar. En una app más madura se
     *   mostraría un Snackbar, pero eso requiere `SnackbarHostState` pasado desde la Activity;
     *   se deja como mejora para la sección 7 (Calidad).
     *
     * Si es alta con [DialogoConductorState.vehiculoId], asigna el vehículo con una segunda
     * escritura secuencial — no transacción, porque la consistencia eventual es aceptable
     * aquí: si la asignación falla, el gestor puede reasignar desde el detalle del vehículo.
     */
    /**
     * Crea el nuevo conductor en Firestore.
     *
     * La asignación de vehículo se gestiona desde [ConductorDetalleScreen], no aquí.
     * Separar alta y asignación simplifica el flujo: primero el conductor existe, luego
     * el gestor puede asignarle un vehículo desde su pantalla de detalle.
     */
    fun guardar() {
        val d = _dialogo.value ?: return
        _dialogo.update { it?.copy(guardando = true) }
        viewModelScope.launch {
            runCatching {
                conductorRepo.add(d.nombre.trim(), d.telefono.trim())
            }.onSuccess {
                cerrarDialogo()
            }.onFailure {
                _dialogo.update { it?.copy(guardando = false) }
            }
        }
    }
}
