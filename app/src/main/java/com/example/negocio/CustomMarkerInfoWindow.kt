package com.example.negocio

import android.view.View
import android.widget.TextView
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
        mView.findViewById<TextView>(R.id.infowindow_title).apply {
            text = marker.title ?: ""
        }
        mView.findViewById<TextView>(R.id.infowindow_snippet).apply {
            text = marker.snippet ?: ""
            visibility = if (marker.snippet.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onClose() {
        // Nada que limpiar
    }
}
