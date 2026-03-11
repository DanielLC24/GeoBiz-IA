package com.example.negocio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class SimuladorFragment : Fragment() {

    private enum class Categoria {
        RESTAURANTE, CAFETERIA, TALLER, FARMACIA, PAPELERIA, FLORERIA, PERSONALIZADO
    }

    private var categoriaSeleccionada: Categoria? = null
    private var gastoMarketingMensual: Double = 0.0
    private var costoEmpleadosMensual: Double = 0.0
    private var diasOperacionMes: Int = 30

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_simulador, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardInputs = view.findViewById<MaterialCardView>(R.id.card_inputs)
        val btnCalcular = view.findViewById<MaterialButton>(R.id.btnCalcularRoi)
        val btnAjustesAvanzados = view.findViewById<MaterialButton>(R.id.btnAjustesAvanzados)

        val editRenta = view.findViewById<TextInputEditText>(R.id.editRentaMensual)
        val editInversion = view.findViewById<TextInputEditText>(R.id.editInversionInicial)
        val editTicket = view.findViewById<TextInputEditText>(R.id.editTicketPromedio)
        val editMargen = view.findViewById<TextInputEditText>(R.id.editMargenPromedio)
        val editVentas = view.findViewById<TextInputEditText>(R.id.editVentasEstimadas)

        val textRoi = view.findViewById<android.widget.TextView>(R.id.textRoiPorcentaje)
        val textRoiDetalle = view.findViewById<android.widget.TextView>(R.id.textRoiDetalle)
        val textComparativa = view.findViewById<android.widget.TextView>(R.id.textComparativaMercado)
        val viewRiesgo = view.findViewById<View>(R.id.viewRiesgo)
        val textIngresosMensuales = view.findViewById<android.widget.TextView>(R.id.textIngresosMensuales)
        val textPuntoEquilibrio = view.findViewById<android.widget.TextView>(R.id.textPuntoEquilibrio)

        // Grid de categorías
        val cards = mapOf(
            Categoria.RESTAURANTE to view.findViewById<MaterialCardView>(R.id.card_restaurante),
            Categoria.CAFETERIA to view.findViewById<MaterialCardView>(R.id.card_cafeteria),
            Categoria.TALLER to view.findViewById<MaterialCardView>(R.id.card_taller),
            Categoria.FARMACIA to view.findViewById<MaterialCardView>(R.id.card_farmacia),
            Categoria.PAPELERIA to view.findViewById<MaterialCardView>(R.id.card_papeleria),
            Categoria.FLORERIA to view.findViewById<MaterialCardView>(R.id.card_floreria),
            Categoria.PERSONALIZADO to view.findViewById<MaterialCardView>(R.id.card_personalizado)
        )

        cards.forEach { (categoria, card) ->
            card.setOnClickListener {
                seleccionarCategoria(categoria, cards.values.toList(), cardInputs, btnAjustesAvanzados)
                aplicarPresetParaCategoria(
                    categoria,
                    editRenta,
                    editInversion,
                    editTicket,
                    editMargen,
                    editVentas
                )
            }
        }

        btnAjustesAvanzados.setOnClickListener {
            mostrarDialogoAjustesAvanzados()
        }

        btnCalcular.setOnClickListener {
            val categoria = categoriaSeleccionada
            if (categoria == null) {
                Toast.makeText(requireContext(), "Selecciona un giro para comenzar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val renta = editRenta.text.toString().replace(",", "").toDoubleOrNull()
            val inversion = editInversion.text.toString().replace(",", "").toDoubleOrNull()
            val ticket = editTicket.text.toString().replace(",", "").toDoubleOrNull()
            val margen = editMargen.text.toString().replace(",", "").toDoubleOrNull()
            val ventas = editVentas.text.toString().replace(",", "").toDoubleOrNull()

            if (renta == null || inversion == null || ticket == null || margen == null || ventas == null) {
                Toast.makeText(requireContext(), "Completa todos los parámetros numéricos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            calcularYMostrarRoi(
                rentaMensual = renta,
                inversionInicial = inversion,
                ticketPromedio = ticket,
                margenPorcentaje = margen,
                ventasEstimadas = ventas,
                textRoi = textRoi,
                textRoiDetalle = textRoiDetalle,
                textComparativa = textComparativa,
                viewRiesgo = viewRiesgo,
                textIngresosMensuales = textIngresosMensuales,
                textPuntoEquilibrio = textPuntoEquilibrio
            )
        }
    }

    private fun seleccionarCategoria(
        categoria: Categoria,
        todasLasCards: List<MaterialCardView>,
        cardInputs: MaterialCardView,
        btnAjustesAvanzados: MaterialButton
    ) {
        categoriaSeleccionada = categoria
        val primary = ContextCompat.getColor(requireContext(), R.color.primary_geobiz)
        val surface = ContextCompat.getColor(requireContext(), R.color.surface_geobiz)
        val surfaceVariant = ContextCompat.getColor(requireContext(), R.color.surface_variant_geobiz)

        todasLasCards.forEach { card ->
            val seleccionada = when (card.id) {
                R.id.card_restaurante -> categoria == Categoria.RESTAURANTE
                R.id.card_cafeteria -> categoria == Categoria.CAFETERIA
                R.id.card_taller -> categoria == Categoria.TALLER
                R.id.card_farmacia -> categoria == Categoria.FARMACIA
                R.id.card_papeleria -> categoria == Categoria.PAPELERIA
                R.id.card_floreria -> categoria == Categoria.FLORERIA
                R.id.card_personalizado -> categoria == Categoria.PERSONALIZADO
                else -> false
            }

            card.strokeWidth = if (seleccionada) (2 * resources.displayMetrics.density).toInt() else 0
            card.setCardBackgroundColor(if (seleccionada) surfaceVariant else surface)
            card.strokeColor = if (seleccionada) primary else 0
        }

        cardInputs.alpha = 1f
        cardInputs.isEnabled = true
        setEnabledRecursive(cardInputs, true)

        if (categoria == Categoria.PERSONALIZADO) {
            btnAjustesAvanzados.visibility = View.VISIBLE
        } else {
            btnAjustesAvanzados.visibility = View.GONE
            gastoMarketingMensual = 0.0
            costoEmpleadosMensual = 0.0
            diasOperacionMes = 30
        }
    }

    private fun aplicarPresetParaCategoria(
        categoria: Categoria,
        editRenta: TextInputEditText,
        editInversion: TextInputEditText,
        editTicket: TextInputEditText,
        editMargen: TextInputEditText,
        editVentas: TextInputEditText
    ) {
        when (categoria) {
            Categoria.RESTAURANTE -> {
                editRenta.setText("18000")
                editInversion.setText("280000")
                editTicket.setText("150")
                editMargen.setText("40")
                editVentas.setText("900")
            }
            Categoria.CAFETERIA -> {
                editRenta.setText("14000")
                editInversion.setText("220000")
                editTicket.setText("90")
                editMargen.setText("45")
                editVentas.setText("750")
            }
            Categoria.TALLER -> {
                editRenta.setText("16000")
                editInversion.setText("260000")
                editTicket.setText("350")
                editMargen.setText("35")
                editVentas.setText("320")
            }
            Categoria.FARMACIA -> {
                editRenta.setText("20000")
                editInversion.setText("300000")
                editTicket.setText("180")
                editMargen.setText("30")
                editVentas.setText("800")
            }
            Categoria.PAPELERIA -> {
                editRenta.setText("12000")
                editInversion.setText("180000")
                editTicket.setText("70")
                editMargen.setText("32")
                editVentas.setText("650")
            }
            Categoria.FLORERIA -> {
                editRenta.setText("10000")
                editInversion.setText("160000")
                editTicket.setText("110")
                editMargen.setText("38")
                editVentas.setText("500")
            }
            Categoria.PERSONALIZADO -> {
                editRenta.text = null
                editInversion.text = null
                editTicket.text = null
                editMargen.text = null
                editVentas.text = null
            }
        }
    }

    private fun mostrarDialogoAjustesAvanzados() {
        val ctx = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_ajustes_avanzados, null)
        val editDias = dialogView.findViewById<TextInputEditText>(R.id.editDiasOperacion)
        val editMarketing = dialogView.findViewById<TextInputEditText>(R.id.editGastoMarketing)
        val editEmpleados = dialogView.findViewById<TextInputEditText>(R.id.editCostoEmpleados)

        editDias.setText(diasOperacionMes.toString())
        if (gastoMarketingMensual > 0) {
            editMarketing.setText(gastoMarketingMensual.toString())
        }
        if (costoEmpleadosMensual > 0) {
            editEmpleados.setText(costoEmpleadosMensual.toString())
        }

        MaterialAlertDialogBuilder(ctx)
            .setView(dialogView)
            .setPositiveButton("Aplicar") { _, _ ->
                diasOperacionMes = editDias.text?.toString()?.toIntOrNull() ?: diasOperacionMes
                gastoMarketingMensual =
                    editMarketing.text?.toString()?.replace(",", "")?.toDoubleOrNull()
                        ?: gastoMarketingMensual
                costoEmpleadosMensual =
                    editEmpleados.text?.toString()?.replace(",", "")?.toDoubleOrNull()
                        ?: costoEmpleadosMensual
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun calcularYMostrarRoi(
        rentaMensual: Double,
        inversionInicial: Double,
        ticketPromedio: Double,
        margenPorcentaje: Double,
        ventasEstimadas: Double,
        textRoi: android.widget.TextView,
        textRoiDetalle: android.widget.TextView,
        textComparativa: android.widget.TextView,
        viewRiesgo: View,
        textIngresosMensuales: android.widget.TextView,
        textPuntoEquilibrio: android.widget.TextView
    ) {
        val margenDecimal = margenPorcentaje / 100.0
        val ingresosBrutosMensuales = ticketPromedio * ventasEstimadas
        val utilidadBrutaMensual = ingresosBrutosMensuales * margenDecimal
        val otrosCostosMensuales = gastoMarketingMensual + costoEmpleadosMensual
        val utilidadNetaMensual = utilidadBrutaMensual - rentaMensual - otrosCostosMensuales

        // Ingresos mensuales (dashboard)
        textIngresosMensuales.text = String.format(
            Locale.getDefault(),
            "Ingresos mensuales estimados: $%,.0f MXN",
            ingresosBrutosMensuales
        )

        if (utilidadNetaMensual <= 0.0 || inversionInicial <= 0.0) {
            textRoi.text = "ROI estimado —"
            textRoiDetalle.text =
                "Con los parámetros actuales, la inversión no se recupera (utilidad neta negativa)."
            viewRiesgo.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.risk_high)
            )

            textPuntoEquilibrio.text = "Punto de equilibrio: no se alcanza con los parámetros actuales."
        } else {
            val mesesParaRecuperar = inversionInicial / utilidadNetaMensual
            val roiAnual = (utilidadNetaMensual * 12.0 / inversionInicial) * 100.0

            val roiStr = String.format(Locale.getDefault(), "%.1f%%", roiAnual)
            val mesesStr = String.format(Locale.getDefault(), "%.1f", mesesParaRecuperar)
            textRoi.text = "ROI estimado $roiStr"
            textRoiDetalle.text = "Retorno aproximado en $mesesStr meses."

            val colorRiesgo = when {
                mesesParaRecuperar > 36 || roiAnual < 5 -> R.color.risk_high
                mesesParaRecuperar > 18 || roiAnual < 15 -> R.color.risk_medium
                else -> R.color.risk_low
            }
            viewRiesgo.setBackgroundColor(
                ContextCompat.getColor(requireContext(), colorRiesgo)
            )

            // Punto de equilibrio (clientes/mes)
            val costoVariableUnitario = ticketPromedio * (1.0 - margenDecimal)
            val costosFijosMensuales = rentaMensual + otrosCostosMensuales
            val margenUnitario = ticketPromedio - costoVariableUnitario

            if (margenUnitario > 0.0) {
                val puntoEquilibrioClientes = costosFijosMensuales / margenUnitario
                val peStr = String.format(Locale.getDefault(), "%,.0f", puntoEquilibrioClientes)
                textPuntoEquilibrio.text =
                    "Punto de equilibrio: $peStr clientes/mes (aprox.)"
            } else {
                textPuntoEquilibrio.text =
                    "Punto de equilibrio: no se puede calcular (margen unitario nulo o negativo)."
            }
        }

        // Comparativa simple con promedio de renta de la zona BUAP (valor de referencia)
        val rentaPromedioZona = 15000.0
        val diferencia = rentaMensual - rentaPromedioZona
        val porcentaje = if (rentaPromedioZona != 0.0) {
            (diferencia / rentaPromedioZona) * 100.0
        } else 0.0

        val diferenciaAbs = kotlin.math.abs(porcentaje)
        val porcentajeTexto = String.format(Locale.getDefault(), "%.1f", diferenciaAbs)

        textComparativa.text = when {
            diferencia > 0 ->
                "La renta que ingresaste está $porcentajeTexto% por encima del promedio estimado de la zona BUAP."
            diferencia < 0 ->
                "La renta que ingresaste está $porcentajeTexto% por debajo del promedio estimado de la zona BUAP."
            else ->
                "La renta que ingresaste coincide con el promedio estimado de la zona BUAP."
        }
    }

    private fun setEnabledRecursive(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setEnabledRecursive(view.getChildAt(i), enabled)
            }
        }
    }
}

