package com.juandiegogarcia.mapas.utils

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
/**
 * Utility function to retrieve the last known location from the device.
 *
 * This method uses the FusedLocationProviderClient to access the most recent location available.
 * It suppresses the missing permission warning, assuming the caller has already checked and granted permissions.
 *
 * @param fusedLocationClient The location provider client used to access device location.
 * @param callback A lambda that receives the retrieved Location, or null if unavailable.
 */
@SuppressLint("MissingPermission")
fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClient,
    callback: (Location?) -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { callback(it) }     // Call with location if successful
        .addOnFailureListener { callback(null) }   // Call with null if failed
}
