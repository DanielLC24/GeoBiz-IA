package com.example.negocio

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private val ubicacionFCC = GeoPoint(19.0050, -98.2044)
    private val radioMetros = 1000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setCenter(ubicacionFCC)
        mapView.controller.setZoom(15.0)
        mapView.setMultiTouchControls(true)

        dibujarCirculo(ubicacionFCC, radioMetros)
        mapView.invalidate()

        val businessMenuButton = findViewById<CardView>(R.id.business_menu_button)
        businessMenuButton.setOnClickListener { view ->
            showBusinessMenu(view)
        }

        findViewById<View>(R.id.btn_zoom_in).setOnClickListener { mapView.controller.zoomIn() }
        findViewById<View>(R.id.btn_zoom_out).setOnClickListener { mapView.controller.zoomOut() }
        findViewById<View>(R.id.btn_center).setOnClickListener {
            mapView.controller.animateTo(ubicacionFCC)
            mapView.controller.setZoom(15.0)
        }
    }

    private fun showBusinessMenu(anchor: View) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_business_menu, null)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true

        val dismiss = { popupWindow.dismiss() }
        popupView.findViewById<View>(R.id.option_one).setOnClickListener { ejecutarLogicaIA("Restaurante"); dismiss() }
        popupView.findViewById<View>(R.id.option_two).setOnClickListener { ejecutarLogicaIA("Cafeteria"); dismiss() }
        popupView.findViewById<View>(R.id.option_three).setOnClickListener { ejecutarLogicaIA("Taller autos"); dismiss() }
        popupView.findViewById<View>(R.id.option_four).setOnClickListener { ejecutarLogicaIA("Farmacia"); dismiss() }
        popupView.findViewById<View>(R.id.option_five).setOnClickListener { ejecutarLogicaIA("Papeleria"); dismiss() }
        popupView.findViewById<View>(R.id.option_six).setOnClickListener { ejecutarLogicaIA("Floreria"); dismiss() }

        popupWindow.showAsDropDown(anchor)
    }

    private fun dibujarCirculo(centro: GeoPoint, radioMetros: Double) {
        val primaryColor = ContextCompat.getColor(this, R.color.map_zone_stroke)
        val fillColor = ContextCompat.getColor(this, R.color.map_zone_fill)
        val puntos = Polygon.pointsAsCircle(centro, radioMetros)
        val polygon = Polygon(mapView).apply {
            setPoints(puntos)
            outlinePaint.color = primaryColor
            outlinePaint.strokeWidth = 5f
            fillPaint.color = fillColor
        }
        mapView.overlays.add(polygon)
    }

    private fun ejecutarLogicaIA(tipoSeleccionado: String) {
        val puntosCompetencia = mutableListOf<GeoPoint>()

        try {
            val inputStream = assets.open("datos_fcc.csv")
            val reader = inputStream.bufferedReader(Charsets.ISO_8859_1)

            mapView.overlays.clear()
            dibujarCirculo(ubicacionFCC, radioMetros)

            reader.readLines().forEach { linea ->
                val columnas = linea.split(",")
                if (columnas.size >= 4 && columnas[3].contains(tipoSeleccionado, ignoreCase = true)) {
                    val lat = columnas[0].toDouble()
                    val lon = columnas[1].toDouble()
                    val nombre = columnas[2]
                    val posicion = GeoPoint(lat, lon)
                    puntosCompetencia.add(posicion)

                    val marker = Marker(mapView).apply {
                        position = posicion
                        title = nombre
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        infoWindow = CustomMarkerInfoWindow(mapView)
                    }
                    mapView.overlays.add(marker)
                }
            }

            if (puntosCompetencia.isNotEmpty()) {
                val avgLat = puntosCompetencia.map { it.latitude }.average()
                val avgLon = puntosCompetencia.map { it.longitude }.average()
                val puntoSugerido = GeoPoint(avgLat + 0.002, avgLon + 0.002)

                val markerOptimo = Marker(mapView).apply {
                    position = puntoSugerido
                    title = "UBICACIÓN ÓPTIMA"
                    snippet = "Analizado: ${puntosCompetencia.size} locales cerca"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    infoWindow = CustomMarkerInfoWindow(mapView)
                }
                mapView.overlays.add(markerOptimo)
                mapView.controller.animateTo(puntoSugerido)
                mapView.controller.setZoom(15.0)
                mapView.invalidate()
                Toast.makeText(this, "Análisis de densidad completado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No hay competencia de $tipoSeleccionado en la zona", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error con el archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
