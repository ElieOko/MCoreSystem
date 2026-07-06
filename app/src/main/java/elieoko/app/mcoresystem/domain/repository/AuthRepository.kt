package elieoko.app.mcoresystem.domain.repository

import android.util.Log
import elieoko.app.mcoresystem.data.preferences.SessionRepository
import elieoko.app.mcoresystem.data.preferences.UserSession
import elieoko.app.mcoresystem.data.remote.RemoteProfile
import elieoko.app.mcoresystem.data.remote.SupabaseProvider
import elieoko.app.mcoresystem.data.remote.SyncManager
import elieoko.app.mcoresystem.data.room.MCoreRoomDatabase
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.model.room.UserModel
import elieoko.app.mcoresystem.domain.util.TimeUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Point d'entrée unique pour l'authentification.
 *
 * - Si Supabase est configuré : authentification cloud (email + mot de passe),
 *   session gérée et restaurée automatiquement par le SDK.
 * - Sinon (ou hors ligne) : repli transparent sur l'authentification locale Room.
 * La session applicative est persistée dans DataStore pour que l'utilisateur
 * reste connecté entre les lancements.
 */
class AuthRepository(
    private val database: MCoreRoomDatabase,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager
) {

    suspend fun login(identifier: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseProvider.client
            if (client != null && identifier.contains("@")) {
                try {
                    client.auth.signInWith(Email) {
                        this.email = identifier
                        this.password = password
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Connexion Supabase impossible, repli local : ${e.message}")
                    return@withContext localLogin(identifier, password)
                }
                val local = database.userDao().findByEmail(identifier)
                    ?: return@withContext localLogin(identifier, password)
                return@withContext persistAndReturn(local)
            }
            localLogin(identifier, password)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur de connexion", e)
            AuthResult.Error(e.message ?: "Erreur de connexion")
        }
    }

    private suspend fun localLogin(identifier: String, password: String): AuthResult {
        val user = database.userDao().login(identifier, password)
            ?: database.userDao().findByEmail(identifier)?.takeIf { it.password == password }
        return if (user != null) persistAndReturn(user)
        else AuthResult.Error("Identifiants incorrects")
    }

    /**
     * Inscription : crée l'organisation (si nécessaire), le compte utilisateur,
     * définit l'utilisateur comme administrateur, puis pousse le tout vers Supabase.
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
            // 1. Organisation locale
            val existing = database.organismDao().getAll().firstOrNull { it.name == organizationName }
            val organism = existing ?: OrganismModel(
                id = (database.organismDao().getAll().maxOfOrNull { it.id } ?: 0) + 1,
                name = organizationName,
                updatedAt = now
            ).also { database.organismDao().insertAll(it) }

            // 2. Compte Supabase (si configuré et email fourni)
            val client = SupabaseProvider.client
            var userUuid = java.util.UUID.randomUUID().toString()
            if (client != null && !email.isNullOrBlank()) {
                try {
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    client.auth.currentUserOrNull()?.id?.let { userUuid = it }
                } catch (e: Exception) {
                    Log.w(TAG, "Inscription Supabase impossible (mode hors ligne) : ${e.message}")
                }
            }

            // 3. Utilisateur local, administrateur de l'organisation
            val userId = database.userDao().countUsers() + 1
            val user = UserModel(
                id = userId,
                username = username,
                phone = phone,
                email = email,
                password = password,
                organismId = organism.id,
                role = ROLE_ADMIN,
                uuid = userUuid,
                updatedAt = now
            )
            database.userDao().insertAll(user)

            // 4. Pousse organisation + profil vers Supabase
            syncManager.pushOrganism(organism)
            syncManager.pushProfile(
                RemoteProfile(
                    uuid = userUuid,
                    organismUuid = organism.uuid,
                    username = username,
                    email = email,
                    phone = phone,
                    role = ROLE_ADMIN,
                    updatedAt = now
                )
            )
            persistAndReturn(user)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur d'inscription", e)
            AuthResult.Error(e.message ?: "Erreur d'inscription")
        }
    }

    /** Restaure la session persistée (démarrage de l'application). */
    suspend fun restoreSession(): UserSession? {
        val session = sessionRepository.session.first()
        return if (session.isLoggedIn) session else null
    }

    suspend fun logout() {
        try {
            SupabaseProvider.client?.auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Déconnexion Supabase : ${e.message}")
        }
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
