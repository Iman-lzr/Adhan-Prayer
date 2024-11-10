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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

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

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://api.aladhan.com/")
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

        updateCurrentTime() // Appel à la fonction de mise à jour de l'heure
        checkAndUpdatePrayerTimes()

        // des listeners pour les switches pour activer les notifications
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
            // Si la date a changé, mettre à jour les horaires de prière
            lastCheckedDate = currentDate
            getUserLocation()  // Récupère la localisation et les horaires
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
                        // Mise à jour des horaires affichés
                        fajrTextView.text = it.Fajr
                        dhuhrTextView.text = it.Dhuhr
                        asrTextView.text = it.Asr
                        maghribTextView.text = it.Maghrib
                        ishaTextView.text = it.Isha

                        // Vérification des switches et planification des notifications
                        if (switchFajr.isChecked) {
                            schedulePrayerNotification(it.Fajr, "Fajr", 1)
                            triggerAdhan("Fajr")
                        }
                        if (switchDhuhr.isChecked) {
                            schedulePrayerNotification(it.Dhuhr, "Dhuhr", 2)
                            triggerAdhan("Dhuhr")
                        }
                        if (switchAsr.isChecked) {
                            schedulePrayerNotification(it.Asr, "Asr", 3)
                            triggerAdhan("Asr")
                        }
                        if (switchMaghrib.isChecked) {
                            schedulePrayerNotification(it.Maghrib, "Maghrib", 4)
                            triggerAdhan("Maghrib")
                        }
                        if (switchIsha.isChecked) {
                            schedulePrayerNotification(it.Isha, "Isha", 5)
                            triggerAdhan("Isha")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
                Log.e("PrayerTimes", "Error fetching prayer times", t)
                Toast.makeText(context, "Erreur lors de la récupération des horaires", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun schedulePrayerNotification(prayerTime: String, prayerName: String, requestCode: Int) {
        try {
            val prayerCalendar = Calendar.getInstance().apply {
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(prayerTime)
            }

            // Vérifier si l'heure de prière est déjà passée, sinon programmer l'alarme
            val currentTime = Calendar.getInstance()
            if (prayerCalendar.before(currentTime)) {
                prayerCalendar.add(Calendar.DATE, 1)  // Ajouter un jour si l'heure est déjà passée
            }

            val triggerTime = prayerCalendar.timeInMillis

            // Créer un PendingIntent unique pour chaque prière
            val intent = Intent(requireContext(), PrayerNotificationReceiver::class.java).apply {
                putExtra("prayerName", prayerName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            Log.e("PrayerTimes", "Invalid time format for $prayerName", e)
        }
    }


    private fun triggerAdhan(prayerName: String) {

        val intent = Intent(requireContext(), AdhanService::class.java).apply {
            putExtra("prayerName", prayerName)
        }
        requireContext().startService(intent)
    }

    private fun setupSwitchListeners() {
        switchFajr.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                schedulePrayerNotification(fajrTextView.text.toString(), "Fajr", 1)
                triggerAdhan("Fajr")
            }
        }
        switchDhuhr.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                schedulePrayerNotification(dhuhrTextView.text.toString(), "Dhuhr", 2)
                triggerAdhan("Dhuhr")
            }
        }
        switchAsr.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                schedulePrayerNotification(asrTextView.text.toString(), "Asr", 3)
                triggerAdhan("Asr")
            }
        }
        switchMaghrib.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                schedulePrayerNotification(maghribTextView.text.toString(), "Maghrib", 4)
                triggerAdhan("Maghrib")
            }
        }
        switchIsha.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                schedulePrayerNotification(ishaTextView.text.toString(), "Isha", 5)
                triggerAdhan("Isha")
            }
        }
    }

    private fun updateCurrentTime() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val currentTime = Calendar.getInstance().time
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                currentTimeTextView.text = sdf.format(currentTime)

                handler.postDelayed(this, 1000) // Mettre à jour chaque seconde
            }
        }
        handler.post(runnable)
    }
}
