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

class AnalisisFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_analisis, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Valores de ejemplo por defecto (pueden venir por argumentos en el futuro)
        val xValues = doubleArrayOf(75.0, 65.0, 40.0, 80.0, 70.0, 60.0, 50.0)
        val businessType = BusinessType.RESTAURANTE

        val weights = ConfigLoader.loadBusinessWeights(requireContext())
        val engine = GeoBizEngine(weights)
        val result = engine.calculateScore(
            businessType = businessType,
            x1 = xValues[0],
            x2 = xValues[1],
            x3 = xValues[2],
            x4 = xValues[3],
            x5 = xValues[4],
            x6 = xValues[5],
            x7 = xValues[6]
        )

        setupToolbar(view)
        setupScoreSummary(view, result)
        setupStats(view, result)
        setupRadarChart(view, xValues)
        setupFoda(view, result)

        view.findViewById<MaterialButton?>(R.id.btn_simular_punto)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }
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

        val labels = listOf("Peatonal", "Vehicular", "Competencia", "NSE", "Seguridad", "Densidad", "Renta")

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

    private fun setupFoda(root: View, result: GeoBizResult) {
        val textFortalezas = root.findViewById<TextView>(R.id.textFortalezas)
        val textAmenazas = root.findViewById<TextView>(R.id.textAmenazas)
        val textOportunidades = root.findViewById<TextView>(R.id.textOportunidades)

        fun List<String>.toBulletList(): String =
            if (isEmpty()) "Sin elementos destacados para este cuadrante."
            else joinToString(separator = "\n") { "• $it" }

        textFortalezas.text = result.foda.strengths.toBulletList()
        textAmenazas.text = result.foda.threats.toBulletList()
        textOportunidades.text = result.foda.opportunities.toBulletList()
    }
}
