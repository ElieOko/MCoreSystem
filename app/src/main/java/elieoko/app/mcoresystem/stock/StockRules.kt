package elieoko.app.mcoresystem.stock

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Règles pures du module stock. Le stock central et le mini-stock d'un livreur
 * ne peuvent jamais devenir négatifs.
 */
object StockRules {
    const val EPSILON = 0.0001

    fun parseQuantity(raw: String): Double? {
        val normalized = raw.trim().replace(',', '.').replace(" ", "")
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value <= 0.0 || value.isNaN() || value.isInfinite()) return null
        return value
    }

    /** Montant unitaire : vide vaut 0, un nombre négatif est refusé. */
    fun parseAmount(raw: String): Double? {
        if (raw.isBlank()) return 0.0
        val normalized = raw.trim().replace(',', '.').replace(" ", "")
        val value = normalized.toDoubleOrNull() ?: return null
        if (value < 0.0 || value.isNaN() || value.isInfinite()) return null
        return value
    }

    fun formatAmount(value: Double): String {
        val rounded = round(value * 100.0) / 100.0
        return String.format(Locale.FRANCE, "%.2f", rounded)
    }

    fun formatQty(value: Double): String {
        val rounded = round(value * 100.0) / 100.0
        return if (abs(rounded - rounded.toLong()) < 0.001) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.FRANCE, "%.2f", rounded)
        }
    }

    fun cleanName(raw: String): String? {
        val name = raw.trim().replace(Regex("\\s+"), " ")
        return name.takeIf { it.isNotEmpty() }
    }

    fun giveError(central: Double, requested: Double): String? {
        if (requested <= 0.0) return "La quantité doit être supérieure à zéro"
        if (central + EPSILON < requested) {
            return "Stock central insuffisant (${formatQty(central)} disponible)"
        }
        return null
    }

    fun takeFromMiniError(available: Double, requested: Double): String? {
        if (requested <= 0.0) return "La quantité doit être supérieure à zéro"
        if (available + EPSILON < requested) {
            return "Mini-stock insuffisant (${formatQty(available)} disponible)"
        }
        return null
    }

    fun subtract(available: Double, requested: Double): Double {
        val left = available - requested
        return if (left <= EPSILON) 0.0 else left
    }

    fun add(current: Double, incoming: Double): Double = current + incoming

    fun isGone(quantity: Double): Boolean = quantity <= EPSILON

    fun normalizeUsername(raw: String): String? {
        val value = raw.trim()
        if (value.length < 3 || value.any { it.isWhitespace() }) return null
        return value
    }

    fun passwordError(raw: String): String? {
        if (raw.length < 4) return "Le mot de passe doit contenir au moins 4 caractères"
        return null
    }

    /**
     * Après une livraison : la commande est livrée quand chaque ligne
     * commandée a été au moins couverte par les sorties.
     */
    fun nextOrderStatus(orderedAndDelivered: List<Pair<Double, Double>>): String {
        val complete = orderedAndDelivered.isNotEmpty() &&
            orderedAndDelivered.all { (ordered, delivered) -> delivered + EPSILON >= ordered }
        return if (complete) "LIVREE" else "EN_LIVRAISON"
    }
}
