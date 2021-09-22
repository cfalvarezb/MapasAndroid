package com.example.mapas

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.mapas.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener {

    private lateinit var binding: ActivityMapsBinding

    private val permisoFineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val permisoCoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION

    private val CODIGO_SOLICITUD_PERMISO = 100

    var fusedLocationProviderClient: FusedLocationProviderClient? = null

    var locationRequest: LocationRequest? = null

    var callback: LocationCallback? = null

    private var mapa: Mapa? = null
    private val markerListener = this
    private val dragListener = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        inicializarLocationRequest()
    }

    private fun inicializarLocationRequest(){
        locationRequest = LocationRequest()
        locationRequest?.interval = 10000
        locationRequest?.fastestInterval = 5000
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
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
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {

        mapa = Mapa(googleMap, applicationContext, markerListener, dragListener)

        mapa?.cambiarEstiloMapa()
        mapa?.marcadoresEstaticos()
        mapa?.crearListeners()
        mapa?.prepararMarcadores()
        mapa?.dibujarLineas()

    }


    override fun onMarkerDragStart(marker: Marker) {
        Toast.makeText(this, "Empezando a mover el marcador",Toast.LENGTH_SHORT).show()
        Log.d("MARCADOR INICIAL", marker.position.latitude.toString() + " - " + marker.position.longitude.toString())
    }

    override fun onMarkerDrag(marker: Marker) {
        title = marker.position.latitude.toString() + " - " + marker.position.longitude.toString()
    }

    override fun onMarkerDragEnd(marker: Marker) {
        Toast.makeText(this, "Acabo el elemento de drag & drop",Toast.LENGTH_SHORT).show()
        Log.d("MARCADOR FINAL", marker.position.latitude.toString() + " - " + marker.position.longitude.toString())
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        var numeroCLicks = marker?.tag as? Int

        if ( numeroCLicks != null ) {
            marker?.tag = ++numeroCLicks
            Toast.makeText(this, "Se han dado " + numeroCLicks.toString() + " clicks",Toast.LENGTH_SHORT).show()
        }

        return false
    }

    private fun validarPermisosUbicacion(): Boolean{
        val hayUbicacionPrecisa = ActivityCompat.checkSelfPermission(this, permisoFineLocation) == PackageManager.PERMISSION_GRANTED
        val hayUbicacionOrdinaria = ActivityCompat.checkSelfPermission(this, permisoCoarseLocation) == PackageManager.PERMISSION_GRANTED
        return hayUbicacionPrecisa && hayUbicacionOrdinaria
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion(){

        callback = object : LocationCallback () {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                if (mapa != null){

                    mapa?.configurarUbicacion()

                    locationResult?.locations?.forEach {
                        // Add a marker in actual position and move the camera
                        mapa?.myPosition = LatLng(it.latitude, it.longitude)
                        //mapa?.addMarcadorMyPosition()
                    }
                }
            }
        }

        fusedLocationProviderClient?.requestLocationUpdates(locationRequest, callback, null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun pedirPermisos(){
        val deboProveerContexto = ActivityCompat.shouldShowRequestPermissionRationale(this, permisoFineLocation)

        if (deboProveerContexto){
            //mandar un mensaje con explicacion a dicional
            solicitudPermiso()
        } else {
            solicitudPermiso()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun solicitudPermiso(){
        requestPermissions(arrayOf(permisoFineLocation,permisoCoarseLocation),CODIGO_SOLICITUD_PERMISO)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CODIGO_SOLICITUD_PERMISO -> {
                if ( grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    //Obtener ubicacion
                    obtenerUbicacion()
                } else {
                    Toast.makeText(this, "No diste permiso para acceder a ubicacion", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun detenerActualizacionUbicacion(){
        fusedLocationProviderClient?.removeLocationUpdates(callback)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        if (validarPermisosUbicacion()){
            obtenerUbicacion()
        } else {
            pedirPermisos()
        }
    }

    override fun onPause() {
        super.onPause()
        detenerActualizacionUbicacion()
    }

}