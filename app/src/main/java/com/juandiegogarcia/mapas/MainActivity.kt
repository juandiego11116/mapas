package com.juandiegogarcia.mapas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.juandiegogarcia.mapas.ui.theme.MapasTheme
import com.juandiegogarcia.mapas.ui.screens.MapScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity is the entry point of the application.
 *
 * It is annotated with @AndroidEntryPoint to enable Hilt dependency injection
 * and sets the UI content to the MapScreen composable using the app's theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sets the content of the activity to the MapScreen composable
        setContent {
            MapasTheme {
                MapScreen()
            }
        }
    }
}
