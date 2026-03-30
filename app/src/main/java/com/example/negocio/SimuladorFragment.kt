package com.example.negocio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ImageView
import android.animation.ObjectAnimator
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

        // Toolbar acciones
        view.findViewById<ImageView?>(R.id.btnBackSim)?.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Sliders y valores
        val seekRenta = view.findViewById<SeekBar>(R.id.seekRenta)
        val seekInversion = view.findViewById<SeekBar>(R.id.seekInversion)
        val seekTicket = view.findViewById<SeekBar>(R.id.seekTicket)
        val seekVentas = view.findViewById<SeekBar>(R.id.seekVentas)

        val textRentaValor = view.findViewById<TextView>(R.id.textRentaValor)
        val textInversionValor = view.findViewById<TextView>(R.id.textInversionValor)
        val textTicketValor = view.findViewById<TextView>(R.id.textTicketValor)
        val textVentasValor = view.findViewById<TextView>(R.id.textVentasValor)

        // ROI highlight
        val textRoiBig = view.findViewById<TextView>(R.id.textRoiBig)
        val textRoiDelta = view.findViewById<TextView>(R.id.textRoiDelta)
        val textMesesRetorno = view.findViewById<TextView>(R.id.textMesesRetorno)
        val textNivelRiesgo = view.findViewById<TextView>(R.id.textNivelRiesgo)
        val dotRiesgo = view.findViewById<View>(R.id.dotRiesgo)
        val timelineLine = view.findViewById<View>(R.id.timelineLine)
        val dotEquilibrio = view.findViewById<View>(R.id.dotEquilibrio)
        val pillEquilibrio = view.findViewById<TextView>(R.id.textPuntoEquilibrioPill)
        val labelMesEquilibrio = view.findViewById<TextView>(R.id.textMesEquilibrioNumber)

        fun showNumberInput(title: String, hint: String, initial: Double?, onOk: (Double) -> Unit) {
            val input = TextInputEditText(requireContext()).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(initial?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "")
                hint?.let { setHint(it) }
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Aceptar") { _, _ ->
                    val v = input.text?.toString()?.replace(",", "")?.replace("$", "")?.trim()?.toDoubleOrNull()
                    if (v != null) onOk(v) else Toast.makeText(requireContext(), "Valor no válido", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        fun rentaFrom(progress: Int) = 2500.0 + progress * 1000.0
        fun inversionFrom(progress: Int) = 50000.0 + progress * 10000.0
        fun ticketFrom(progress: Int) = 50.0 + progress * 20.0
        fun ventasFrom(progress: Int) = 50.0 + progress * 10.0

        fun actualizarLabels() {
            textRentaValor.text = String.format(Locale.getDefault(), "$%,.0f", rentaFrom(seekRenta.progress))
            textInversionValor.text = String.format(Locale.getDefault(), "$%,.0f", inversionFrom(seekInversion.progress))
            textTicketValor.text = String.format(Locale.getDefault(), "$%,.0f", ticketFrom(seekTicket.progress))
            textVentasValor.text = String.format(Locale.getDefault(), "%,.0f/mes", ventasFrom(seekVentas.progress))
        }

        fun posicionarPuntoEquilibrio(meses: Double) {
            val mesesClamped = meses.coerceIn(1.0, 24.0)
            timelineLine.post {
                val lineLeft = timelineLine.left.toFloat()
                val lineWidth = timelineLine.width.toFloat().coerceAtLeast(1f)
                val ratio = ((mesesClamped - 1.0) / 23.0).toFloat()
                val xPos = lineLeft + ratio * lineWidth - dotEquilibrio.width / 2f
                ObjectAnimator.ofFloat(dotEquilibrio, View.TRANSLATION_X, dotEquilibrio.translationX, xPos).apply {
                    duration = 220
                    start()
                }
                pillEquilibrio.post {
                    ObjectAnimator.ofFloat(pillEquilibrio, View.TRANSLATION_X, pillEquilibrio.translationX, xPos - (pillEquilibrio.width / 2f) + (dotEquilibrio.width / 2f)).apply {
                        duration = 220
                        start()
                    }
                }
                labelMesEquilibrio.post {
                    labelMesEquilibrio.text = String.format(Locale.getDefault(), "Mes %.0f", mesesClamped)
                    ObjectAnimator.ofFloat(labelMesEquilibrio, View.TRANSLATION_X, labelMesEquilibrio.translationX, xPos - (labelMesEquilibrio.width / 2f) + (dotEquilibrio.width / 2f)).apply {
                        duration = 220
                        start()
                    }
                }
            }
        }

        fun recalcular() {
            val renta = rentaFrom(seekRenta.progress)
            val inversion = inversionFrom(seekInversion.progress)
            val ticket = ticketFrom(seekTicket.progress)
            val ventas = ventasFrom(seekVentas.progress)
            val margen = 40.0

            val margenDecimal = margen / 100.0
            val ingresosBrutosMensuales = ticket * ventas
            val utilidadBrutaMensual = ingresosBrutosMensuales * margenDecimal
            val otrosCostosMensuales = gastoMarketingMensual + costoEmpleadosMensual
            val utilidadNetaMensual = utilidadBrutaMensual - renta - otrosCostosMensuales

            if (utilidadNetaMensual <= 0.0 || inversion <= 0.0) {
                textRoiBig.text = "--"
                textMesesRetorno.text = "—"
                textNivelRiesgo.text = "DEMASIADO ALTO"
                dotRiesgo.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.risk_high)
                textRoiDelta.text = "0.0%"
            } else {
                val meses = inversion / utilidadNetaMensual
                val roiAnual = (utilidadNetaMensual * 12.0 / inversion) * 100.0
                textRoiBig.text = String.format(Locale.getDefault(), "%.1f%%", roiAnual)
                textMesesRetorno.text = String.format(Locale.getDefault(), "%.0f meses", meses)
                posicionarPuntoEquilibrio(meses)

                val color = when {
                    meses > 36 || roiAnual < 5 -> R.color.risk_high
                    meses > 18 || roiAnual < 15 -> R.color.risk_medium
                    else -> R.color.risk_low
                }
                textNivelRiesgo.text = when (color) {
                    R.color.risk_high -> "ALTO"
                    R.color.risk_medium -> "MEDIO"
                    else -> "BAJO"
                }
                dotRiesgo.backgroundTintList = ContextCompat.getColorStateList(requireContext(), color)
                textRoiDelta.text = "+2.1%"
            }
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                actualizarLabels()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                recalcular()
            }
        }

        seekRenta.setOnSeekBarChangeListener(listener)
        seekInversion.setOnSeekBarChangeListener(listener)
        seekTicket.setOnSeekBarChangeListener(listener)
        seekVentas.setOnSeekBarChangeListener(listener)

        textRentaValor.setOnClickListener {
            showNumberInput("Renta Mensual", "$", rentaFrom(seekRenta.progress)) { value ->
                val p = ((value - 2500.0) / 1000.0).toInt().coerceIn(0, 100)
                seekRenta.progress = p
                actualizarLabels()
                recalcular()
            }
        }
        textInversionValor.setOnClickListener {
            showNumberInput("Inversión Inicial", "$", inversionFrom(seekInversion.progress)) { value ->
                val p = ((value - 50000.0) / 10000.0).toInt().coerceIn(0, 100)
                seekInversion.progress = p
                actualizarLabels()
                recalcular()
            }
        }
        textTicketValor.setOnClickListener {
            showNumberInput("Ticket Promedio", "$", ticketFrom(seekTicket.progress)) { value ->
                val p = ((value - 50.0) / 20.0).toInt().coerceIn(0, 100)
                seekTicket.progress = p
                actualizarLabels()
                recalcular()
            }
        }
        textVentasValor.setOnClickListener {
            showNumberInput("Ventas Estimadas (mes)", "", ventasFrom(seekVentas.progress)) { value ->
                val p = ((value - 50.0) / 10.0).toInt().coerceIn(0, 100)
                seekVentas.progress = p
                actualizarLabels()
                recalcular()
            }
        }

        actualizarLabels()
        recalcular()
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
