package elieoko.app.mcoresystem.data.remote

import android.util.Log
import elieoko.app.mcoresystem.data.room.MCoreRoomDatabase
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel
import elieoko.app.mcoresystem.domain.util.TimeUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR, OFFLINE_ONLY, AUTH_REQUIRED }

/**
 * Moteur de synchronisation Offline First.
 *
 * - Room est la source de vérité pour l'UI (toutes les lectures viennent de Room).
 * - Chaque écriture locale est journalisée dans TSyncQueue puis poussée vers Supabase.
 * - Le pull récupère les données distantes de l'organisation et les fusionne dans Room
 *   avec une stratégie Last-Write-Wins basée sur `updated_at`.
 * - Sans configuration Supabase ou sans réseau : tout continue de fonctionner localement.
 */
class SyncManager(private val database: MCoreRoomDatabase) {

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val mutex = Mutex()

    suspend fun syncAll(organismUuid: String, username: String = "", role: String = "MEMBER") {
        val client = SupabaseProvider.client
        if (client == null) {
            _status.value = SyncStatus.OFFLINE_ONLY
            return
        }
        if (organismUuid.isBlank()) return
        // Sans session Supabase, RLS rejette toute écriture : on garde la file
        // en attente au lieu de générer des erreurs, et on informe l'UI.
        if (client.auth.currentSessionOrNull() == null) {
            _status.value = SyncStatus.AUTH_REQUIRED
            _lastError.value = "Session Supabase requise : connectez-vous avec votre email"
            return
        }
        mutex.withLock {
            _status.value = SyncStatus.SYNCING
            try {
                withContext(Dispatchers.IO) {
                    ensureRemoteIdentity(organismUuid, username, role)
                    push()
                    pull(organismUuid)
                }
                _status.value = SyncStatus.SUCCESS
                _lastError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Synchronisation échouée", e)
                _lastError.value = e.message
                _status.value = SyncStatus.ERROR
            }
        }
    }

    /**
     * Auto-réparation : garantit que l'organisation et le profil de l'utilisateur
     * connecté existent côté Supabase avant de pousser les données.
     * Sans cela, `current_organism_uuid()` est nul et toutes les politiques RLS échouent.
     */
    private suspend fun ensureRemoteIdentity(organismUuid: String, username: String, role: String) {
        val client = SupabaseProvider.client ?: return
        val authUid = client.auth.currentUserOrNull()?.id ?: return
        val organism = database.organismDao().findByUuid(organismUuid) ?: return

        val localUser = database.userDao().findByUuid(authUid)

        // 1. L'organisation d'abord (clé étrangère du profil ; insert ouvert
        //    à tout utilisateur authentifié).
        runCatching {
            client.from(TABLE_ORGANISMS).upsert(
                RemoteOrganism(
                    uuid = organism.uuid,
                    name = organism.name,
                    updatedAt = organism.updatedAt.ifBlank { TimeUtil.nowIso() }
                )
            ) { onConflict = "uuid" }
        }.onFailure { Log.w(TAG, "Upsert organisation : ${it.message}") }

        // 2. Puis le profil : RLS impose uuid = auth.uid(), et il est indispensable
        //    pour que current_organism_uuid() fonctionne sur toutes les autres tables.
        runCatching {
            client.from(TABLE_PROFILES).upsert(
                RemoteProfile(
                    uuid = authUid,
                    organismUuid = organism.uuid,
                    username = username.ifBlank { localUser?.username ?: "Utilisateur" },
                    email = localUser?.email,
                    phone = localUser?.phone,
                    role = role,
                    updatedAt = TimeUtil.nowIso()
                )
            ) { onConflict = "uuid" }
        }.onFailure { Log.w(TAG, "Upsert profil : ${it.message}") }
    }

    /** Room → Supabase : rejoue la file d'attente des changements locaux. */
    private suspend fun push() {
        val client = SupabaseProvider.client ?: return
        val queue = database.syncQueueDao().getAll()
        for (entry in queue) {
            try {
                when (entry.operation) {
                    SyncQueueModel.OP_DELETE -> {
                        val table = tableFor(entry.entityType) ?: continue
                        client.from(table).delete { filter { eq("uuid", entry.entityUuid) } }
                    }
                    SyncQueueModel.OP_UPSERT -> pushUpsert(entry)
                }
                database.syncQueueDao().deleteById(entry.id)
            } catch (e: Exception) {
                Log.e(TAG, "Échec push ${entry.entityType}/${entry.entityUuid}", e)
                throw e
            }
        }
    }

    private suspend fun pushUpsert(entry: SyncQueueModel) {
        val client = SupabaseProvider.client ?: return
        when (entry.entityType) {
            SyncQueueModel.TYPE_ORGANISM -> {
                val local = database.organismDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_ORGANISMS).upsert(
                    RemoteOrganism(uuid = local.uuid, name = local.name, updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() })
                ) { onConflict = "uuid" }
            }
            SyncQueueModel.TYPE_CURRENCY -> {
                val local = database.currencyDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_CURRENCIES).upsert(
                    RemoteCurrency(
                        uuid = local.uuid,
                        organismUuid = currentOrganismUuid(),
                        name = local.name, code = local.code, symbol = local.symbol,
                        updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() }
                    )
                ) { onConflict = "uuid" }
            }
            SyncQueueModel.TYPE_PAYMENT_METHOD -> {
                val local = database.paymentMethodDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_PAYMENT_METHODS).upsert(
                    RemotePaymentMethod(
                        uuid = local.uuid,
                        organismUuid = currentOrganismUuid(),
                        name = local.name,
                        updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() }
                    )
                ) { onConflict = "uuid" }
            }
            SyncQueueModel.TYPE_TYPE_CATEGORY -> {
                val local = database.typeCategoryDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_TYPE_CATEGORIES).upsert(
                    RemoteTypeCategory(
                        uuid = local.uuid,
                        organismUuid = organismUuidFor(local.organismId),
                        name = local.name, description = local.description, isActive = local.isActive,
                        updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() }
                    )
                ) { onConflict = "uuid" }
            }
            SyncQueueModel.TYPE_CATEGORY -> {
                val local = database.categoryDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_CATEGORIES).upsert(
                    RemoteCategory(
                        uuid = local.uuid,
                        organismUuid = organismUuidFor(local.organismId),
                        typeCategoryUuid = database.typeCategoryDao().findById(local.typeCategoryId)?.uuid,
                        name = local.name, description = local.description,
                        updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() }
                    )
                ) { onConflict = "uuid" }
            }
            SyncQueueModel.TYPE_USER -> {
                val local = database.userDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_USERS).upsert(
                    RemoteUser(
                        uuid = local.uuid,
                        organismUuid = organismUuidFor(local.organismId),
                        username = local.username,
                        email = local.email,
                        phone = local.phone,
                        role = local.role,
                        updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() }
                    )
                ) { onConflict = "uuid" }
            }
            SyncQueueModel.TYPE_OPERATION -> {
                val local = database.operationDao().findByUuid(entry.entityUuid) ?: return
                client.from(TABLE_OPERATIONS).upsert(
                    RemoteOperation(
                        uuid = local.uuid,
                        organismUuid = organismUuidFor(local.organismId),
                        categoryUuid = database.categoryDao().findById(local.categoryId)?.uuid,
                        userUuid = null,
                        paymentMethodUuid = database.paymentMethodDao().findById(local.paymentMethodId)?.uuid,
                        currencyUuid = database.currencyDao().findById(local.currencyId)?.uuid,
                        amount = local.amount,
                        taskName = local.taskName,
                        description = local.description,
                        createdOn = local.createdOn,
                        isActive = local.isActive,
                        status = local.status,
                        updatedAt = local.updatedAt.ifBlank { TimeUtil.nowIso() }
                    )
                ) { onConflict = "uuid" }
            }
        }
    }

    /** Supabase → Room : fusion Last-Write-Wins sur `updated_at`. */
    private suspend fun pull(organismUuid: String) {
        val client = SupabaseProvider.client ?: return
        val pendingUuids = database.syncQueueDao().getAll().map { it.entityUuid }.toSet()

        val remoteCurrencies = client.from(TABLE_CURRENCIES).select(Columns.ALL) {
            filter { eq("organism_uuid", organismUuid) }
        }.decodeList<RemoteCurrency>()
        for (remote in remoteCurrencies) {
            if (remote.uuid in pendingUuids) continue
            val local = database.currencyDao().findByUuid(remote.uuid)
            if (local == null) {
                database.currencyDao().insertAll(
                    CurrencyModel(name = remote.name, code = remote.code, symbol = remote.symbol, uuid = remote.uuid, updatedAt = remote.updatedAt)
                )
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                database.currencyDao().updateAll(
                    local.copy(name = remote.name, code = remote.code, symbol = remote.symbol, updatedAt = remote.updatedAt)
                )
            }
        }

        val remotePayments = client.from(TABLE_PAYMENT_METHODS).select(Columns.ALL) {
            filter { eq("organism_uuid", organismUuid) }
        }.decodeList<RemotePaymentMethod>()
        for (remote in remotePayments) {
            if (remote.uuid in pendingUuids) continue
            val local = database.paymentMethodDao().findByUuid(remote.uuid)
            if (local == null) {
                database.paymentMethodDao().insertAll(
                    PaymentMethodModel(name = remote.name, uuid = remote.uuid, updatedAt = remote.updatedAt)
                )
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                database.paymentMethodDao().updateAll(local.copy(name = remote.name, updatedAt = remote.updatedAt))
            }
        }

        val localOrganismId = database.organismDao().findByUuid(organismUuid)?.id ?: 1

        // Table users applicative (indépendante de auth.users) : partage des membres
        // de l'organisation entre appareils. Le mot de passe reste local.
        val remoteUsers = client.from(TABLE_USERS).select(Columns.ALL) {
            filter { eq("organism_uuid", organismUuid) }
        }.decodeList<RemoteUser>()
        for (remote in remoteUsers) {
            if (remote.uuid in pendingUuids) continue
            val local = database.userDao().findByUuid(remote.uuid)
            if (local == null) {
                database.userDao().insertAll(
                    elieoko.app.mcoresystem.domain.model.room.UserModel(
                        id = database.userDao().countUsers() + 1,
                        username = remote.username,
                        phone = remote.phone,
                        email = remote.email,
                        password = "",
                        organismId = localOrganismId,
                        role = remote.role,
                        uuid = remote.uuid,
                        updatedAt = remote.updatedAt
                    )
                )
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                database.userDao().updateAll(
                    local.copy(
                        username = remote.username,
                        phone = remote.phone,
                        email = remote.email,
                        role = remote.role,
                        updatedAt = remote.updatedAt
                    )
                )
            }
        }

        val remoteTypes = client.from(TABLE_TYPE_CATEGORIES).select(Columns.ALL) {
            filter { eq("organism_uuid", organismUuid) }
        }.decodeList<RemoteTypeCategory>()
        for (remote in remoteTypes) {
            if (remote.uuid in pendingUuids) continue
            val local = database.typeCategoryDao().findByUuid(remote.uuid)
            if (local == null) {
                database.typeCategoryDao().insertAll(
                    TypeCategoryModel(
                        organismId = localOrganismId, name = remote.name, description = remote.description,
                        isActive = remote.isActive, uuid = remote.uuid, updatedAt = remote.updatedAt
                    )
                )
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                database.typeCategoryDao().updateAll(
                    local.copy(name = remote.name, description = remote.description, isActive = remote.isActive, updatedAt = remote.updatedAt)
                )
            }
        }

        val remoteCategories = client.from(TABLE_CATEGORIES).select(Columns.ALL) {
            filter { eq("organism_uuid", organismUuid) }
        }.decodeList<RemoteCategory>()
        for (remote in remoteCategories) {
            if (remote.uuid in pendingUuids) continue
            val typeId = remote.typeCategoryUuid?.let { database.typeCategoryDao().findByUuid(it)?.id } ?: 0
            val local = database.categoryDao().findByUuid(remote.uuid)
            if (local == null) {
                database.categoryDao().insertAll(
                    CategoryModel(
                        organismId = localOrganismId, typeCategoryId = typeId,
                        name = remote.name, description = remote.description,
                        uuid = remote.uuid, updatedAt = remote.updatedAt
                    )
                )
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                database.categoryDao().updateAll(
                    local.copy(typeCategoryId = typeId, name = remote.name, description = remote.description, updatedAt = remote.updatedAt)
                )
            }
        }

        val remoteOperations = client.from(TABLE_OPERATIONS).select(Columns.ALL) {
            filter { eq("organism_uuid", organismUuid) }
        }.decodeList<RemoteOperation>()
        for (remote in remoteOperations) {
            if (remote.uuid in pendingUuids) continue
            val categoryId = remote.categoryUuid?.let { database.categoryDao().findByUuid(it)?.id } ?: 0
            val currencyId = remote.currencyUuid?.let { database.currencyDao().findByUuid(it)?.id } ?: 0
            val paymentId = remote.paymentMethodUuid?.let { database.paymentMethodDao().findByUuid(it)?.id } ?: 0
            val local = database.operationDao().findByUuid(remote.uuid)
            if (local == null) {
                database.operationDao().insertAll(
                    OperationModel(
                        organismId = localOrganismId, categoryId = categoryId, userId = 1,
                        paymentMethodId = paymentId, currencyId = currencyId,
                        amount = remote.amount, taskName = remote.taskName, description = remote.description,
                        createdOn = remote.createdOn, isActive = remote.isActive, status = remote.status,
                        uuid = remote.uuid, updatedAt = remote.updatedAt
                    )
                )
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                database.operationDao().updateAll(
                    local.copy(
                        categoryId = categoryId, paymentMethodId = paymentId, currencyId = currencyId,
                        amount = remote.amount, taskName = remote.taskName, description = remote.description,
                        createdOn = remote.createdOn, isActive = remote.isActive, status = remote.status,
                        updatedAt = remote.updatedAt
                    )
                )
            }
        }
    }

    /** Pousse l'organisation elle-même (création de compte). */
    suspend fun pushOrganism(organism: OrganismModel) {
        val client = SupabaseProvider.client ?: return
        withContext(Dispatchers.IO) {
            try {
                client.from(TABLE_ORGANISMS).upsert(
                    RemoteOrganism(uuid = organism.uuid, name = organism.name, updatedAt = organism.updatedAt.ifBlank { TimeUtil.nowIso() })
                ) { onConflict = "uuid" }
            } catch (e: Exception) {
                Log.e(TAG, "Échec push organisation", e)
            }
        }
    }

    suspend fun pushProfile(profile: RemoteProfile) {
        val client = SupabaseProvider.client ?: return
        withContext(Dispatchers.IO) {
            try {
                client.from(TABLE_PROFILES).upsert(profile) { onConflict = "uuid" }
            } catch (e: Exception) {
                Log.e(TAG, "Échec push profil", e)
            }
        }
    }

    private fun currentOrganismUuid(): String =
        database.organismDao().getAll().firstOrNull()?.uuid ?: ""

    private fun organismUuidFor(localId: Int): String =
        database.organismDao().findById(localId)?.uuid ?: currentOrganismUuid()

    private fun tableFor(entityType: String): String? = when (entityType) {
        SyncQueueModel.TYPE_CURRENCY -> TABLE_CURRENCIES
        SyncQueueModel.TYPE_PAYMENT_METHOD -> TABLE_PAYMENT_METHODS
        SyncQueueModel.TYPE_CATEGORY -> TABLE_CATEGORIES
        SyncQueueModel.TYPE_TYPE_CATEGORY -> TABLE_TYPE_CATEGORIES
        SyncQueueModel.TYPE_OPERATION -> TABLE_OPERATIONS
        SyncQueueModel.TYPE_ORGANISM -> TABLE_ORGANISMS
        SyncQueueModel.TYPE_USER -> TABLE_USERS
        else -> null
    }

    companion object {
        private const val TAG = "SyncManager"
        const val TABLE_ORGANISMS = "organisms"
        const val TABLE_PROFILES = "profiles"
        const val TABLE_USERS = "users"
        const val TABLE_CURRENCIES = "currencies"
        const val TABLE_PAYMENT_METHODS = "payment_methods"
        const val TABLE_TYPE_CATEGORIES = "type_categories"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_OPERATIONS = "operations"
    }
}
