package mx.edu.utez.gpsmapas4e.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission")
@Composable
fun MapaScreen() {
    var concedido by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

    // Estados para el GPS Drawing
    var grabando by remember { mutableStateOf(false) }
    val trazosCompletados = remember { mutableStateListOf<List<LatLng>>() }
    val trazoActual = remember { mutableStateListOf<LatLng>() }

    // Cliente de ubicación y estado de la cámara
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(contexto) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(18.8500, -99.2022), 18f)
    }

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            concedido = true
        }
    }

    // Verificar permisos al abrir
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permissions)
        } else {
            concedido = true
        }
    }

    // Escuchar actualizaciones del GPS cuando se activa "grabando"
    DisposableEffect(concedido, grabando) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L) // Cada 3 segundos
            .setMinUpdateDistanceMeters(2f) // Solo guarda si te moviste al menos 2 metros
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val nuevoPunto = LatLng(loc.latitude, loc.longitude)
                    if (grabando) {
                        trazoActual.add(nuevoPunto)
                    }
                    // Mueve la cámara suavemente hacia donde vas caminando
                    cameraPositionState.move(CameraUpdateFactory.newLatLng(nuevoPunto))
                }
            }
        }

        if (concedido) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Interfaz: Mapa arriba, controles abajo
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
    ) {
        // MAPA (Ocupa todo el espacio disponible)
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                properties = MapProperties(
                    isMyLocationEnabled = concedido,
                    isTrafficEnabled = false
                )
            ) {
                // 1. Dibujar los trazos que ya terminaste
                trazosCompletados.forEach { listaPuntos ->
                    if (listaPuntos.size > 1) {
                        Polyline(
                            points = listaPuntos,
                            color = Color.Blue,
                            width = 12f
                        )
                    }
                }

                // 2. Dibujar el trazo que estás haciendo en este momento (en rojo)
                if (trazoActual.size > 1) {
                    Polyline(
                        points = trazoActual.toList(),
                        color = Color.Red,
                        width = 12f
                    )
                }
            }

            // Etiqueta flotante de estado
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (grabando) Color(0xFFD32F2F) else Color(0xFF424242)
            ) {
                Text(
                    text = if (grabando) "● DIBUJANDO (${trazoActual.size} pts)" else "⏸ EN PAUSA",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // PANEL DE 4 BOTONES
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fila 1: Dibujar/Pausar y Nuevo Trazo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { grabando = !grabando },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (grabando) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (grabando) "Pausar" else "Dibujar")
                    }

                    Button(
                        onClick = {
                            if (trazoActual.isNotEmpty()) {
                                trazosCompletados.add(trazoActual.toList())
                                trazoActual.clear()
                            }
                            grabando = false // Pausa para que camines al siguiente punto sin rayar
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nuevo Trazo")
                    }
                }

                // Fila 2: Deshacer y Limpiar todo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (trazoActual.isNotEmpty()) {
                                trazoActual.clear()
                            } else if (trazosCompletados.isNotEmpty()) {
                                trazosCompletados.removeAt(trazosCompletados.lastIndex)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deshacer")
                    }

                    OutlinedButton(
                        onClick = {
                            grabando = false
                            trazoActual.clear()
                            trazosCompletados.clear()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Limpiar Todo")
                    }
                }
            }
        }
    }
}