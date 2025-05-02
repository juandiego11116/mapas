package com.juandiegogarcia.mapas

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.juandiegogarcia.mapas.data.AppDatabase
import com.juandiegogarcia.mapas.model.FavoritePlace
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.Dispatchers

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val zoomState = remember { mutableStateOf(12.0) }
    val currentStyle = remember { mutableStateOf(Style.MAPBOX_STREETS) }
    var showDialog by remember { mutableStateOf(false) }
    var newPlaceName by remember { mutableStateOf("") }
    var newPoint by remember { mutableStateOf<Point?>(null) }
    val db = remember { AppDatabase.getInstance(context) }
    val dao = db.favoritePlaceDao()
    val coroutineScope = rememberCoroutineScope()
    val pointAnnotationManager = remember { mapView.annotations.createPointAnnotationManager() }
    val savedAnnotations = remember { mutableStateMapOf<Point, String>() }
    val favoritePlaces: List<FavoritePlace> by dao.getAllFlow().collectAsState(initial = emptyList())
    val markers = mutableListOf<PointAnnotation>()
    var isAlertPoint by remember { mutableStateOf(false) }
    var showPointTypeDialog by remember { mutableStateOf(false) }
    var tappedPoint by remember { mutableStateOf<Point?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getLastKnownLocation(fusedLocationClient) {
                it?.let {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(it.longitude, it.latitude))
                            .zoom(zoomState.value)
                            .build()
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLastKnownLocation(fusedLocationClient) {
                it?.let {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(it.longitude, it.latitude))
                            .zoom(zoomState.value)
                            .build()

                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) {
            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                val rawBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.red_marker)
                val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, 512, 512, true)
                style.addImage("red_marker", scaledBitmap)
                val alertBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alert_marker)
                val scaledAlert = Bitmap.createScaledBitmap(alertBitmap, 512, 512, true)
                style.addImage("alert_marker", scaledAlert)
                val geoJsonSource = geoJsonSource("ne_50m_populated_places_simple") {
                    url("asset://ne_50m_populated_places_simple.geojson")
                }
                style.addSource(geoJsonSource)

                val geoJsonLayer = SymbolLayer("geojson-layer", "ne_50m_populated_places_simple")
                    .iconImage("red_marker")
                    .iconSize(0.4)
                    .iconAllowOverlap(true)
                    .iconAnchor(IconAnchor.BOTTOM)

                style.addLayer(geoJsonLayer)

                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                    locationPuck = LocationPuck2D(
                        bearingImage = null,
                        shadowImage = null,
                        scaleExpression = interpolate {
                            linear()
                            zoom()
                            stop { literal(0.0); literal(0.6) }
                            stop { literal(20.0); literal(1.0) }
                        }.toString()
                    )
                }

                mapView.getMapboxMap().addOnCameraChangeListener {
                    val zoom = mapView.getMapboxMap().cameraState.zoom
                    zoomState.value = zoom
                    val newSize = currentZoomToIconSize(zoom)
                    markers.forEach {
                        it.iconSize = newSize
                        pointAnnotationManager.update(it)
                    }
                }

                mapView.getMapboxMap().addOnMapClickListener { point ->
                    tappedPoint = point
                    showPointTypeDialog = true
                    true
                }


                pointAnnotationManager.addClickListener { clickedAnnotation ->
                    val point = clickedAnnotation.point
                    if (savedAnnotations.containsKey(point)) {
                        val name = savedAnnotations[point] ?: "(sin nombre)"
                        Toast.makeText(context, "📍 Lugar: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        newPoint = point
                        showDialog = true
                    }
                    true
                }
            }
        }

        if (showPointTypeDialog && tappedPoint != null) {
            AlertDialog(
                onDismissRequest = { showPointTypeDialog = false },
                title = { Text("Tipo de punto") },
                text = { Text("¿Qué tipo de punto deseas agregar?") },
                confirmButton = {
                    Button(onClick = {
                        tappedPoint?.let { point ->
                            isAlertPoint = false
                            newPoint = point
                            showDialog = true
                        }
                        showPointTypeDialog = false
                    }) {
                        Text("Punto normal")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        tappedPoint?.let { point ->
                            isAlertPoint = true
                            newPoint = point
                            showDialog = true
                        }
                        showPointTypeDialog = false
                    }) {
                        Text("Punto alerta 🚨")
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
            }

            LazyColumn(modifier = Modifier.weight(0.4f)) {
                items(favoritePlaces) { place ->
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(place.name)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val point = Point.fromLngLat(place.longitude, place.latitude)
                                    val icon = if (place.isAlert) "alert_marker" else "red_marker"

                                    pointAnnotationManager.create(
                                        PointAnnotationOptions()
                                            .withPoint(point)
                                            .withIconImage(icon)
                                            .withIconSize(currentZoomToIconSize(zoomState.value))
                                    )

                                    mapView.getMapboxMap().setCamera(
                                        CameraOptions.Builder()
                                            .center(point)
                                            .zoom(zoomState.value)
                                            .build()
                                    )
                                }) {
                                    Text("Ir")
                                }
                                Button(onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        dao.delete(place)
                                    }
                                }) {
                                    Text("🗑️")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showDialog && newPoint != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Nombre del lugar") },
                text = {
                    TextField(
                        value = newPlaceName,
                        onValueChange = { newPlaceName = it },
                        label = { Text("Ej. Mi casa") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            newPoint?.let { point ->
                                val icon = if (isAlertPoint) "alert_marker" else "red_marker"
                                val annotation = pointAnnotationManager.create(
                                    PointAnnotationOptions()
                                        .withPoint(point)
                                        .withIconImage(icon)
                                        .withIconSize(currentZoomToIconSize(zoomState.value))
                                )
                                savedAnnotations[point] = newPlaceName
                                dao.insert(
                                    FavoritePlace(
                                        name = newPlaceName,
                                        latitude = point.latitude(),
                                        longitude = point.longitude(),
                                        isAlert = isAlertPoint
                                    )
                                )

                            }
                            launch(Dispatchers.Main) {
                                showDialog = false
                                newPlaceName = ""
                            }
                        }
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Botón cambiar de mapa
        FloatingActionButton(
            onClick = {
                currentStyle.value = if (currentStyle.value == Style.MAPBOX_STREETS)
                    Style.SATELLITE
                else
                    Style.MAPBOX_STREETS

                mapView.getMapboxMap().loadStyleUri(currentStyle.value) {
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("🗺️")
        }

        // Botón volver a mi ubicación
        FloatingActionButton(
            onClick = {
                getLastKnownLocation(fusedLocationClient) {
                    it?.let {
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(it.longitude, it.latitude))
                                .zoom(zoomState.value)
                                .build()
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("📍")
        }

        // ➕ Botón Zoom
        FloatingActionButton(
            onClick = {
                val newZoom = (zoomState.value + 1).coerceAtMost(22.0)
                mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(newZoom).build())
                zoomState.value = newZoom
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("+")
        }

        // ➖ Botón Zoom
        FloatingActionButton(
            onClick = {
                val newZoom = (zoomState.value - 1).coerceAtLeast(3.0)
                mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(newZoom).build())
                zoomState.value = newZoom
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 88.dp)
        ) {
            Text("-")
        }
    }
}

@SuppressLint("MissingPermission")
private fun getLastKnownLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    callback: (Location?) -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { callback(it) }
        .addOnFailureListener { callback(null) }
}

private fun currentZoomToIconSize(zoom: Double): Double {
    return when {
        zoom < 5 -> 0.2
        zoom < 10 -> 0.35
        zoom < 15 -> 0.55
        else -> 0.8
    }
}
