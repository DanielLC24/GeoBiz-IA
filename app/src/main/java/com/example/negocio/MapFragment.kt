package com.example.negocio
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private val defaultCenter = GeoPoint(19.0050, -98.2044)
    private val radioMetros = 1000.0
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var analysisCircle: Polygon? = null
    private var analysisCenter: GeoPoint? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            setupLocationOverlay()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = requireContext().packageName
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setCenter(defaultCenter)
        mapView.controller.setZoom(15.0)
        mapView.setMultiTouchControls(true)

        Toast.makeText(requireContext(), "Mantén presionado para definir un área de análisis", Toast.LENGTH_LONG).show()

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                BasicInfoWindow.closeAllInfoWindowsOn(mapView)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                analysisCenter = p
                dibujarCirculo(p, radioMetros)
                mapView.overlays.removeAll { it is Marker && it !is MyLocationNewOverlay }
                mapView.invalidate()
                Toast.makeText(requireContext(), "Nueva área de análisis definida", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        mapView.overlays.add(0, MapEventsOverlay(eventsReceiver))

        checkLocationPermission()

        view.findViewById<CardView>(R.id.business_menu_button).setOnClickListener { anchorView ->
            showBusinessMenu(anchorView)
        }

        view.findViewById<View>(R.id.btn_zoom_in).setOnClickListener { mapView.controller.zoomIn() }
        view.findViewById<View>(R.id.btn_zoom_out).setOnClickListener { mapView.controller.zoomOut() }
        view.findViewById<View>(R.id.btn_center).setOnClickListener {
            if (::locationOverlay.isInitialized && locationOverlay.myLocation != null) {
                mapView.controller.animateTo(locationOverlay.myLocation)
            } else {
                mapView.controller.animateTo(defaultCenter)
                mapView.controller.setZoom(15.0)
            }
        }
        mapView.invalidate()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationOverlay()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupLocationOverlay() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView).apply {
            enableMyLocation()
            runOnFirstFix {
                activity?.runOnUiThread {
                    mapView.controller.animateTo(myLocation)
                    mapView.controller.setZoom(16.0)
                }
            }
        }
        mapView.overlays.add(locationOverlay)
    }

    private fun showBusinessMenu(anchor: View) {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_business_menu, null)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        val dismiss = { popupWindow.dismiss() }

        // --- NUEVO: Botón de cerrar ---
        popupView.findViewById<View>(R.id.btn_close_popup).setOnClickListener { dismiss() }

        popupView.findViewById<View>(R.id.option_one).setOnClickListener { ejecutarLogicaIA("Restaurante"); dismiss() }
        popupView.findViewById<View>(R.id.option_two).setOnClickListener { ejecutarLogicaIA("Cafeteria"); dismiss() }
        popupView.findViewById<View>(R.id.option_three).setOnClickListener { ejecutarLogicaIA("Taller autos"); dismiss() }
        popupView.findViewById<View>(R.id.option_four).setOnClickListener { ejecutarLogicaIA("Farmacia"); dismiss() }
        popupView.findViewById<View>(R.id.option_five).setOnClickListener { ejecutarLogicaIA("Papeleria"); dismiss() }
        popupView.findViewById<View>(R.id.option_six).setOnClickListener { ejecutarLogicaIA("Floreria"); dismiss() }

        popupWindow.showAsDropDown(anchor)
    }

    private fun dibujarCirculo(centro: GeoPoint, radioMetros: Double) {
        analysisCircle?.let { mapView.overlays.remove(it) }
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.map_zone_stroke)
        val fillColor = ContextCompat.getColor(requireContext(), R.color.map_zone_fill)

        analysisCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(centro, radioMetros)
            fillPaint.color = fillColor
            outlinePaint.color = primaryColor
            outlinePaint.strokeWidth = 5f
        }
        mapView.overlays.add(analysisCircle)
        mapView.invalidate()
    }

    // ── Icono de color para marcadores de competencia ─────────────
    private fun createColoredMarkerIcon(color: Int): android.graphics.drawable.Drawable {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_custom_marker)!!.mutate()
        drawable.setTint(color)
        return drawable
    }

    private fun ejecutarLogicaIA(tipoSeleccionado: String) {
        val currentAnalysisCenter = analysisCenter
        if (currentAnalysisCenter == null) {
            Toast.makeText(requireContext(), "Primero define un área manteniendo presionado el mapa", Toast.LENGTH_LONG).show()
            return
        }

        val overlaysToRemove = mapView.overlays.filterIsInstance<Marker>()
            .filter { it !is MyLocationNewOverlay }
        mapView.overlays.removeAll(overlaysToRemove)
        mapView.invalidate()

        Toast.makeText(requireContext(), "Analizando zona con IA...", Toast.LENGTH_SHORT).show()

        // ── Obtener tipo para la API usando el Enum ───────────────
        val businessType = BusinessType.fromLabel(tipoSeleccionado)
        val tipoParaApi = businessType?.jsonKey ?: tipoSeleccionado

        // ── Mapeo tipo negocio → tipos OSM a marcar ───────────────
        val tiposOSM = when (tipoSeleccionado.lowercase()) {
            "restaurante"  -> listOf("restaurant", "fast_food", "food_court")
            "cafeteria"    -> listOf("cafe", "bar")
            "taller autos" -> listOf("fuel")
            "farmacia"     -> listOf("pharmacy")
            "papeleria"    -> listOf("shop")
            "floreria"     -> listOf("marketplace")
            else           -> listOf("restaurant")
        }

        // ── Colores por tipo ──────────────────────────────────────
        val colorPorTipo = mapOf(
            "restaurant"  to 0xFFE74C3C.toInt(),
            "fast_food"   to 0xFFE67E22.toInt(),
            "food_court"  to 0xFFE67E22.toInt(),
            "cafe"        to 0xFF8E44AD.toInt(),
            "bar"         to 0xFF2980B9.toInt(),
            "pharmacy"    to 0xFF27AE60.toInt(),
            "fuel"        to 0xFF7F8C8D.toInt(),
            "marketplace" to 0xFFF39C12.toInt(),
            "shop"        to 0xFF16A085.toInt()
        )

        lifecycleScope.launch {
            try {
                // ── 1. Score principal ────────────────────────────
                val responseScore = RetrofitClient.getApi(requireContext()).predecir(
                    lat   = currentAnalysisCenter.latitude,
                    lng   = currentAnalysisCenter.longitude,
                    tipo  = tipoParaApi,
                    radio = radioMetros.toInt()
                )

                // ── 2. Lugares cercanos por tipo ──────────────────
                tiposOSM.forEach { tipoOSM ->
                    val responseLugares = RetrofitClient.getApi(requireContext()).getLugaresCercanos(
                        lat   = currentAnalysisCenter.latitude,
                        lng   = currentAnalysisCenter.longitude,
                        tipo  = tipoOSM,
                        radio = radioMetros.toInt()
                    )

                    if (responseLugares.isSuccessful) {
                        val lugares = responseLugares.body()?.lugares ?: emptyList()
                        val color   = colorPorTipo[tipoOSM] ?: 0xFF95A5A6.toInt()

                        withContext(Dispatchers.Main) {
                            lugares.forEach { lugar ->
                                val marcador = Marker(mapView).apply {
                                    position = GeoPoint(lugar.lat[0], lugar.lng[0])
                                    title    = lugar.tipo[0].replaceFirstChar { it.uppercase() }
                                    snippet  = "Tipo: ${lugar.tipo[0]}"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon     = createColoredMarkerIcon(color)
                                    infoWindow = CustomMarkerInfoWindow(mapView)
                                }
                                mapView.overlays.add(marcador)
                            }
                            mapView.invalidate()
                        }
                    }
                }

                // ── 3. Marcador principal con score ───────────────
                if (responseScore.isSuccessful) {
                    val resultado = responseScore.body()!!
                    withContext(Dispatchers.Main) {
                        val marker = Marker(mapView).apply {
                            position = currentAnalysisCenter
                            title    = resultado.recomendacion[0]
                            snippet = buildString {
                                append("Score: ${resultado.score_final[0]}/100\n")
                                append("─────────────────\n")
                                append("Tráfico\n")
                                append("  Cafés: ${resultado.osm.cafes[0]}\n")
                                append("  Bares: ${resultado.osm.bares[0]}\n")
                                append("  Bus:   ${resultado.osm.paradas_bus[0]}\n")
                                append("─────────────────\n")
                                append("Competencia\n")
                                append("  Restaurantes: ${resultado.osm.restaurantes[0]}\n")
                                append("  Directa:      ${resultado.osm.competencia_directa[0]}\n")
                                append("─────────────────\n")
                                append("Socioec. INEGI\n")
                                append("  Índice: ${resultado.inegi.indice_socio[0]}\n")
                            }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            infoWindow = CustomMarkerInfoWindow(mapView)
                        }
                        mapView.overlays.add(marker)
                        marker.showInfoWindow()
                        mapView.invalidate()

                        Toast.makeText(
                            requireContext(),
                            "${resultado.recomendacion[0]} — Score: ${resultado.score_final[0]}/100",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("API_ERROR", "Error: ${e.javaClass.name}: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (::locationOverlay.isInitialized) locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (::locationOverlay.isInitialized) locationOverlay.disableMyLocation()
    }
}
