package com.example.negocio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class InicioFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_inicio, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.let { main ->
            view.findViewById<View>(R.id.card_mapa).setOnClickListener { main.navigateToTab(3) }
            view.findViewById<View>(R.id.card_analisis).setOnClickListener { main.navigateToTab(0) }
            view.findViewById<View>(R.id.card_simulador).setOnClickListener { main.navigateToTab(1) }
        }
    }
}
