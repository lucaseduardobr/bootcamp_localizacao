package com.frances.myapplication.bootcamp_localizacao

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.frances.myapplication.bootcamp_localizacao.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import java.util.*

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {



    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var lastLocation : Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private var locationUpdateState = false

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE  = 1
        private const val REQUEST_CHECK_SETTINGS  = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //estanciando essa variavl para usar esee comportamento descrito no seu corpo
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(p0: LocationResult)
                {
                    super.onLocationResult(p0)

                    lastLocation = p0.lastLocation

                    placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                }
        }

        createLocationRequest()





    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap



        //zoom no mapa
        map.getUiSettings().setZoomControlsEnabled(true)
        //abre uma janelinha do lado do zoom in e zomm out
        //para poder definir uma rota ou abrir google maps
        map.setOnMarkerClickListener(this)

        setUpMap()
    }

    override fun onMarkerClick(p0: Marker?) =false




    private fun setUpMap(){
        //pergunta se tem acesso a localizacao
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)

            return

        }
        //habilita minha localizacao
        map.isMyLocationEnabled = true
        //tipo de mapa (imagem de satelite por ex)
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        
        
        // verifica se acho ultima localizacao
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            if (location != null) {

                lastLocation = location
                //aqui pega localizacao
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                //aqui direciona para a localizacao desejada
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

            }

        }

    }

    //coloca o marcador vermelho que existe
    //no googlz maps
    private fun placeMarkerOnMap (location: LatLng) {
        val markerOptions = MarkerOptions() .position (location)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(
            BitmapFactory.decodeResource(resources,R.mipmap.ic_user_location
            )))
        val titleStr = getAddress(location)
        markerOptions.title(titleStr)

        map.addMarker (markerOptions)
    }
    //para dizer o endereço , estado que o cara esta
    private fun getAddress (latLng: LatLng): String {

        val geocoder: Geocoder

        val addresses: List<Address>

        geocoder = Geocoder(  this, Locale.getDefault())

        //maxResults significa que vai pegar o primeiro
        // nome de endereco
        //mas ele pode retornar cidade
        //estado...
        addresses = geocoder.getFromLocation (latLng. latitude, latLng.longitude,  1)

        val address = addresses [0].getAddressLine( 0)

        val city = addresses [0].locality

        val state = addresses [0].adminArea

        val country = addresses [0].countryName

        val postalCode = addresses [0].postalCode

        return address

    }



    private fun startLocationUpdates () {

            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions( this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE)

            return
        }

        //fica atualizando as variavies
        //locationRequest e locationCallback
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, null )

    }

    private fun createLocationRequest() {

        locationRequest = LocationRequest()

        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        //pra gente ter a localizacao
        val client = LocationServices.getSettingsClient(this)

        val task = client.checkLocationSettings (builder.build())

                task.addOnSuccessListener {

                    locationUpdateState = true

                    startLocationUpdates()
                }

        task.addOnFailureListener { e ->
             if (e is ResolvableApiException) {

            try {

                e.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
            } catch (sendEx: IntentSender.SendIntentException) {}
        }

        }

    }
    //se o usuario nao estiver usando nossa
    //app a gente pde para nao ficar
    //atualizando a localizacoa
    //para nao gastar energia
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if(!locationUpdateState){
            startLocationUpdates()
        }
    }










}







