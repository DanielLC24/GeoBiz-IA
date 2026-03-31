package com.example.negocio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import java.util.Locale
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AnalisisFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_analisis, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("geobiz_session", android.content.Context.MODE_PRIVATE)
        val lastBusiness = prefs.getString("last_business_label", null)
        val lastScore = prefs.getFloat("last_score_final", -1f).toDouble()

        var metricCompetencia = 40.0
        var metricNse = 80.0
        var metricSeguridad = 70.0
        var metricDensidad = 60.0
        var metricRenta = 50.0
        var competenciaDirecta = 0
        var competenciaTotal = 0

        val result = if (!lastBusiness.isNullOrBlank() && lastScore >= 0.0) {
            val cafes = prefs.getInt("last_cafes", 0)
            val bares = prefs.getInt("last_bares", 0)
            val fastFood = prefs.getInt("last_fast_food", 0)
            val supermercados = prefs.getInt("last_supermercados", 0)
            val bancos = prefs.getInt("last_bancos", 0)
            val restaurantes = prefs.getInt("last_restaurantes", 0)
            competenciaTotal = prefs.getInt("last_competencia_total", 0)
            competenciaDirecta = prefs.getInt("last_competencia_directa", 0)
            val buses = prefs.getInt("last_buses", 0)
            val indiceSocio = prefs.getFloat("last_indice_socio", 0f).toDouble()

            fun clamp(v: Double) = v.coerceIn(0.0, 100.0)
            fun scaled(count: Int, factor: Double) = clamp(count.toDouble() * factor)

            val peatonal = clamp(
                scaled(cafes, 4.0) + scaled(fastFood, 3.0) + scaled(bares, 2.0) + scaled(buses, 4.0)
            )
            val vehicular = clamp(
                scaled(supermercados, 6.0) + scaled(bancos, 5.0) + scaled(restaurantes, 2.0) + scaled(buses, 2.0)
            )

            val competenciaScore = clamp(100.0 - (competenciaTotal.toDouble() / 60.0) * 100.0)
            val nseScore = clamp(indiceSocio)

            val densidad = clamp(
                scaled(cafes, 2.0) +
                        scaled(fastFood, 2.0) +
                        scaled(restaurantes, 2.0) +
                        scaled(supermercados, 3.0) +
                        scaled(bancos, 2.0) +
                        scaled(buses, 3.0)
            )

            val seguridad = clamp(
                55.0 +
                        (nseScore - 50.0) * 0.5 -
                        scaled(bares, 2.0) -
                        scaled(competenciaDirecta, 1.5) +
                        scaled(bancos, 0.8)
            )

            val renta = clamp(
                100.0 - ((nseScore * 0.6 + densidad * 0.4) - 50.0)
            )

            metricCompetencia = competenciaScore
            metricNse = nseScore
            metricSeguridad = seguridad
            metricDensidad = densidad
            metricRenta = renta

            val color = when {
                lastScore > 75.0 -> RecommendationColor.VERDE_EXITO
                lastScore >= 50.0 -> RecommendationColor.AMARILLO_RIESGO_MEDIO
                else -> RecommendationColor.ROJO_NO_RECOMENDADO
            }

            val strengths = buildList {
                if (competenciaScore > 70) add("Baja competencia en la zona")
                if (nseScore > 70) add("NSE alto")
                if (seguridad > 70) add("Zona relativamente segura")
                if (densidad > 70) add("Alta densidad de actividad")
                if (renta > 70) add("Renta relativamente accesible")
            }
            val threats = buildList {
                if (competenciaScore < 40) add("Competencia elevada para el giro seleccionado")
                if (seguridad < 40) add("Riesgo percibido por seguridad")
                if (renta < 40) add("Renta potencialmente alta")
                if (densidad < 35) add("Baja densidad de actividad en el entorno")
            }
            val opportunities = buildList {
                if (nseScore > 70 && competenciaScore > 60) add("Mercado con poder adquisitivo y baja competencia relativa")
                if (densidad > 65 && renta > 55) add("Entorno activo con costos potencialmente manejables")
            }

            val xValuesAll = doubleArrayOf(
                peatonal,
                vehicular,
                competenciaScore,
                nseScore,
                seguridad,
                densidad,
                renta
            )

            view.setTag(R.id.radarChartGeoBiz, xValuesAll)

            GeoBizResult(
                normalizedScore = lastScore.coerceIn(0.0, 100.0),
                recommendationColor = color,
                foda = FodaAnalysis(
                    strengths = strengths,
                    threats = threats,
                    opportunities = opportunities
                )
            )
        } else {
            val xValues = doubleArrayOf(75.0, 65.0, 40.0, 80.0, 70.0, 60.0, 50.0)
            val weights = ConfigLoader.loadBusinessWeights(requireContext())
            val engine = GeoBizEngine(weights)
            engine.calculateScore(
                businessType = BusinessType.RESTAURANTE,
                x1 = xValues[0],
                x2 = xValues[1],
                x3 = xValues[2],
                x4 = xValues[3],
                x5 = xValues[4],
                x6 = xValues[5],
                x7 = xValues[6]
            ).also {
                view.setTag(R.id.radarChartGeoBiz, xValues)
                metricCompetencia = xValues[2]
                metricNse = xValues[3]
                metricSeguridad = xValues[4]
                metricDensidad = xValues[5]
                metricRenta = xValues[6]
            }
        }

        setupToolbar(view)
        setupScoreSummary(view, result)
        setupStats(view, result)
        val all = (view.getTag(R.id.radarChartGeoBiz) as? DoubleArray) ?: doubleArrayOf(75.0, 65.0, 40.0, 80.0, 70.0, 60.0, 50.0)
        val marketValues = doubleArrayOf(all[2], all[3], all[4], all[5], all[6])
        setupRadarChart(view, marketValues)
        setupFoda(view, result, metricCompetencia, metricNse, metricSeguridad, metricDensidad, metricRenta, competenciaTotal, competenciaDirecta)
        setupMapPreview(view)

        view.findViewById<MaterialButton?>(R.id.btn_simular_punto)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }
        view.findViewById<View?>(R.id.mapPreviewClick)?.setOnClickListener {
            prefs.edit().putBoolean("restore_map_pending", true).apply()
            (activity as? MainActivity)?.navigateToTab(3)
        }
    }

    private fun setupMapPreview(root: View) {
        val prefs = requireContext().getSharedPreferences("geobiz_session", android.content.Context.MODE_PRIVATE)
        if (!prefs.contains("last_lat") || !prefs.contains("last_lng")) return
        val lat = prefs.getFloat("last_lat", 0f).toDouble()
        val lng = prefs.getFloat("last_lng", 0f).toDouble()

        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = requireContext().packageName

        val mapView = root.findViewById<MapView?>(R.id.mapPreview) ?: return
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(false)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(GeoPoint(lat, lng))

        mapView.overlays.clear()
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_person_pin)
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun setupToolbar(root: View) {
        root.findViewById<ImageView?>(R.id.btnBack)?.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        root.findViewById<ImageView?>(R.id.btnShare)?.setOnClickListener {
            // Espacio para compartir en futuras versiones
        }
    }

    private fun setupScoreSummary(root: View, result: GeoBizResult) {
        val textScoreInterpretacion = root.findViewById<TextView>(R.id.textScoreInterpretacion)
        val progressScore = root.findViewById<ProgressBar>(R.id.progressScore)
        val textScoreValor = root.findViewById<TextView>(R.id.textScoreValor)

        val scoreRounded = result.normalizedScore.coerceIn(0.0, 100.0)
        progressScore?.progress = scoreRounded.toInt()
        textScoreValor?.text = scoreRounded.toInt().toString()

        val (mensaje, colorRes) = when (result.recommendationColor) {
            RecommendationColor.VERDE_EXITO ->
                "Alta probabilidad de éxito en esta ubicación." to R.color.risk_low

            RecommendationColor.AMARILLO_RIESGO_MEDIO ->
                "Viabilidad intermedia: revisa con detalle costos y competencia." to R.color.risk_medium

            RecommendationColor.ROJO_NO_RECOMENDADO ->
                "No recomendado: el punto presenta alto riesgo para este giro." to R.color.risk_high
        }

        textScoreInterpretacion.text = mensaje
        textScoreInterpretacion.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun setupStats(root: View, result: GeoBizResult) {
        val textViabilidadNivel = root.findViewById<TextView>(R.id.textViabilidadNivel)
        val textViabilidadPorcentaje = root.findViewById<TextView>(R.id.textViabilidadPorcentaje)
        val dotViabilidad = root.findViewById<View>(R.id.dotViabilidad)
        val textRoiValor = root.findViewById<TextView>(R.id.textRoiValor)
        val textRoiPercentil = root.findViewById<TextView>(R.id.textRoiPercentil)

        when (result.recommendationColor) {
            RecommendationColor.VERDE_EXITO -> {
                textViabilidadNivel?.text = "Alta"
                dotViabilidad?.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.risk_low)
                textViabilidadPorcentaje?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.risk_low)
                )
            }
            RecommendationColor.AMARILLO_RIESGO_MEDIO -> {
                textViabilidadNivel?.text = "Media"
                dotViabilidad?.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.risk_medium)
                textViabilidadPorcentaje?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.risk_medium)
                )
            }
            RecommendationColor.ROJO_NO_RECOMENDADO -> {
                textViabilidadNivel?.text = "Baja"
                dotViabilidad?.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.risk_high)
                textViabilidadPorcentaje?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.risk_high)
                )
            }
        }

        val growthPct = 12.4
        textViabilidadPorcentaje?.text = String.format("%+.1f%%", growthPct)

        val roiMonths = 14
        textRoiValor?.text = "$roiMonths meses"
        textRoiPercentil?.text = "Percentil 90 local"
    }

    private fun setupRadarChart(root: View, xValues: DoubleArray) {
        val radarChart = root.findViewById<RadarChart>(R.id.radarChartGeoBiz)

        val entries = xValues.map { RadarEntry(it.toFloat()) }

        val dataSet = RadarDataSet(entries, "Perfil del punto").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_geobiz)
            fillColor = ContextCompat.getColor(requireContext(), R.color.primary_geobiz)
            setDrawFilled(true)
            fillAlpha = 120
            lineWidth = 2f
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.on_surface_geobiz)
        }

        radarChart.data = RadarData(dataSet)
        radarChart.description.isEnabled = false
        radarChart.legend.isEnabled = false

        val labels = listOf("Competencia", "NSE", "Seguridad", "Densidad", "Renta")

        radarChart.xAxis.apply {
            textSize = 11f
            textColor = ContextCompat.getColor(requireContext(), R.color.on_surface_geobiz)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt() % labels.size
                    return labels[index]
                }
            }
            position = XAxis.XAxisPosition.BOTTOM
        }

        radarChart.yAxis.apply {
            axisMinimum = 0f
            axisMaximum = 100f
            labelCount = 5
            textSize = 9f
            textColor = ContextCompat.getColor(requireContext(), R.color.on_surface_geobiz)
            setDrawLabels(false)
            axisLineColor = ContextCompat.getColor(requireContext(), R.color.surface_variant_geobiz)
            gridColor = ContextCompat.getColor(requireContext(), R.color.surface_variant_geobiz)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        }

        radarChart.invalidate()
    }

    private fun setupFoda(
        root: View,
        result: GeoBizResult,
        competencia: Double,
        nse: Double,
        seguridad: Double,
        densidad: Double,
        renta: Double,
        competenciaTotal: Int,
        competenciaDirecta: Int
    ) {
        val textFortalezas = root.findViewById<TextView>(R.id.textFortalezas)
        val textAmenazas = root.findViewById<TextView>(R.id.textAmenazas)
        val textOportunidades = root.findViewById<TextView>(R.id.textOportunidades)
        val textDebilidades = root.findViewById<TextView>(R.id.textDebilidades)

        fun List<String>.toBulletList(): String =
            if (isEmpty()) "• Sin elementos destacados para este cuadrante."
            else joinToString(separator = "\n") { "• $it" }

        val fortalezas = buildList {
            if (competencia > 70) add("Competencia baja (${competencia.toInt()}/100)")
            if (nse > 70) add("NSE alto (${String.format(Locale.getDefault(), "%.1f", nse)})")
            if (seguridad > 70) add("Buena percepción de seguridad (${seguridad.toInt()}/100)")
            if (densidad > 70) add("Alta densidad de actividad (${densidad.toInt()}/100)")
            if (renta > 70) add("Renta potencialmente accesible (${renta.toInt()}/100)")
            add("Score GeoBiz: ${result.normalizedScore.toInt()}/100")
        }

        val oportunidades = buildList {
            if (nse > 65 && competencia > 60) add("Buen mercado objetivo con presión competitiva controlada")
            if (densidad > 60 && seguridad > 55) add("Entorno con flujo y condiciones operativas aceptables")
            if (renta > 60 && densidad > 60) add("Potencial para crecer sin una renta excesiva")
            add("Aprovecha horarios pico y promociones locales para capturar demanda temprana")
        }

        val debilidades = buildList {
            if (competencia < 55) add("Competencia alta en el entorno (${competenciaTotal} negocios)")
            if (competenciaDirecta > 10) add("Competencia directa relevante (${competenciaDirecta} directa)")
            if (seguridad < 55) add("Seguridad por debajo de lo ideal (${seguridad.toInt()}/100)")
            if (densidad < 50) add("Densidad moderada/baja (${densidad.toInt()}/100)")
            if (renta < 45) add("Renta potencialmente alta (${renta.toInt()}/100)")
            add("Optimiza costos fijos y define una propuesta clara para diferenciarte")
        }

        val amenazas = buildList {
            if (result.normalizedScore < 50) add("Score bajo: riesgo alto si no se ajusta el modelo operativo")
            if (competencia < 45) add("Presión competitiva elevada: guerra de precios o saturación")
            if (seguridad < 45) add("Riesgo por seguridad: posibles caídas en afluencia nocturna")
            if (renta < 40) add("Costo de renta podría afectar margen y punto de equilibrio")
            if (isEmpty()) add("No se observan amenazas críticas con estas métricas; monitorea cambios de competencia y renta")
        }

        textFortalezas.text = fortalezas.toBulletList()
        textOportunidades.text = oportunidades.toBulletList()
        textDebilidades.text = debilidades.toBulletList()
        textAmenazas.text = amenazas.toBulletList()
    }
}
