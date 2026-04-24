package com.garagebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
