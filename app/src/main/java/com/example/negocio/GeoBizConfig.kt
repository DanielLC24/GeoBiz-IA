package com.example.negocio

import android.content.Context
import org.json.JSONObject

/**
 * Tipo de negocio soportado por GeoBiz.
 */
enum class BusinessType(val jsonKey: String, val displayName: String) {
    RESTAURANTE("Restaurante", "Restaurante"),
    CAFETERIA("Cafeteria", "Cafetería"),
    AUTOS("Autos", "Taller / Autos"),
    FARMACIA("Farmacia", "Farmacia"),
    PAPELERIA("Papeleria", "Papelería"),
    FLORERIA("Floreria", "Florería");

    companion object {
        fun fromLabel(label: String): BusinessType? {
            val normalized = label.trim().lowercase()
            return entries.firstOrNull { type ->
                type.jsonKey.lowercase() == normalized ||
                        type.displayName.lowercase() == normalized ||
                        when (type) {
                            AUTOS -> normalized.contains("taller") || normalized.contains("autos")
                            else -> false
                        }
            }
        }
    }
}

/**
 * Pesos multicriterio para un negocio dado.
 *
 * w1..w7 corresponden a:
 * 1) Peatonal, 2) Vehicular, 3) Competencia, 4) NSE,
 * 5) Seguridad, 6) Densidad, 7) Renta.
 */
data class BusinessWeights(
    val w1: Double,
    val w2: Double,
    val w3: Double,
    val w4: Double,
    val w5: Double,
    val w6: Double,
    val w7: Double
) {
    val asList: List<Double>
        get() = listOf(w1, w2, w3, w4, w5, w6, w7)
}

/**
 * Resultado del análisis FODA.
 */
data class FodaAnalysis(
    val strengths: List<String>,
    val threats: List<String>,
    val opportunities: List<String>
)

/**
 * Resultado global del motor GeoBiz.
 */
data class GeoBizResult(
    val normalizedScore: Double,
    val recommendationColor: RecommendationColor,
    val foda: FodaAnalysis
)

enum class RecommendationColor {
    VERDE_EXITO,
    AMARILLO_RIESGO_MEDIO,
    ROJO_NO_RECOMENDADO
}

/**
 * Carga el archivo de configuración de negocios desde assets/business_config.json.
 *
 * Formato esperado (ejemplo):
 *
 * {
 *   "Restaurante": { "w1": 0.2, "w2": 0.15, ..., "w7": 0.1 },
 *   "Cafeteria":   { "w1": 0.18, "w2": 0.12, ..., "w7": 0.08 },
 *   ...
 * }
 */
object ConfigLoader {

    private const val CONFIG_FILE_NAME = "business_config.json"

    fun loadBusinessWeights(context: Context): Map<BusinessType, BusinessWeights> {
        return runCatching {
            context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { it.readText() }
        }.mapCatching { jsonString ->
            parseConfig(jsonString)
        }.getOrElse {
            emptyMap()
        }
    }

    private fun parseConfig(jsonString: String): Map<BusinessType, BusinessWeights> {
        val root = JSONObject(jsonString)
        val result = mutableMapOf<BusinessType, BusinessWeights>()

        for (type in BusinessType.entries) {
            val node = root.optJSONObject(type.jsonKey) ?: continue
            val weights = BusinessWeights(
                w1 = node.optDouble("w1", 0.0),
                w2 = node.optDouble("w2", 0.0),
                w3 = node.optDouble("w3", 0.0),
                w4 = node.optDouble("w4", 0.0),
                w5 = node.optDouble("w5", 0.0),
                w6 = node.optDouble("w6", 0.0),
                w7 = node.optDouble("w7", 0.0)
            )
            result[type] = weights
        }
        return result
    }
}

/**
 * Motor de evaluación GeoBiz.
 *
 * x1..x7 son valores normalizados de 0 a 100 para:
 * Peatonal, Vehicular, Competencia, NSE, Seguridad, Densidad, Renta.
 */
class GeoBizEngine(
    private val businessWeights: Map<BusinessType, BusinessWeights>
 = emptyMap()) {

    fun calculateScore(
        businessType: BusinessType,
        x1: Double,
        x2: Double,
        x3: Double,
        x4: Double,
        x5: Double,
        x6: Double,
        x7: Double
    ): GeoBizResult {
        val weights = businessWeights[businessType]
            ?: BusinessWeights(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)

        val xs = listOf(x1, x2, x3, x4, x5, x6, x7).map { it.coerceIn(0.0, 100.0) }
        val ws = weights.asList

        val rawScore = xs.zip(ws).sumOf { (x, w) -> w * x }
        val maxPossible = ws.sumOf { it * 100.0 }
        val normalized = if (maxPossible > 0.0) (rawScore / maxPossible) * 100.0 else 0.0

        val color = when {
            normalized > 75.0 -> RecommendationColor.VERDE_EXITO
            normalized >= 50.0 -> RecommendationColor.AMARILLO_RIESGO_MEDIO
            else -> RecommendationColor.ROJO_NO_RECOMENDADO
        }

        val foda = buildFoda(xs)

        return GeoBizResult(
            normalizedScore = normalized,
            recommendationColor = color,
            foda = foda
        )
    }

    private fun buildFoda(xs: List<Double>): FodaAnalysis {
        val labels = listOf(
            "Flujo peatonal",
            "Flujo vehicular",
            "Competencia",
            "Nivel socioeconómico (NSE)",
            "Seguridad",
            "Densidad poblacional",
            "Renta"
        )

        val strengths = mutableListOf<String>()
        val threats = mutableListOf<String>()
        val opportunities = mutableListOf<String>()

        xs.forEachIndexed { index, value ->
            if (value > 70.0) {
                strengths += labels[index]
            }
            if (value < 40.0) {
                threats += labels[index]
            }
        }

        val competencia = xs[2] // x3
        if (competencia > 70.0) {
            threats += "Competencia muy alta en la zona"
        }

        val nse = xs[3] // x4
        if (nse > 70.0 && competencia < 40.0) {
            opportunities += "Alto nivel socioeconómico con baja competencia: oportunidad de capturar mercado premium"
        }

        // Oportunidad adicional si la renta es relativamente baja pero el entorno es bueno.
        val renta = xs[6] // x7
        val entornoPromedio = listOf(xs[0], xs[1], xs[3], xs[4], xs[5]).average()
        if (entornoPromedio > 65.0 && renta < 50.0) {
            opportunities += "Renta relativamente accesible en una zona atractiva"
        }

        return FodaAnalysis(
            strengths = strengths.distinct(),
            threats = threats.distinct(),
            opportunities = opportunities.distinct()
        )
    }
}

