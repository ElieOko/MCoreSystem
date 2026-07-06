package elieoko.app.mcoresystem.domain.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * File d'attente locale des changements à propager vers Supabase.
 * Chaque écriture locale (création / modification / suppression) est enregistrée ici
 * puis rejouée dès que la connexion est disponible. Aucune donnée n'est perdue hors ligne.
 */
@Entity(tableName = "TSyncQueue")
data class SyncQueueModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sync_id") val id: Int = 0,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_uuid") val entityUuid: String,
    @ColumnInfo(name = "operation") val operation: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val OP_UPSERT = "UPSERT"
        const val OP_DELETE = "DELETE"

        const val TYPE_CURRENCY = "currency"
        const val TYPE_PAYMENT_METHOD = "payment_method"
        const val TYPE_CATEGORY = "category"
        const val TYPE_TYPE_CATEGORY = "type_category"
        const val TYPE_OPERATION = "operation"
        const val TYPE_ORGANISM = "organism"
        const val TYPE_USER = "user"
    }
}
