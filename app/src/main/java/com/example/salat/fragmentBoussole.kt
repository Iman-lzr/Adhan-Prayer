package com.example.salat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CompassFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var accelerometer: Sensor? = null
    private lateinit var compassImageView: ImageView
    private lateinit var qiblaMessageTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private var currentLocation: Location? = null

    private val qiblaLatitude = 21.4225  // Latitude de la Kaaba
    private val qiblaLongitude = 39.8262 // Longitude de la Kaaba

    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_boussole, container, false)

        compassImageView = rootView.findViewById(R.id.compassImageView)
        qiblaMessageTextView = rootView.findViewById(R.id.qiblaMessageTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        sensorManager = activity?.getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Demander la permission pour obtenir la localisation
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation() // Récupérer la position si la permission est accordée
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        return rootView
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
            }
        }
    }

    private fun calculateQiblaAzimuth(latitude: Double, longitude: Double): Float {
        val deltaLongitude = Math.toRadians(qiblaLongitude - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(qiblaLatitude)

        val x = sin(deltaLongitude) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLongitude)

        return Math.toDegrees(atan2(x, y).toDouble()).toFloat()
    }

    private fun isAlignedWithQibla(userAzimuth: Float): Boolean {
        val qiblaAzimuth = calculateQiblaAzimuth(currentLocation?.latitude ?: 0.0, currentLocation?.longitude ?: 0.0)
        return Math.abs(userAzimuth - qiblaAzimuth) < 10  // Tolérance de 10 degrés
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values
        }

        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)

            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)

                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                // Rotation de l'image de la boussole
                compassImageView.rotation = azimuth

                // Vérification de l'alignement avec la Qibla
                if (currentLocation != null && isAlignedWithQibla(azimuth)) {
                    qiblaMessageTextView.text = "Vous êtes bien orienté vers la Qibla!"
                    qiblaMessageTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                } else {
                    qiblaMessageTextView.text = "Alignez-vous vers la Qibla."
                    qiblaMessageTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Aucune gestion particulière pour l'exactitude des capteurs
    }
}
