package elieoko.app.mcoresystem.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtil {

    private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun nowIso(): String {
        val format = SimpleDateFormat(ISO_PATTERN, Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    /** Comparaison lexicographique valable pour le format ISO UTC. */
    fun isNewer(candidate: String?, reference: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        if (reference.isNullOrBlank()) return true
        return candidate > reference
    }
}
