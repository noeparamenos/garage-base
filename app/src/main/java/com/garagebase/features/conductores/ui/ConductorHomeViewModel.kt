package com.garagebase.features.conductores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garagebase.core.model.Conductor
import com.garagebase.core.model.Incidencia
import com.garagebase.core.model.Vehiculo
import com.garagebase.features.conductores.data.ConductorRepositoryImpl
import com.garagebase.features.incidencias.data.IncidenciaRepositoryImpl
import com.garagebase.features.vehiculos.data.VehiculoRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Estados posibles de la pantalla principal del conductor.
 */
sealed class ConductorHomeUiState {
    /** Cargando datos iniciales de Firestore. */
    object Cargando : ConductorHomeUiState()

    /**
     * El conductor existe pero aún no tiene vehículo asignado.
     * La UI muestra un mensaje orientativo en lugar de datos vacíos.
     *
     * @param conductorNombre Nombre del conductor para el saludo.
     */
    data class SinVehiculo(val conductorNombre: String) : ConductorHomeUiState()

    /**
     * Datos cargados y vehículo asignado.
     *
     * @param conductor Perfil del conductor (necesario para crear incidencias con el snapshot de nombre).
     * @param vehiculo Vehículo asignado con km, horas e historial de actualizaciones.
     * @param incidencias Lista completa del vehículo, ordenada de más reciente a más antigua.
     */
    data class Ok(
        val conductor: Conductor,
        val vehiculo: Vehiculo,
        val incidencias: List<Incidencia>
    ) : ConductorHomeUiState()

    /** Error irrecuperable (ej. fallo de red, documento no encontrado). */
    data class Error(val mensaje: String) : ConductorHomeUiState()
}

/**
 * ViewModel de la pantalla principal del conductor.
 *
 * Combina tres Flows de Firestore en un único [uiState]:
 * 1. El perfil del conductor ([ConductorRepositoryImpl.findById]).
 * 2. El vehículo asignado ([VehiculoRepositoryImpl.getVehiculoAsignado]).
 * 3. Las incidencias del vehículo ([IncidenciaRepositoryImpl.getDeVehiculo]).
 *
 * **`flatMapLatest`**: cuando el vehículo asignado cambia (el gestor reasigna),
 * cancela el Flow de incidencias anterior y abre uno nuevo con el nuevo vehiculoId.
 * Sin `flatMapLatest` se quedarían dos listeners de Firestore activos a la vez.
 *
 * El [uid] se obtiene directamente de Firebase Auth porque en este punto de la navegación
 * la sesión está garantizada — SplashScreen no llega aquí si no hay usuario autenticado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConductorHomeViewModel : ViewModel() {

    private val uid: String = checkNotNull(FirebaseAuth.getInstance().currentUser?.uid) {
        "ConductorHomeViewModel creado sin sesión activa"
    }

    private val conductorRepo = ConductorRepositoryImpl()
    private val vehiculoRepo = VehiculoRepositoryImpl()
    private val incidenciaRepo = IncidenciaRepositoryImpl()

    private val _uiState = MutableStateFlow<ConductorHomeUiState>(ConductorHomeUiState.Cargando)

    /** Estado observable por la UI. Solo lectura fuera del ViewModel. */
    val uiState: StateFlow<ConductorHomeUiState> = _uiState.asStateFlow()

    /** Controla la visibilidad del diálogo de actualización de km y horas. */
    private val _dialogoActualizar = MutableStateFlow(false)
    val dialogoActualizar: StateFlow<Boolean> = _dialogoActualizar.asStateFlow()

    /** Controla la visibilidad del diálogo para añadir una incidencia. */
    private val _dialogoIncidencia = MutableStateFlow(false)
    val dialogoIncidencia: StateFlow<Boolean> = _dialogoIncidencia.asStateFlow()

    /**
     * Error temporal vinculado a la validación o escritura de un diálogo.
     * Se limpia al abrir o cerrar cualquier diálogo.
     */
    private val _errorDialogo = MutableStateFlow<String?>(null)
    val errorDialogo: StateFlow<String?> = _errorDialogo.asStateFlow()

    init {
        viewModelScope.launch {
            // Primer login: el gestor creó el documento con ID auto-generado.
            // Antes de observar, migramos ese documento al UID real de Firebase Auth
            // para que findById(uid) y getVehiculoAsignado(uid) funcionen correctamente.
            val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber
            if (phone != null) {
                runCatching { conductorRepo.vincularPorTelefono(uid, phone) }
                    .onFailure { e ->
                        _uiState.value = ConductorHomeUiState.Error(
                            e.message ?: "Error al inicializar el perfil."
                        )
                        return@launch
                    }
            }
            observarDatos()
        }
    }

    /**
     * Arranca la observación en tiempo real de conductor, vehículo e incidencias.
     *
     * Se llama siempre después de [vincularPorTelefono], cuando ya se garantiza que
     * el documento del conductor en Firestore usa el UID correcto como ID.
     */
    private fun observarDatos() {
        viewModelScope.launch {
            combine(
                conductorRepo.findById(uid),
                vehiculoRepo.getVehiculoAsignado(uid)
            ) { conductor, vehiculo -> Pair(conductor, vehiculo) }
                .flatMapLatest { (conductor, vehiculo) ->
                    when {
                        conductor == null ->
                            // El gestor aún no ha dado de alta a este conductor.
                            flowOf(ConductorHomeUiState.Error(
                                "Tu número no está registrado en el sistema.\nContacta con el gestor."
                            ))
                        vehiculo == null ->
                            flowOf(ConductorHomeUiState.SinVehiculo(conductor.nombre))
                        else ->
                            // Cada vez que cambia el vehículo asignado, flatMapLatest
                            // cancela el listener anterior y abre uno nuevo.
                            incidenciaRepo.getDeVehiculo(vehiculo.id).map { incidencias ->
                                ConductorHomeUiState.Ok(conductor, vehiculo, incidencias)
                            }
                    }
                }
                .catch { e ->
                    _uiState.value = ConductorHomeUiState.Error(
                        e.message ?: "Error al cargar los datos."
                    )
                }
                .collect { _uiState.value = it }
        }
    }

    // ── Diálogo: actualizar km y horas ────────────────────────────────────────

    fun abrirDialogoActualizar() {
        _errorDialogo.value = null
        _dialogoActualizar.value = true
    }

    fun cerrarDialogoActualizar() {
        _dialogoActualizar.value = false
        _errorDialogo.value = null
    }

    /**
     * Valida y guarda los nuevos km y horas del vehículo.
     *
     * Las reglas de validación son invariantes del dominio:
     * - Los km y las horas solo pueden aumentar (un vehículo no viaja hacia atrás).
     * - Errores de validación se emiten en [errorDialogo] para mostrarlos en el diálogo.
     *
     * @param kmTexto Valor introducido por el usuario en el campo de km.
     * @param horasTexto Valor introducido por el usuario en el campo de horas (admite coma o punto decimal).
     */
    fun actualizarKmYHoras(kmTexto: String, horasTexto: String) {
        val estado = _uiState.value as? ConductorHomeUiState.Ok ?: return
        val km = kmTexto.trim().toIntOrNull()
        val horas = horasTexto.trim().replace(',', '.').toFloatOrNull()

        if (km == null || horas == null) {
            _errorDialogo.value = "Introduce valores numéricos válidos."
            return
        }
        if (km < estado.vehiculo.km) {
            _errorDialogo.value =
                "Los km no pueden ser menores que los actuales (%,d km).".format(estado.vehiculo.km)
            return
        }
        if (horas < estado.vehiculo.horas) {
            _errorDialogo.value =
                "Las horas no pueden ser menores que las actuales (%.1f h).".format(estado.vehiculo.horas)
            return
        }

        viewModelScope.launch {
            runCatching { vehiculoRepo.actualizarKmYHoras(estado.vehiculo.id, km, horas) }
                .onSuccess { cerrarDialogoActualizar() }
                .onFailure { e ->
                    _errorDialogo.value = e.message ?: "Error al guardar. Inténtalo de nuevo."
                }
        }
    }

    // ── Diálogo: añadir incidencia ────────────────────────────────────────────

    fun abrirDialogoIncidencia() {
        _errorDialogo.value = null
        _dialogoIncidencia.value = true
    }

    fun cerrarDialogoIncidencia() {
        _dialogoIncidencia.value = false
        _errorDialogo.value = null
    }

    /**
     * Crea una incidencia nueva con el snapshot del momento actual.
     *
     * Los campos inmutables se toman del estado ya cargado en memoria:
     * - [conductorId] y [conductorNombre]: identifican quién reporta (snapshot inmutable).
     * - [kmAlReportar]: los km actuales del vehículo al momento del reporte (snapshot inmutable).
     * - [fecha]: [Instant.now()] — hora del dispositivo; aceptable aquí porque es un reporte
     *   de usuario, no una transacción financiera.
     *
     * @param descripcion Texto libre con la descripción del problema.
     */
    fun añadirIncidencia(descripcion: String) {
        val estado = _uiState.value as? ConductorHomeUiState.Ok ?: return
        if (descripcion.isBlank()) {
            _errorDialogo.value = "La descripción no puede estar vacía."
            return
        }

        val incidencia = Incidencia(
            id = "",   // Firestore genera el ID con add()
            vehiculoId = estado.vehiculo.id,
            descripcion = descripcion.trim(),
            fecha = Instant.now(),
            conductorId = uid,
            conductorNombre = estado.conductor.nombre,
            kmAlReportar = estado.vehiculo.km,
            revisada = false,
            fechaRevisada = null,
            kmAlRevisar = null
        )

        viewModelScope.launch {
            runCatching { incidenciaRepo.add(incidencia) }
                .onSuccess { cerrarDialogoIncidencia() }
                .onFailure { e ->
                    _errorDialogo.value = e.message ?: "Error al guardar. Inténtalo de nuevo."
                }
        }
    }
}
