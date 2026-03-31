package com.example.negocio

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * InfoWindow personalizado para mostrar título y snippet de los marcadores
 * con tamaño de letra legible (16sp / 14sp).
 */
class CustomMarkerInfoWindow(mapView: MapView) : InfoWindow(R.layout.infowindow_marker, mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        val snippet = marker.snippet.orEmpty()
        val isZona = snippet.contains("geobiz_card=zona", ignoreCase = true)

        val containerSimple = mView.findViewById<View>(R.id.container_simple)
        val containerZona = mView.findViewById<View>(R.id.container_zone)

        containerSimple.visibility = if (isZona) View.GONE else View.VISIBLE
        containerZona.visibility = if (isZona) View.VISIBLE else View.GONE

        if (!isZona) {
            mView.findViewById<TextView>(R.id.infowindow_title).text = marker.title.orEmpty()
            mView.findViewById<TextView>(R.id.infowindow_snippet).text = snippet
            return
        }

        fun findValue(key: String): String? {
            val regex = Regex("""(?m)^\s*${Regex.escape(key)}=(.*)\s*$""")
            return regex.find(snippet)?.groupValues?.getOrNull(1)?.trim()
        }

        fun findInt(key: String): Int = findValue(key)?.toIntOrNull() ?: 0
        fun findDouble(key: String): Double = findValue(key)?.toDoubleOrNull() ?: 0.0

        val negocio = findValue("negocio").orEmpty()
        val score = findDouble("score")
        val trafico1Label = findValue("trafico1_label").orEmpty()
        val trafico1Value = findInt("trafico1_value")
        val trafico2Value = findInt("trafico2_value")
        val trafico3Value = findInt("trafico3_value")
        val competenciaLabel = findValue("competencia_label").orEmpty()
        val competenciaTotal = findInt("competencia_total")
        val directa = findInt("competencia_directa")
        val indice = findDouble("socio_indice")

        val titulo = marker.title.orEmpty().ifBlank { "Zona recomendada" }
        mView.findViewById<TextView>(R.id.textTituloZona).text = titulo.replaceFirstChar { it.uppercase() }

        val progress = mView.findViewById<ProgressBar>(R.id.progressZonaScore)
        progress.max = 100
        progress.progress = score.coerceIn(0.0, 100.0).toInt()
        mView.findViewById<TextView>(R.id.textZonaScore).text = String.format("%.1f", score)

        val iconTraffic1 = mView.findViewById<android.widget.ImageView>(R.id.iconTraffic1)
        val iconRes = when (negocio.lowercase()) {
            "restaurante" -> R.drawable.ic_restaurant
            "cafeteria" -> R.drawable.ic_cafe
            "taller autos" -> R.drawable.ic_car_repair
            "farmacia" -> R.drawable.ic_pharmacy
            "papeleria" -> R.drawable.ic_stationery
            "floreria" -> R.drawable.ic_flower
            else -> R.drawable.ic_custom_business
        }
        iconTraffic1.setImageResource(iconRes)

        mView.findViewById<TextView>(R.id.textTraffic1).text = "$trafico1Value $trafico1Label"
        mView.findViewById<TextView>(R.id.textTraffic2).text = "$trafico2Value ${findValue("trafico2_label") ?: ""}"
        val iconTraffic2 = mView.findViewById<android.widget.ImageView>(R.id.iconTraffic2)
        val iconCompRes = when (negocio.lowercase()) {
            "restaurante"  -> R.drawable.ic_martini
            "cafeteria"    -> R.drawable.ic_restaurant
            "taller autos" -> R.drawable.ic_car_repair
            "farmacia"     -> R.drawable.ic_group
            "papeleria"    -> R.drawable.ic_group
            "floreria"     -> R.drawable.ic_group
            else           -> R.drawable.ic_group
        }
        iconTraffic2.setImageResource(iconCompRes)
        mView.findViewById<TextView>(R.id.textTraffic3).text = if (trafico3Value == 1) "1 Bus" else "$trafico3Value Buses"

        mView.findViewById<TextView>(R.id.textCompetenciaTotal).text = competenciaTotal.toString()
        mView.findViewById<TextView>(R.id.textCompetenciaLabel).text = competenciaLabel.uppercase()
        mView.findViewById<TextView>(R.id.textCompetenciaDirecta).text = "• $directa Directa"

        mView.findViewById<TextView>(R.id.textIndiceSocio).text = String.format("%.1f", indice)
        val nivel = when {
            indice >= 70.0 -> "NIVEL A/B"
            indice >= 55.0 -> "NIVEL C+"
            else -> "NIVEL C"
        }
        mView.findViewById<TextView>(R.id.textNivelSocio).text = nivel

        mView.findViewById<MaterialButton>(R.id.btnReporteDetallado).setOnClickListener {
            mapView.context.getSharedPreferences("geobiz_session", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("restore_map_pending", true)
                .apply()
            (mapView.context as? MainActivity)?.navigateToTab(0)
            close()
        }
    }

    override fun onClose() {
        // Nada que limpiar
    }
}
