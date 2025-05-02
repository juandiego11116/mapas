package com.juandiegogarcia.mapas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MyMapApp is the base Application class for the app.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * including the base class for the application that serves as the
 * dependency container.
 *
 * This is required to use Hilt for dependency injection across the app.
 */
@HiltAndroidApp
class MyMapApp : Application()
