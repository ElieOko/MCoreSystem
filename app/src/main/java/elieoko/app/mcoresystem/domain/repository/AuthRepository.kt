package elieoko.app.mcoresystem.domain.repository

import android.util.Log
import elieoko.app.mcoresystem.data.preferences.SessionRepository
import elieoko.app.mcoresystem.data.preferences.UserSession
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.data.remote.RemoteOrganism
import elieoko.app.mcoresystem.data.remote.RemoteUser
import elieoko.app.mcoresystem.data.remote.SupabaseProvider
import elieoko.app.mcoresystem.data.remote.SyncManager
import elieoko.app.mcoresystem.data.room.MCoreRoomDatabase
import elieoko.app.mcoresystem.domain.model.AuthMode
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.model.room.UserModel
import elieoko.app.mcoresystem.domain.util.SecurityUtil
import elieoko.app.mcoresystem.domain.util.TimeUtil
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Authentification sans Supabase Auth : les comptes vivent dans la table
 * applicative `users` (cloud) et dans Room (local).
 *
 * Trois modes :
 * - LOCAL  : Room uniquement.
 * - ONLINE : table `users` Supabase uniquement (hash SHA-256 du mot de passe).
 * - AUTO   : local d'abord, puis Supabase si le compte n'existe pas localement.
 */
class AuthRepository(
    private val database: MCoreRoomDatabase,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager
) {

    suspend fun login(
        identifier: String,
        password: String,
        mode: AuthMode = AuthMode.AUTO
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            when (mode) {
                AuthMode.LOCAL -> localLogin(identifier, password)
                    ?: AuthResult.Error("Identifiants incorrects (compte local introuvable)")
                AuthMode.ONLINE -> onlineLogin(identifier, password)
                AuthMode.AUTO -> {
                    localLogin(identifier, password)
                        ?: if (localAccountExists(identifier)) {
                            AuthResult.Error("Identifiants incorrects")
                        } else {
                            onlineLogin(identifier, password)
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur de connexion", e)
            AuthResult.Error(e.message ?: "Erreur de connexion")
        }
    }

    private fun localAccountExists(identifier: String): Boolean =
        database.userDao().findByEmail(identifier) != null ||
            database.userDao().findByUsername(identifier) != null

    private suspend fun localLogin(identifier: String, password: String): AuthResult? {
        val user = database.userDao().login(identifier, password)
            ?: database.userDao().findByEmail(identifier)?.takeIf { it.password == password }
        return user?.let { persistAndReturn(it) }
    }

    /** Connexion contre la table `users` de Supabase (hash du mot de passe). */
    private suspend fun onlineLogin(identifier: String, password: String): AuthResult {
        val client = SupabaseProvider.client
            ?: return AuthResult.Error("Supabase n'est pas configuré : utilisez le mode local")
        return try {
            val candidates = client.from(SyncManager.TABLE_USERS).select(Columns.ALL) {
                filter {
                    or {
                        eq("username", identifier)
                        eq("email", identifier)
                    }
                }
            }.decodeList<RemoteUser>()

            val hash = SecurityUtil.sha256(password)
            val remote = candidates.firstOrNull { it.passwordHash == hash }
                ?: return AuthResult.Error(
                    if (candidates.isEmpty()) "Compte introuvable sur le cloud"
                    else "Mot de passe incorrect"
                )

            // Rapatrie l'organisation puis le compte dans Room (cache local).
            val organism = ensureLocalOrganism(client, remote.organismUuid)
            val existing = database.userDao().findByUuid(remote.uuid)
            val user = if (existing == null) {
                UserModel(
                    id = database.userDao().countUsers() + 1,
                    username = remote.username,
                    phone = remote.phone,
                    email = remote.email,
                    password = password, // en clair uniquement en local, pour le mode hors ligne
                    organismId = organism?.id ?: 1,
                    role = remote.role,
                    uuid = remote.uuid,
                    updatedAt = remote.updatedAt
                ).also { database.userDao().insertAll(it) }
            } else {
                existing.copy(
                    username = remote.username,
                    email = remote.email,
                    phone = remote.phone,
                    password = password,
                    role = remote.role
                ).also { database.userDao().updateAll(it) }
            }
            persistAndReturn(user)
        } catch (e: Exception) {
            Log.e(TAG, "Connexion en ligne impossible", e)
            AuthResult.Error("Connexion en ligne impossible : ${e.message}")
        }
    }

    private suspend fun ensureLocalOrganism(
        client: io.github.jan.supabase.SupabaseClient,
        organismUuid: String
    ): OrganismModel? {
        database.organismDao().findByUuid(organismUuid)?.let { return it }
        val remote = runCatching {
            client.from(SyncManager.TABLE_ORGANISMS).select(Columns.ALL) {
                filter { eq("uuid", organismUuid) }
            }.decodeList<RemoteOrganism>().firstOrNull()
        }.getOrNull() ?: return null
        val organism = OrganismModel(
            id = (database.organismDao().getAll().maxOfOrNull { it.id } ?: 0) + 1,
            name = remote.name,
            uuid = remote.uuid,
            updatedAt = remote.updatedAt
        )
        database.organismDao().insertAll(organism)
        return organism
    }

    /**
     * Inscription : organisation + compte administrateur, créés localement puis
     * synchronisés vers la table `users` du cloud (jamais dans Supabase Auth).
     */
    suspend fun register(
        username: String,
        email: String?,
        phone: String?,
        password: String,
        organizationName: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val now = TimeUtil.nowIso()
            val existing = database.organismDao().getAll().firstOrNull { it.name == organizationName }
            val organism = existing ?: OrganismModel(
                id = (database.organismDao().getAll().maxOfOrNull { it.id } ?: 0) + 1,
                name = organizationName,
                updatedAt = now
            ).also { database.organismDao().insertAll(it) }

            val user = UserModel(
                id = database.userDao().countUsers() + 1,
                username = username,
                phone = phone,
                email = email,
                password = password,
                organismId = organism.id,
                role = ROLE_ADMIN,
                updatedAt = now
            )
            database.userDao().insertAll(user)
            // Fera partir le compte vers la table `users` du cloud à la prochaine sync.
            ChangeTracker(database.syncQueueDao()).recordUpsert(SyncQueueModel.TYPE_USER, user.uuid)
            syncManager.pushOrganism(organism)

            persistAndReturn(user)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur d'inscription", e)
            AuthResult.Error(e.message ?: "Erreur d'inscription")
        }
    }

    suspend fun restoreSession(): UserSession? {
        val session = sessionRepository.session.first()
        return if (session.isLoggedIn) session else null
    }

    suspend fun logout() {
        sessionRepository.clear()
    }

    private suspend fun persistAndReturn(user: UserModel): AuthResult {
        val organism = database.organismDao().findById(user.organismId)
        val session = UserSession(
            isLoggedIn = true,
            userId = user.id,
            username = user.username,
            organismId = user.organismId,
            organismUuid = organism?.uuid ?: "",
            userUuid = user.uuid,
            role = user.role
        )
        sessionRepository.saveSession(session)
        return AuthResult.Success(session)
    }

    companion object {
        private const val TAG = "AuthRepository"
        const val ROLE_ADMIN = "ADMIN"
    }
}
