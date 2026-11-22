package com.example.capstonesample.security

import android.location.Location

enum class GeofenceStatus {
    INSIDE,
    OUTSIDE,
    DISABLED,
    NOT_CONFIGURED
}

data class GeofenceCheckResult(
    val status: GeofenceStatus,
    val isInside: Boolean,
    val distanceMeters: Float?,
    val allowedRadiusMeters: Float,
    val message: String
)

object GeofenceHelper {

    private const val DEFAULT_RADIUS_METERS = 200.0f

    /**
     * Retrieves the best available cached location from enabled location providers.
     */
    fun getLastKnownLocation(context: android.content.Context): Location? {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return null
        val providers = locationManager.getProviders(true)
        var freshest: Location? = null

        for (provider in providers) {
            val loc = try { locationManager.getLastKnownLocation(provider) } catch (_: Exception) { null } ?: continue
            if (freshest == null || loc.time > freshest.time) {
                freshest = loc
            }
        }
        return freshest
    }

    /**
     * Calculates the surface distance in meters between two GPS coordinates using Android Location APIs.
     */
    fun calculateDistance(
        userLat: Double,
        userLng: Double,
        targetLat: Double,
        targetLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, targetLat, targetLng, results)
        return results[0]
    }

    /**
     * Evaluates whether a user's location is within the defined project geofence boundary.
     */
    fun evaluateGeofence(
        userLat: Double?,
        userLng: Double?,
        fenceLat: Double?,
        fenceLng: Double?,
        radiusMeters: Double?,
        enabled: Boolean?
    ): GeofenceCheckResult {
        val isEnabled = enabled == true
        val allowedRadius = radiusMeters?.toFloat() ?: DEFAULT_RADIUS_METERS

        if (!isEnabled) {
            return GeofenceCheckResult(
                status = GeofenceStatus.DISABLED,
                isInside = true,
                distanceMeters = null,
                allowedRadiusMeters = allowedRadius,
                message = "Geofence verification is disabled for this project."
            )
        }

        if (fenceLat == null || fenceLng == null) {
            return GeofenceCheckResult(
                status = GeofenceStatus.NOT_CONFIGURED,
                isInside = true,
                distanceMeters = null,
                allowedRadiusMeters = allowedRadius,
                message = "Project site coordinates are not yet configured."
            )
        }

        if (userLat == null || userLng == null) {
            return GeofenceCheckResult(
                status = GeofenceStatus.OUTSIDE,
                isInside = false,
                distanceMeters = null,
                allowedRadiusMeters = allowedRadius,
                message = "GPS location is currently unavailable. Please enable device location."
            )
        }

        val distance = calculateDistance(userLat, userLng, fenceLat, fenceLng)
        val isInside = distance <= allowedRadius

        val roundedDistance = Math.round(distance * 10) / 10.0f
        return if (isInside) {
            GeofenceCheckResult(
                status = GeofenceStatus.INSIDE,
                isInside = true,
                distanceMeters = roundedDistance,
                allowedRadiusMeters = allowedRadius,
                message = "Verified On-Site: ${roundedDistance}m from project center (Limit: ${allowedRadius.toInt()}m)"
            )
        } else {
            val exceedBy = (roundedDistance - allowedRadius).toInt()
            GeofenceCheckResult(
                status = GeofenceStatus.OUTSIDE,
                isInside = false,
                distanceMeters = roundedDistance,
                allowedRadiusMeters = allowedRadius,
                message = "Outside Site Boundary: ${roundedDistance.toInt()}m from site (${exceedBy}m beyond ${allowedRadius.toInt()}m perimeter)"
            )
        }
    }
}
