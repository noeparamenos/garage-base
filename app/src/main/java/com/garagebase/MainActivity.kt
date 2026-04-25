package com.garagebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.rememberNavController
import com.garagebase.navigation.NavGraph
import com.garagebase.ui.theme.GarageBaseTheme

/**
 * Única Activity de la app (patrón Single Activity).
 * Toda la navegación ocurre dentro del [NavGraph] mediante Compose Navigation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GarageBaseTheme {
                // La imagen ocupa toda la pantalla como capa base.
                // Los Scaffold de cada pantalla tienen background = Color.Transparent
                // (configurado en el tema), así que la imagen se ve a través de ellos.
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.fondo_garagebase),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.5f
                    )
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
