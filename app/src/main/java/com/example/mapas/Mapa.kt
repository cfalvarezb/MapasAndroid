package com.example.mapas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Polyline
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson

class Mapa(mapa: GoogleMap, context: Context, var markerClickListener: GoogleMap.OnMarkerClickListener, var markerDragListener: GoogleMap.OnMarkerDragListener) {

    private var mMap: GoogleMap? = null
    private var context: Context? = null

    private var listaMarcadores: ArrayList<Marker>? = null

    var myPosition: LatLng? = null

    private var rutaMarcada: Polyline? = null

    //Marcadores de mapa
    private var marcadorGolden: Marker? = null
    private var marcadorPiramides: Marker? = null
    private var marcadorTorre: Marker? = null

    init {
        this.mMap = mapa
        this.context = context
    }

    fun dibujarLineas(){

        val coordenadasPolyLine = PolylineOptions()
            .add(LatLng(14.648211222425566, 20.26979450136423))
            .add(LatLng(13.548211222425566, 19.16979450136423))
            .add(LatLng(12.448211222425566, 18.06979450136423))
            .add(LatLng(11.348211222425566, 17.96979450136423))
            .pattern(arrayListOf<PatternItem>(Dot(), Gap(10f)))
            .color(Color.BLUE)
            .width(30f)
        mMap?.addPolyline(coordenadasPolyLine)

        val coordenadasPolygon = PolygonOptions()
            .add(LatLng(19.648211222425566, 20.26979450136423))
            .add(LatLng(18.548211222425566, 19.16979450136423))
            .add(LatLng(17.448211222425566, 18.06979450136423))
            .add(LatLng(16.348211222425566, 17.96979450136423))
            .strokePattern(arrayListOf<PatternItem>(Dash(10f), Gap(10f)))
            .strokeColor(Color.RED)
            .fillColor(Color.YELLOW)
            .strokeWidth(5f)

        mMap?.addPolygon(coordenadasPolygon)

        val coordenadasCirculo = CircleOptions()
            .center(LatLng(20.648211222425566,18.06979450136423 ))
            .radius(120000.0)
            .strokePattern(arrayListOf<PatternItem>(Dash(10f), Gap(10f)))
            .strokeWidth(15f)
            .strokeColor(Color.CYAN)
            .fillColor(Color.GREEN)

        mMap?.addCircle(coordenadasCirculo)

    }

    fun cambiarEstiloMapa(){
        //mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE //Aqui se escoge el tipo de mapa ya otorgado por el api

        val exitoCambioMapa = mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.estilo_mapa))

        if ( !exitoCambioMapa!!) {
            Toast.makeText(context, "Hubo un problema al cambiar el estilo del mapa", Toast.LENGTH_SHORT).show()
        }
    }

    fun crearListeners(){
        mMap?.setOnMarkerClickListener(markerClickListener)
        mMap?.setOnMarkerDragListener(markerDragListener)
    }

    fun marcadoresEstaticos(){

        val GOLDEN_GATE = LatLng(37.8199286,-122.4782551)
        val PIRAMIDES = LatLng(29.9772962,31.1324955)
        val TORRE_PISA = LatLng(43.722952,10.396597)

        marcadorGolden = mMap?.addMarker(
            MarkerOptions()
            .position(GOLDEN_GATE)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.tren))
            .snippet("Metro de San Francisco")
            .alpha(1f)
            .title("Golden Gate"))
        marcadorGolden?.tag = 0

        marcadorPiramides = mMap?.addMarker(
            MarkerOptions()
            .position(PIRAMIDES)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            .snippet("Ubicacion Piramides Giza")
            .alpha(0.6f)
            .title("Piramides Giza"))
        marcadorPiramides?.tag = 0

        marcadorTorre = mMap?.addMarker(
            MarkerOptions()
            .position(TORRE_PISA)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            .snippet("Ubicacion Torre pisa")
            .alpha(0.9f)
            .title("Torre Pisa"))
        marcadorTorre?.tag = 0

    }

    fun prepararMarcadores(){
        listaMarcadores = ArrayList()

        mMap?.setOnMapLongClickListener {
                location: LatLng ->

            listaMarcadores?.add(mMap!!.addMarker(
                MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .snippet("Nueva Ubicacion")
                .alpha(0.9f)
                .title("Nuevo Marcador")))

            listaMarcadores?.last()?.isDraggable = true //Para que el nuevo marcador creado se pueda arrastrar

            val coordenadas = LatLng(listaMarcadores?.last()!!.position.latitude, listaMarcadores?.last()!!.position.longitude)
            val origen = "origin=" + myPosition?.latitude + "," + myPosition?.longitude + "&"
            val destino = "destination=" + coordenadas?.latitude + "," + coordenadas?.longitude + "&"
            val parametros = origen + destino + "sensor=false&mode=driving&key=AIzaSyCRDQ5WTfzX69AptnhhfA1nhGw2eHULEto"
            cargarURL("https://maps.googleapis.com/maps/api/directions/json?" + parametros)
        }
    }

    private fun cargarURL(url: String){
        val queue = Volley.newRequestQueue(context)

        val solicitud = StringRequest(Request.Method.GET, url, Response.Listener<String> {
                response -> Log.d("HTTP", response)
            if ( !response.contains("REQUEST_DENIED") ) {
                val coordenadas = obtenerCoordenadas(response)
                dibujarRuta(coordenadas)
            }
        }, com.android.volley.Response.ErrorListener {
                error ->   Log.d("HTTP", error.toString())
        })

        queue.add(solicitud)
    }

    private fun dibujarRuta(coordenadas: PolylineOptions){
        if (rutaMarcada != null){
            rutaMarcada?.remove()
        }
        rutaMarcada = mMap?.addPolyline(coordenadas)
    }

    private fun obtenerCoordenadas (json: String): PolylineOptions{
        val gson = Gson()
        val objeto = gson.fromJson(json, com.example.mapas.Response::class.java)

        val puntos = objeto.routes?.get(0)!!.legs?.get(0)!!.steps!!

        var coordenadas = PolylineOptions()

        puntos.forEach {
            coordenadas.add(it.start_location?.toLatLng())
            coordenadas.add(it.end_location?.toLatLng())
        }

        coordenadas.color(Color.CYAN)
            .width(15f)

        return coordenadas
    }

    @SuppressLint("MissingPermission")
    fun configurarUbicacion(){
        mMap?.isMyLocationEnabled = true
        mMap?.uiSettings?.isMyLocationButtonEnabled = true
    }

    fun addMarcadorMyPosition(){
        mMap?.addMarker(MarkerOptions().position(myPosition).title("Aqui estoy!"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(myPosition))
    }

    private fun decodePoly(encoded: String) : List<GeoPoint>{
        val poly = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while ( index < len ) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while ( b >= 0x20 )
            val dlat = if ( result and 1 != 0 ) ( result shr 1 ).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while ( b >= 0x20 )
            val dlng = if ( result and 1 != 0 ) ( result shr 1 ).inv() else result shr 1
            lng += dlng

            val p = GeoPoint( (lat.toDouble() / 1E5 * 1E6).toInt().toDouble(), (lng.toDouble() / 1E5 * 1E6).toInt().toDouble() )
            poly.add(p)
        }
        return poly
    }

}