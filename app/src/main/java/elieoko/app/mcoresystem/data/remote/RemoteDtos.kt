package elieoko.app.mcoresystem.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modèles distants (tables Supabase). Les références entre entités utilisent des UUID,
 * car les identifiants entiers Room sont locaux à chaque appareil.
 */

@Serializable
data class RemoteOrganism(
    val uuid: String,
    val name: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteCurrency(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val name: String = "",
    val code: String = "",
    val symbol: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemotePaymentMethod(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val name: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteTypeCategory(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val name: String = "",
    val description: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteCategory(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    @SerialName("type_category_uuid") val typeCategoryUuid: String? = null,
    val name: String = "",
    val description: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteOperation(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    @SerialName("category_uuid") val categoryUuid: String? = null,
    @SerialName("user_uuid") val userUuid: String? = null,
    @SerialName("payment_method_uuid") val paymentMethodUuid: String? = null,
    @SerialName("currency_uuid") val currencyUuid: String? = null,
    val amount: Double = 0.0,
    @SerialName("task_name") val taskName: String = "",
    val description: String = "",
    @SerialName("created_on") val createdOn: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    val status: String = "OUVERT",
    @SerialName("updated_at") val updatedAt: String = ""
)

/**
 * Table `users` applicative : indépendante de auth.users.
 * Seul le hash SHA-256 du mot de passe est synchronisé (jamais le mot de passe en clair),
 * ce qui permet la connexion en ligne sans Supabase Auth.
 */
@Serializable
data class RemoteUser(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val username: String = "",
    val email: String? = null,
    val phone: String? = null,
    val role: String = "MEMBER",
    @SerialName("password_hash") val passwordHash: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)
