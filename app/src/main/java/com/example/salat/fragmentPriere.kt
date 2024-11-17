package com.example.salat

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PrayerTimesFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var cityTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private lateinit var fajrTextView: TextView
    private lateinit var dhuhrTextView: TextView
    private lateinit var asrTextView: TextView
    private lateinit var maghribTextView: TextView
    private lateinit var ishaTextView: TextView

    private lateinit var switchFajr: Switch
    private lateinit var switchDhuhr: Switch
    private lateinit var switchAsr: Switch
    private lateinit var switchMaghrib: Switch
    private lateinit var switchIsha: Switch

    private val REQUEST_LOCATION_PERMISSION = 1

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.aladhan.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val prayerTimesAPI = retrofit.create(PrayerTimesAPI::class.java)

    private var lastCheckedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_priere, container, false)

        // Initialisation des vues
        cityTextView = rootView.findViewById(R.id.cityTextView)
        dateTextView = rootView.findViewById(R.id.dateTextView)
        currentTimeTextView = rootView.findViewById(R.id.heuretextview)
        fajrTextView = rootView.findViewById(R.id.fajrTextView)
        dhuhrTextView = rootView.findViewById(R.id.dhuhrTextView)
        asrTextView = rootView.findViewById(R.id.asrTextView)
        maghribTextView = rootView.findViewById(R.id.maghribTextView)
        ishaTextView = rootView.findViewById(R.id.ishaTextView)

        switchFajr = rootView.findViewById(R.id.fajrSwitch)
        switchDhuhr = rootView.findViewById(R.id.dhuhrSwitch)
        switchAsr = rootView.findViewById(R.id.asrSwitch)
        switchMaghrib = rootView.findViewById(R.id.maghribSwitch)
        switchIsha = rootView.findViewById(R.id.ishaSwitch)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        createNotificationChannel()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        updateCurrentTime()
        checkAndUpdatePrayerTimes()

        setupSwitchListeners()

        return rootView
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Prayer Times Notifications"
            val descriptionText = "Notifications for prayer times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("PRAYER_TIMES_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val city = addresses[0].locality ?: "Ville inconnue"
                        val country = addresses[0].countryName ?: "Pays inconnu"

                        cityTextView.text = "$city, $country"
                        dateTextView.text = getCurrentDate()

                        fetchPrayerTimes(latitude, longitude)
                    } else {
                        cityTextView.text = "Ville inconnue"
                    }
                } catch (e: Exception) {
                    cityTextView.text = "Erreur de localisation"
                }
            } else {
                cityTextView.text = "Localisation non disponible"
            }
        }
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun checkAndUpdatePrayerTimes() {
        val currentDate = getCurrentDate()
        if (currentDate != lastCheckedDate) {
            lastCheckedDate = currentDate
            getUserLocation()
        }
    }

    private fun fetchPrayerTimes(latitude: Double, longitude: Double) {
        prayerTimesAPI.getPrayerTimes(latitude, longitude).enqueue(object : Callback<PrayerTimesResponse> {
            override fun onResponse(
                call: Call<PrayerTimesResponse>,
                response: Response<PrayerTimesResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val prayerTimes = response.body()?.data?.timings
                    prayerTimes?.let {
                        fajrTextView.text = it.Fajr ?: "Inconnu"
                        dhuhrTextView.text = it.Dhuhr ?: "Inconnu"
                        asrTextView.text = it.Asr ?: "Inconnu"
                        maghribTextView.text = it.Maghrib ?: "Inconnu"
                        ishaTextView.text = it.Isha ?: "Inconnu"

                        if (switchFajr.isChecked) {
                            it.Fajr?.let { it1 -> schedulePrayerNotificationWithWorkManager(it1, "Fajr", 1) }
                        }
                        if (switchDhuhr.isChecked) {
                            it.Dhuhr?.let { it1 -> schedulePrayerNotificationWithWorkManager(it1, "Dhuhr", 2) }
                        }
                        if (switchAsr.isChecked) {
                            it.Asr?.let { it1 -> schedulePrayerNotificationWithWorkManager(it1, "Asr", 3) }
                        }
                        if (switchMaghrib.isChecked) {
                            it.Maghrib?.let { it1 -> schedulePrayerNotificationWithWorkManager(it1, "Maghrib", 4) }
                        }
                        if (switchIsha.isChecked) {
                            it.Isha?.let { it1 -> schedulePrayerNotificationWithWorkManager(it1, "Isha", 5) }
                        }
                    }
                } else {
                    Log.e("PrayerTimes", "Erreur API: ${response.code()}")
                    Toast.makeText(context, "Erreur lors de la récupération des horaires", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
                Log.e("PrayerTimes", "Erreur lors de la récupération des horaires", t)
                Toast.makeText(context, "Erreur lors de la récupération des horaires", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun schedulePrayerNotificationWithWorkManager(prayerTime: String, prayerName: String, id: Int) {
        try {
            val prayerCalendar = Calendar.getInstance()
            prayerCalendar.time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(prayerTime)

            // Si l'heure de la prière est déjà passée, planifiez-la pour le jour suivant.
            if (prayerCalendar.timeInMillis < System.currentTimeMillis()) {
                prayerCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delay = prayerCalendar.timeInMillis - System.currentTimeMillis()

            val inputData = Data.Builder()
                .putString("prayerName", prayerName)
                .build()

            val workRequest = OneTimeWorkRequest.Builder(PrayerNotificationWorker::class.java)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(requireContext()).enqueue(workRequest)

        } catch (e: Exception) {
            Log.e("PrayerTimes", "Erreur de planification de la notification avec WorkManager", e)
        }
    }


    private fun setupSwitchListeners() {
        switchFajr.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                fajrTextView.text?.let { schedulePrayerNotificationWithWorkManager(it.toString(), "Fajr", 1) }
            }
        }
        switchDhuhr.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dhuhrTextView.text?.let { schedulePrayerNotificationWithWorkManager(it.toString(), "Dhuhr", 2) }
            }
        }
        switchAsr.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                asrTextView.text?.let { schedulePrayerNotificationWithWorkManager(it.toString(), "Asr", 3) }
            }
        }
        switchMaghrib.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                maghribTextView.text?.let { schedulePrayerNotificationWithWorkManager(it.toString(), "Maghrib", 4) }
            }
        }
        switchIsha.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ishaTextView.text?.let { schedulePrayerNotificationWithWorkManager(it.toString(), "Isha", 5) }
            }
        }
    }
    private fun updateCurrentTime() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // Get the current time and format it to HH:mm:ss
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                currentTimeTextView.text = currentTime
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        handler.post(runnable)
    }

}

