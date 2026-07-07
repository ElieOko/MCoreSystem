package elieoko.app.mcoresystem.domain.util

import java.security.MessageDigest

object SecurityUtil {
    /** Hash SHA-256 : seul le hash du mot de passe est synchronisé vers le cloud. */
    fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
