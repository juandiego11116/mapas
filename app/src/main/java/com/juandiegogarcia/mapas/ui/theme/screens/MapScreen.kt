package com.juandiegogarcia.mapas.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.juandiegogarcia.mapas.R
import com.juandiegogarcia.mapas.utils.getLastKnownLocation
import com.juandiegogarcia.mapas.viewmodel.MapViewModel
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
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.collectAsState
import com.juandiegogarcia.mapas.model.FavoritePlace

/**
 * Main map screen that displays a Mapbox map and manages favorite and alert points.
 *
 * Features:
 * - Displays user location.
 * - Allows adding normal or alert markers on map click.
 * - Saves markers to local Room database.
 * - Displays a collapsible bottom sheet with saved favorite places.
 * - Buttons to zoom, recenter, and switch map style.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    // Map and marker state
    val mapView = remember { MapView(context) }
    val pointAnnotationManager = remember { mapView.annotations.createPointAnnotationManager() }
    val markers = remember { mutableListOf<PointAnnotation>() }
    val viewModel: MapViewModel = hiltViewModel()

    // Bottom sheet and UI state
    val scaffoldState = rememberBottomSheetScaffoldState()
    val bottomPadding = 120.dp
    val favoritePlaces: List<FavoritePlace> by viewModel.favoritePlaces.collectAsState(initial = emptyList())

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getLastKnownLocation(fusedLocationClient) {
                it?.centerCamera(mapView, viewModel.zoomLevel)
            }
        }
    }

    // Initial location permission and camera positioning
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLastKnownLocation(fusedLocationClient) {
                it?.centerCamera(mapView, viewModel.zoomLevel)
            }
        }
    }

    // Bottom sheet that shows saved favorite places
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Favoritos", style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(favoritePlaces) { place ->
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(place.name)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val point = Point.fromLngLat(place.longitude, place.latitude)
                                        val icon = if (place.isAlert) "alert_marker" else "red_marker"
                                        pointAnnotationManager.create(
                                            PointAnnotationOptions()
                                                .withPoint(point)
                                                .withIconImage(icon)
                                                .withIconSize(currentZoomToIconSize(viewModel.zoomLevel))
                                        )
                                        mapView.getMapboxMap().setCamera(
                                            CameraOptions.Builder()
                                                .center(point)
                                                .zoom(viewModel.zoomLevel)
                                                .build()
                                        )
                                    }) { Text("Ir") }
                                    Button(onClick = { viewModel.deletePlace(place) }) {
                                        Text("🗑️")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        // Main map content and floating buttons
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) {
                mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                    // Add icons to style
                    val redMarkerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.red_marker)
                    val scaledRed = Bitmap.createScaledBitmap(redMarkerBitmap, 400, 400, true)
                    style.addImage("red_marker", scaledRed)

                    val alertMarkerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alert_marker)
                    val scaledAlert = Bitmap.createScaledBitmap(alertMarkerBitmap, 400, 400, true)
                    style.addImage("alert_marker", scaledAlert)

                    // Optional: load GeoJSON
                    val geoJsonSource = geoJsonSource("geo_source") {
                        url("asset://ne_50m_populated_places_simple.geojson")
                    }
                    style.addSource(geoJsonSource)

                    style.addLayer(
                        SymbolLayer("geojson-layer", "geo_source")
                            .iconImage("red_marker")
                            .iconSize(0.4)
                            .iconAllowOverlap(true)
                            .iconAnchor(IconAnchor.BOTTOM)
                    )

                    // Enable location puck
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

                    // Update icon size on zoom
                    mapView.getMapboxMap().addOnCameraChangeListener {
                        val zoom = mapView.getMapboxMap().cameraState.zoom
                        viewModel.updateZoom(zoom)
                        val size = currentZoomToIconSize(zoom)
                        markers.forEach { it.iconSize = size; pointAnnotationManager.update(it) }
                    }

                    // Handle map click
                    mapView.getMapboxMap().addOnMapClickListener { point ->
                        viewModel.tappedPoint = point
                        viewModel.showPointTypeDialog = true
                        true
                    }

                    // Handle marker click
                    pointAnnotationManager.addClickListener { annotation ->
                        val point = annotation.point
                        if (viewModel.savedAnnotations.containsKey(point)) {
                            Toast.makeText(context, "📍 ${viewModel.savedAnnotations[point]}", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.newPoint = point
                            viewModel.showDialog = true
                        }
                        true
                    }
                }
            }

            // Floating buttons: zoom in, zoom out, recenter, switch style
            FloatingActionButton(
                onClick = {
                    val newZoom = (viewModel.zoomLevel + 1).coerceAtMost(22.0)
                    mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(newZoom).build())
                    viewModel.updateZoom(newZoom)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = bottomPadding)
            ) { Text("+") }

            FloatingActionButton(
                onClick = {
                    val newZoom = (viewModel.zoomLevel - 1).coerceAtLeast(3.0)
                    mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(newZoom).build())
                    viewModel.updateZoom(newZoom)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = bottomPadding + 72.dp) // separa verticalmente
            ) { Text("-") }

            FloatingActionButton(
                onClick = {
                    getLastKnownLocation(fusedLocationClient) {
                        it?.centerCamera(mapView, viewModel.zoomLevel)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(end = 16.dp, bottom = bottomPadding)

            ) { Text("📍") }

            FloatingActionButton(
                onClick = {
                    val newStyle = if (viewModel.currentStyleUri == Style.MAPBOX_STREETS)
                        Style.SATELLITE
                    else
                        Style.MAPBOX_STREETS

                    viewModel.currentStyleUri = newStyle
                    mapView.getMapboxMap().getStyle { style ->
                        val currentUri = style.styleURI
                        val newStyle = if (currentUri == Style.MAPBOX_STREETS)
                            Style.SATELLITE
                        else
                            Style.MAPBOX_STREETS

                        mapView.getMapboxMap().loadStyleUri(newStyle)
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) { Text("🗺️") }
        }

        // Dialogs for choosing point type and naming
        if (viewModel.showPointTypeDialog && viewModel.tappedPoint != null) {
            AlertDialog(
                onDismissRequest = { viewModel.showPointTypeDialog = false },
                title = { Text("Tipo de punto") },
                text = { Text("¿Qué tipo de punto deseas agregar?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.isAlertPoint = false
                        viewModel.newPoint = viewModel.tappedPoint
                        viewModel.showDialog = true
                        viewModel.showPointTypeDialog = false
                    }) { Text("Punto normal") }
                },
                dismissButton = {
                    Button(onClick = {
                        viewModel.isAlertPoint = true
                        viewModel.newPoint = viewModel.tappedPoint
                        viewModel.showDialog = true
                        viewModel.showPointTypeDialog = false
                    }) { Text("Punto alerta 🚨") }
                }
            )
        }

        if (viewModel.showDialog && viewModel.newPoint != null) {
            AlertDialog(
                onDismissRequest = { viewModel.resetDialog() },
                title = { Text("Nombre del lugar") },
                text = {
                    TextField(
                        value = viewModel.newPlaceName,
                        onValueChange = { viewModel.newPlaceName = it },
                        label = { Text("Ej. Mi casa") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.newPoint?.let { point ->
                            val icon = if (viewModel.isAlertPoint) "alert_marker" else "red_marker"
                            pointAnnotationManager.create(
                                PointAnnotationOptions()
                                    .withPoint(point)
                                    .withIconAnchor(IconAnchor.BOTTOM)
                                    .withIconImage(icon)
                                    .withIconSize(currentZoomToIconSize(viewModel.zoomLevel))
                            )
                            viewModel.savePlace(viewModel.newPlaceName, point, viewModel.isAlertPoint)
                        }
                        viewModel.resetDialog()
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    Button(onClick = { viewModel.resetDialog() }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

private fun Location.centerCamera(mapView: MapView, zoom: Double) {
    val point = Point.fromLngLat(longitude, latitude)
    mapView.getMapboxMap().setCamera(
        CameraOptions.Builder().center(point).zoom(zoom).build()
    )
}

private fun currentZoomToIconSize(zoom: Double): Double {
    return when {
        zoom < 5 -> 0.2
        zoom < 10 -> 0.35
        zoom < 15 -> 0.55
        else -> 0.8
    }
}
