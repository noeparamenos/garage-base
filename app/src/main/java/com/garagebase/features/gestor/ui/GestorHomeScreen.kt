package com.garagebase.features.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.garagebase.navigation.Screen
import com.garagebase.ui.theme.GarageBaseTheme

/**
 * Menú principal del gestor con accesos a las tres secciones de gestión.
 *
 * No tiene ViewModel propio porque no mantiene estado: solo navega.
 *
 * @param navController Controlador de navegación de la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestorHomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Panel del gestor") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionCard(
                titulo = "Conductores",
                subtitulo = "Añadir, editar y asignar conductores a vehículos"
            ) { navController.navigate(Screen.GestorConductores.route) }

            SeccionCard(
                titulo = "Vehículos",
                subtitulo = "Ver flota, km, horas e incidencias por vehículo"
            ) { navController.navigate(Screen.GestorVehiculos.route) }

            SeccionCard(
                titulo = "Incidencias pendientes",
                subtitulo = "Revisar y marcar incidencias de toda la flota"
            ) { navController.navigate(Screen.GestorIncidencias.route) }

            // Empuja "Mi vehículo" al fondo de la pantalla
            Spacer(Modifier.weight(1f))

            SeccionCard(
                titulo = "Mi vehículo",
                subtitulo = "Ver y actualizar el vehículo asignado a tu propia cuenta",
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) { navController.navigate(Screen.ConductorHome.route) }
        }
    }
}

/**
 * Tarjeta pulsable para cada sección del menú del gestor.
 *
 * @param titulo Nombre de la sección.
 * @param subtitulo Descripción breve de lo que se puede hacer.
 * @param colors Colores de la tarjeta; por defecto los colores estándar de Card.
 * @param onClick Acción al pulsar la tarjeta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeccionCard(
    titulo: String,
    subtitulo: String,
    colors: CardColors = CardDefaults.cardColors(),
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = colors
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.titleLarge)
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GestorHomeScreenPreview() {
    GarageBaseTheme { GestorHomeScreen(navController = rememberNavController()) }
}
