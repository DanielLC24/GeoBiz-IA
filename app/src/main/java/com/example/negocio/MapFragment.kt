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
import java.util.Locale

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private val defaultCenter = GeoPoint(19.0050, -98.2044)
    private val radioMetros = 1000.0
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var analysisCircle: Polygon? = null
    private var analysisCenter: GeoPoint? = null
    private var skipMyLocationAutoCenter: Boolean = false

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

        val prefs = requireContext().getSharedPreferences("geobiz_session", Context.MODE_PRIVATE)
        skipMyLocationAutoCenter = prefs.getBoolean("restore_map_pending", false)

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

        val shouldRestore = prefs.getBoolean("restore_map_pending", false)
        if (shouldRestore && prefs.contains("last_lat") && prefs.contains("last_lng")) {
            val lat = prefs.getFloat("last_lat", defaultCenter.latitude.toFloat()).toDouble()
            val lng = prefs.getFloat("last_lng", defaultCenter.longitude.toFloat()).toDouble()
            val business = prefs.getString("last_business_label", null)
            val zoom = prefs.getFloat("last_zoom", 15.5f).toDouble()

            prefs.edit().putBoolean("restore_map_pending", false).apply()
            val point = GeoPoint(lat, lng)
            analysisCenter = point
            dibujarCirculo(point, radioMetros)
            mapView.controller.setZoom(zoom)
            mapView.controller.setCenter(point)
            mapView.overlays.removeAll { it is Marker && it !is MyLocationNewOverlay }
            mapView.invalidate()
            if (!business.isNullOrBlank()) {
                ejecutarLogicaIA(business)
            }
            skipMyLocationAutoCenter = false
        }
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
                    if (!skipMyLocationAutoCenter) {
                        mapView.controller.animateTo(myLocation)
                        mapView.controller.setZoom(16.0)
                    }
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

        fun displayNameForOsmType(tipoOSM: String): String = when (tipoOSM.lowercase()) {
            "restaurant" -> "Restaurante"
            "fast_food" -> "Fonda"
            "food_court" -> "Comida"
            "cafe" -> "Café"
            "bar" -> "Bar"
            "pharmacy" -> "Farmacia"
            "fuel" -> "Gasolinera"
            "shop" -> "Tienda"
            "marketplace" -> "Mercado"
            "car_repair" -> "Taller"
            "supermarket" -> "Supermercado"
            else -> "Lugar"
        }

        fun formatTipoLabel(raw: String): String =
            raw.replace('_', ' ').replaceFirstChar { it.uppercase() }

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
                val totalsByTipo = mutableMapOf<String, Int>()
                tiposOSM.forEach { tipoOSM ->
                    val responseLugares = RetrofitClient.getApi(requireContext()).getLugaresCercanos(
                        lat   = currentAnalysisCenter.latitude,
                        lng   = currentAnalysisCenter.longitude,
                        tipo  = tipoOSM,
                        radio = radioMetros.toInt()
                    )

                    if (responseLugares.isSuccessful) {
                        val body = responseLugares.body()
                        val total = body?.total?.firstOrNull() ?: body?.lugares?.size ?: 0
                        totalsByTipo[tipoOSM] = total
                        val lugares = body?.lugares ?: emptyList()
                        val color   = colorPorTipo[tipoOSM] ?: 0xFF95A5A6.toInt()

                        withContext(Dispatchers.Main) {
                            lugares.forEachIndexed { index, lugar ->
                                val marcador = Marker(mapView).apply {
                                    position = GeoPoint(lugar.lat[0], lugar.lng[0])
                                    title    = "${displayNameForOsmType(tipoOSM)} ${index + 1}"
                                    snippet  = "Tipo: ${formatTipoLabel(lugar.tipo[0])}"
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
                        val totalSeleccionado = tiposOSM.sumOf { totalsByTipo[it] ?: 0 }
                        val cafes = totalsByTipo["cafe"] ?: resultado.osm.cafes.firstOrNull() ?: 0
                        // Posible competencia secundaria según negocio seleccionado
                        val (compTipo, compLabel) = when (tipoSeleccionado.lowercase()) {
                            "restaurante"  -> "bar" to "Bares"
                            "cafeteria"    -> "fast_food" to "Fondas"
                            "taller autos" -> "car_repair" to "Talleres"
                            "farmacia"     -> "supermarket" to "Supermercados"
                            "papeleria"    -> "supermarket" to "Supermercados"
                            "floreria"     -> "marketplace" to "Mercados"
                            else           -> "shop" to "Tiendas"
                        }
                        val compResponse = RetrofitClient.getApi(requireContext()).getLugaresCercanos(
                            lat = currentAnalysisCenter.latitude,
                            lng = currentAnalysisCenter.longitude,
                            tipo = compTipo,
                            radio = radioMetros.toInt()
                        )
                        val compValue = if (compResponse.isSuccessful) {
                            compResponse.body()?.total?.firstOrNull() ?: compResponse.body()?.lugares?.size ?: 0
                        } else 0
                        val buses = resultado.osm.paradas_bus.firstOrNull() ?: 0

                        val trafico1Label = when (tipoSeleccionado.lowercase()) {
                            "restaurante" -> "Restaurantes"
                            "cafeteria" -> "Cafés"
                            "taller autos" -> "Gasolineras"
                            "farmacia" -> "Farmacias"
                            "papeleria" -> "Tiendas"
                            "floreria" -> "Mercados"
                            else -> "Negocios"
                        }

                        val trafico1Value = when (tipoSeleccionado.lowercase()) {
                            "cafeteria" -> cafes
                            else -> totalSeleccionado
                        }

                        val competenciaLabel = trafico1Label
                        val competenciaTotal = totalSeleccionado
                        val directa = resultado.osm.competencia_directa.firstOrNull() ?: 0
                        val scoreFinal = resultado.score_final.firstOrNull() ?: 0.0
                        val indiceSocio = resultado.inegi.indice_socio.firstOrNull() ?: 0.0
                        val baresTotal = resultado.osm.bares.firstOrNull() ?: 0
                        val fastFoodTotal = resultado.osm.fast_food.firstOrNull() ?: 0
                        val supermercadosTotal = resultado.osm.supermercados.firstOrNull() ?: 0
                        val bancosTotal = resultado.osm.bancos.firstOrNull() ?: 0
                        val restaurantesTotal = resultado.osm.restaurantes.firstOrNull() ?: 0
                        val busesTotal = resultado.osm.paradas_bus.firstOrNull() ?: 0

                        val marker = Marker(mapView).apply {
                            position = currentAnalysisCenter
                            title    = resultado.recomendacion[0]
                            snippet = buildString {
                                append("geobiz_card=zona\n")
                                append("negocio=$tipoSeleccionado\n")
                                append("score=${resultado.score_final.firstOrNull() ?: 0.0}\n")
                                append("trafico1_label=$trafico1Label\n")
                                append("trafico1_value=$trafico1Value\n")
                                append("trafico2_label=$compLabel\n")
                                append("trafico2_value=$compValue\n")
                                append("trafico3_label=Buses\n")
                                append("trafico3_value=$buses\n")
                                append("competencia_label=$competenciaLabel\n")
                                append("competencia_total=$competenciaTotal\n")
                                append("competencia_directa=$directa\n")
                                append("socio_indice=${resultado.inegi.indice_socio.firstOrNull() ?: 0.0}\n")
                            }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            infoWindow = CustomMarkerInfoWindow(mapView)
                        }
                        mapView.overlays.add(marker)
                        marker.showInfoWindow()
                        mapView.invalidate()

                        val prefs = requireContext().getSharedPreferences("geobiz_session", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("last_business_label", tipoSeleccionado)
                            .putFloat("last_score_final", scoreFinal.toFloat())
                            .putFloat("last_indice_socio", indiceSocio.toFloat())
                            .putInt("last_cafes", cafes)
                            .putInt("last_bares", baresTotal)
                            .putInt("last_fast_food", fastFoodTotal)
                            .putInt("last_supermercados", supermercadosTotal)
                            .putInt("last_bancos", bancosTotal)
                            .putInt("last_restaurantes", restaurantesTotal)
                            .putInt("last_competencia_total", competenciaTotal)
                            .putInt("last_competencia_directa", directa)
                            .putInt("last_buses", busesTotal)
                            .putFloat("last_lat", currentAnalysisCenter.latitude.toFloat())
                            .putFloat("last_lng", currentAnalysisCenter.longitude.toFloat())
                            .putFloat("last_zoom", mapView.zoomLevelDouble.toFloat())
                            .apply()

                        Toast.makeText(
                            requireContext(),
                            "${resultado.recomendacion[0]} — Score: ${String.format(Locale.getDefault(), "%.1f", scoreFinal)}/100",
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
