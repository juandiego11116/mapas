package com.juandiegogarcia.mapas.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juandiegogarcia.mapas.model.FavoritePlace
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.juandiegogarcia.mapas.data.FavoritePlaceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel responsible for managing the UI state of the map screen.
 *
 * It interacts with the database via FavoritePlaceDao to persist favorite places,
 * and exposes UI-related state variables for user interaction like zoom, map style,
 * dialogs, and selected points.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val dao: FavoritePlaceDao
) : ViewModel() {

    // Internal cache for local modifications (not currently synced to DB)
    private val _favoritePlaces = MutableStateFlow<List<FavoritePlace>>(emptyList())

    // Reactive stream of favorite places from the database
    val favoritePlaces = dao.getAllFlow()

    // Current map style URI (e.g., streets, satellite)
    var currentStyleUri: String = Style.MAPBOX_STREETS

    // Zoom level for the map
    var zoomLevel by mutableStateOf(12.0)

    // Flags to control visibility of dialogs
    var showDialog by mutableStateOf(false)
    var showPointTypeDialog by mutableStateOf(false)

    // State for new place being added
    var newPlaceName by mutableStateOf("")
    var newPoint by mutableStateOf<Point?>(null)
    var isAlertPoint by mutableStateOf(false)

    // Last tapped point on the map
    var tappedPoint by mutableStateOf<Point?>(null)

    // Memory of annotations saved during the session (name by location)
    var savedAnnotations = mutableStateMapOf<Point, String>()

    /**
     * Updates the zoom level of the map.
     */
    fun updateZoom(zoom: Double) {
        zoomLevel = zoom
    }

    /**
     * Deletes a favorite place from the local list (note: does not affect DB).
     */
    fun deletePlace(place: FavoritePlace) {
        viewModelScope.launch {
            _favoritePlaces.value = _favoritePlaces.value.filterNot { it == place }
        }
    }

    /**
     * Saves a new place to the local list and adds it to the in-memory annotations.
     * This method does not persist to the database yet.
     */
    fun savePlace(name: String, point: Point, isAlert: Boolean) {
        viewModelScope.launch {
            val place = FavoritePlace(name = name, latitude = point.latitude(), longitude = point.longitude(), isAlert = isAlert)
            _favoritePlaces.value = _favoritePlaces.value + place
            savedAnnotations[point] = name
        }
    }

    /**
     * Resets the dialog state and temporary place data.
     */
    fun resetDialog() {
        newPlaceName = ""
        newPoint = null
        showDialog = false
        showPointTypeDialog = false
    }
}
