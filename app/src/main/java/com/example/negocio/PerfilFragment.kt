package com.example.negocio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PerfilFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_perfil, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editIp      = view.findViewById<TextInputEditText>(R.id.edit_server_ip)
        val btnGuardar  = view.findViewById<MaterialButton>(R.id.btn_guardar_ip)
        val textActual  = view.findViewById<TextView>(R.id.text_ip_actual)

        val prefs = requireContext().getSharedPreferences("geobiz_config", android.content.Context.MODE_PRIVATE)

        // Mostrar IP actual guardada
        val ipActual = prefs.getString("server_ip", "192.168.100.17") ?: "192.168.100.17"
        textActual.text = "IP actual: $ipActual"
        editIp.setText(ipActual)

        btnGuardar.setOnClickListener {
            val nuevaIp = editIp.text.toString().trim()

            if (nuevaIp.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa una IP válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar en SharedPreferences
            prefs.edit().putString("server_ip", nuevaIp).apply()
            textActual.text = "IP actual: $nuevaIp"

            Toast.makeText(requireContext(), "IP guardada: $nuevaIp", Toast.LENGTH_SHORT).show()
        }
    }
}