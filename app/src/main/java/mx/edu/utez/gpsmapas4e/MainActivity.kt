package mx.edu.utez.gpsmapas4e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.edu.utez.gpsmapas4e.ui.screens.MapaScreen
import mx.edu.utez.gpsmapas4e.ui.theme.GPSMapas4ETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPSMapas4ETheme {
                MapaScreen()
            }
        }
    }
}

